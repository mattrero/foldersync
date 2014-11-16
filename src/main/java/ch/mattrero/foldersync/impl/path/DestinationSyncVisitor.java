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

import static ch.mattrero.foldersync.SyncStatus.DELETED;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.mattrero.foldersync.SyncStatus;

class DestinationSyncVisitor extends SimpleFileVisitor<Path> {

	final Logger logger = LoggerFactory.getLogger(DestinationSyncVisitor.class);

	public PathSynchronizer pathSynchronizer;

	private final Path fromDir;
	private final Path toDir;

	DestinationSyncVisitor(final PathSynchronizer pathSynchronizer, final Path fromDir, final Path toDir) {
		this.pathSynchronizer = pathSynchronizer;
		this.fromDir = fromDir;
		this.toDir = toDir;
	}

	private Path resolveFromPath(final Path toPath) {
		return fromDir.resolve(toDir.relativize(toPath));
	}

	@Override
	public FileVisitResult preVisitDirectory(final Path toPath, final BasicFileAttributes attrs) {
		if (toPath.equals(toDir)) {
			return FileVisitResult.CONTINUE;
		}

		SyncStatus status = null;

		try {
			status = pathSynchronizer.syncLevel(resolveFromPath(toPath), toPath);
		} catch (final IOException e) {
			logger.warn("Failed to delete directory " + toPath.toAbsolutePath(), e);
		}

		return (status == DELETED ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE);
	}

	@Override
	public FileVisitResult visitFile(final Path toPath, final BasicFileAttributes attrs) {
		try {
			pathSynchronizer.syncLevel(resolveFromPath(toPath), toPath);
		} catch (final IOException e) {
			logger.warn("Failed to delete file " + toPath.toAbsolutePath(), e);
		}

		return FileVisitResult.CONTINUE;
	}

}
