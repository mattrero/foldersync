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

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DestinationFolderVisitor extends SimpleFileVisitor<Path> {

	final Logger logger = LoggerFactory.getLogger(DestinationFolderVisitor.class);

	private final Path fromDir;
	private final Path toDir;

	DestinationFolderVisitor(final Path fromDir, final Path toDir) {
		this.fromDir = fromDir;
		this.toDir = toDir;
	}

	private Path resolveFromPath(final Path toPath) {
		return fromDir.resolve(toDir.relativize(toPath));
	}

	@Override
	public FileVisitResult preVisitDirectory(final Path toPath, final BasicFileAttributes attrs) {
		final Path fromPath = resolveFromPath(toPath);

		if (toPath.equals(toDir)) {
			return FileVisitResult.CONTINUE;
		}

		try {
			if (!Files.exists(fromPath)) {
				FileUtils.deleteDirectory(toPath.toFile());
			}
		} catch (final IOException e) {
			logger.warn("Failed to delete directory " + toPath.toAbsolutePath(), e);
		}

		return FileVisitResult.SKIP_SUBTREE;
	}

	@Override
	public FileVisitResult visitFile(final Path toPath, final BasicFileAttributes attrs) {
		final Path fromPath = resolveFromPath(toPath);

		try {
			if (!Files.exists(fromPath)) {
				Files.delete(toPath);
			}
		} catch (final IOException e) {
			logger.warn("Failed to delete file " + toPath.toAbsolutePath(), e);
		}

		return FileVisitResult.CONTINUE;
	}

}
