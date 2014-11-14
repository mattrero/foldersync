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

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SourceFolderVisitor extends SimpleFileVisitor<Path> {

	final Logger logger = LoggerFactory.getLogger(SourceFolderVisitor.class);

	private final Path fromDir;
	private final Path toDir;

	// FAT file system only has 2 seconds resolution
	private final long modifiedTimeTolerance; // in milliseconds

	SourceFolderVisitor(final Path fromDir, final Path toDir) {
		this(fromDir, toDir, 0L);
	}

	public SourceFolderVisitor(final Path fromDir, final Path toDir, final long modifiedTimeTolerance) {
		this.fromDir = fromDir;
		this.toDir = toDir;
		this.modifiedTimeTolerance = modifiedTimeTolerance;
	}

	private Path resolveToPath(final Path fromPath) {
		return toDir.resolve(fromDir.relativize(fromPath));
	}

	private void copyIfNeeded(final Path fromPath, final Path toPath, final BasicFileAttributes fromAttributes)
			throws IOException {

		if (Files.exists(toPath)) {
			if (!isModified(fromPath, toPath, fromAttributes)) {
				return;
			}

			if (Files.isDirectory(toPath)) {
				FileUtils.deleteDirectory(toPath.toFile());
			} else {
				Files.delete(toPath);
			}
		}

		Files.copy(fromPath, toPath, COPY_ATTRIBUTES, REPLACE_EXISTING);
	}

	private boolean isModified(final Path fromPath, final Path toPath, final BasicFileAttributes fromAttributes)
			throws IOException {

		if (Files.isDirectory(fromPath) != Files.isDirectory(toPath)) {
			return true;
		}

		if (!Files.isDirectory(fromPath)) {

			final BasicFileAttributes toAttributes = Files.readAttributes(toPath, BasicFileAttributes.class);

			if (Math.abs(fromAttributes.lastModifiedTime().toMillis() - toAttributes.lastModifiedTime().toMillis()) >= modifiedTimeTolerance) {
				return true;
			}

			if (fromAttributes.size() != toAttributes.size()) {
				return true;
			}

		}

		return false;
	}

	@Override
	public FileVisitResult preVisitDirectory(final Path fromPath, final BasicFileAttributes attrs) {
		final Path toPath = resolveToPath(fromPath);

		try {
			copyIfNeeded(fromPath, toPath, attrs);
		} catch (final IOException e) {
			logger.warn("Failed to copy directory " + fromPath.toAbsolutePath(), e);
			return FileVisitResult.SKIP_SUBTREE;
		}

		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(final Path fromPath, final BasicFileAttributes attrs) {
		final Path toPath = resolveToPath(fromPath);

		try {
			copyIfNeeded(fromPath, toPath, attrs);
		} catch (final IOException e) {
			logger.warn("Failed to copy file " + fromPath.toAbsolutePath(), e);
		}

		return FileVisitResult.CONTINUE;
	}

}
