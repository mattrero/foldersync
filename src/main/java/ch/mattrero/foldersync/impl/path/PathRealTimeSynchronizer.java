/*
 * Copyright (C) 2014 Matthieu Ferrero
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.mattrero.foldersync.impl.path;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.mattrero.foldersync.IRealTimeSynchronizer;

public class PathRealTimeSynchronizer extends Thread implements IRealTimeSynchronizer {

	final Logger logger = LoggerFactory.getLogger(PathRealTimeSynchronizer.class);

	private final class SourceSyncAndRegisterVisitor extends SourceSyncVisitor {
		private SourceSyncAndRegisterVisitor(final PathSynchronizer pathSynchronizer, final Path fromDir,
				final Path toDir) {
			super(pathSynchronizer, fromDir, toDir);
		}

		@Override
		public FileVisitResult preVisitDirectory(final Path fromPath, final BasicFileAttributes attrs)
				throws IOException {
			register(fromPath);
			super.preVisitDirectory(fromPath, attrs);
			return FileVisitResult.CONTINUE;
		}
	}

	private final PathSynchronizer pathSynchronizer;
	private final SourceSyncAndRegisterVisitor syncAndRegisterVisitor;

	private final WatchService watchService;
	private final Map<WatchKey, Path> watchKeys;

	private final Path fromDir;
	private final Path toDir;

	private volatile boolean running;

	public PathRealTimeSynchronizer(final Path fromDir, final Path toDir) throws IOException {
		this.fromDir = fromDir;
		this.toDir = toDir;

		this.watchKeys = new HashMap<WatchKey, Path>();
		watchService = FileSystems.getDefault().newWatchService();

		this.pathSynchronizer = new PathSynchronizer();
		this.syncAndRegisterVisitor = new SourceSyncAndRegisterVisitor(pathSynchronizer, fromDir, toDir);

		this.running = false;
	}

	private void register(final Path directory) throws IOException {
		watchKeys.put(directory.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY), directory);
	}

	public void syncAndRegister(final Path fromPath) throws IOException {
		Files.walkFileTree(fromPath, syncAndRegisterVisitor);
	}

	private void init() {
		try {
			syncAndRegister(fromDir);
			Files.walkFileTree(toDir, new DestinationSyncVisitor(pathSynchronizer, fromDir, toDir));
		} catch (final IOException e) {
			logger.warn("Failed to sync folders " + fromDir.toAbsolutePath() + " => " + toDir.toAbsolutePath(), e);
		}

		running = true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		WatchKey key;
		Path parentDir;
		Kind<?> kind;
		Path fromPath;

		init();

		while (running) {
			try {
				key = watchService.take();
			} catch (final InterruptedException e) {
				continue;
			}

			parentDir = watchKeys.get(key);

			if (parentDir == null) {
				continue;
			}

			for (final WatchEvent<?> event : key.pollEvents()) {

				kind = event.kind();

				if (kind != OVERFLOW) {
					fromPath = parentDir.resolve(((WatchEvent<Path>) event).context());
					try {
						if (kind == ENTRY_DELETE) {
							pathSynchronizer.syncLevel(fromPath, toDir.resolve(fromDir.relativize(fromPath)));
						} else if (Files.isDirectory(fromPath)) {
							syncAndRegister(fromPath);
						} else {
							pathSynchronizer.syncLevel(fromPath, toDir.resolve(fromDir.relativize(fromPath)));
						}
					} catch (final IOException e) {
						logger.warn("Failed to sync item {}" + fromPath.toAbsolutePath(), e);
					}
				} else {
					logger.warn("Overflow in in the watch service for key " + parentDir.toAbsolutePath());
					// TODO add re-browser & register of folder ?
				}

			}

			// reset key and remove from set if directory no longer accessible
			if (!key.reset()) {
				watchKeys.remove(key);

				if (watchKeys.isEmpty()) {
					running = false;
					logger.warn("No more directory to monitor under {})", fromDir);
					logger.warn("RealTimeSynchronizer shutdown by itself");
				}
			}
		}

		try {
			watchService.close();
		} catch (final IOException e) {
		}

	}

	public boolean isRunning() {
		return running;
	}

	@Override
	public void shutdownNow() throws InterruptedException {
		running = false;
		this.join();
	}
}
