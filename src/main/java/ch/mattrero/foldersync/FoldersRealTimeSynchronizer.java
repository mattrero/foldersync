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
package ch.mattrero.foldersync;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FoldersRealTimeSynchronizer extends Thread implements IRealTimeSynchronizer {

	final Logger logger = LoggerFactory.getLogger(FoldersRealTimeSynchronizer.class);

	private final FoldersSynchronizer foldersSynchronizer;

	private final WatchService watchService;
	private final Map<WatchKey, Path> watchKeys;

	private volatile boolean running;

	public FoldersRealTimeSynchronizer(final FoldersSynchronizer foldersSynchronizer) throws IOException {
		this.watchKeys = new HashMap<WatchKey, Path>();
		this.watchService = FileSystems.getDefault().newWatchService();

		this.foldersSynchronizer = foldersSynchronizer;

		this.running = false;
	}

	public void registerTree(final Path sourceSubDir) throws IOException {
		Files.walkFileTree(sourceSubDir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(final Path directory, final BasicFileAttributes attrs)
					throws IOException {
				watchKeys.put(directory.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY), directory);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		WatchKey key;
		Path parentDir;
		Kind<?> kind;
		Path fromPath;

		try {
			foldersSynchronizer.sync();
			registerTree(foldersSynchronizer.getSourceDir());
		} catch (final IOException e) {
			logger.warn(
					"Failed to sync folders " + foldersSynchronizer.getSourceDir() + " => "
							+ foldersSynchronizer.getBackupDir(), e);
		}

		running = true;

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
							foldersSynchronizer.syncDeleted(fromPath);
						} else if (kind == ENTRY_CREATE) {
							foldersSynchronizer.syncAdded(fromPath);
							registerTree(fromPath);
						} else if (!Files.isDirectory(fromPath)) { // Nothing to do if we received MODIFIED on a directory
							foldersSynchronizer.syncModified(fromPath);
						}
					} catch (final IOException e) {
						logger.warn("Failed to sync item {}" + fromPath.toAbsolutePath(), e);
					}
				} else {
					logger.warn("Overflow for key " + parentDir.toAbsolutePath());
					foldersSynchronizer.syncTree(parentDir);
				}
			}

			// reset key and remove from set if directory no longer accessible
			if (!key.reset()) {
				watchKeys.remove(key);

				if (watchKeys.isEmpty()) {
					running = false;
					logger.warn("Shuting down (no more directory to monitor)");
				}
			}
		}

		try {
			watchService.close();
		} catch (final IOException e) {
		}

	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public void shutdownNow() throws InterruptedException {
		running = false;
		try {
			this.join();
			watchService.close();
		} catch (final InterruptedException e) {
			throw e;
		} catch (final IOException e) {
		}
	}
}
