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

import static ch.mattrero.foldersync.SyncStatus.ADDED;
import static ch.mattrero.foldersync.SyncStatus.DELETED;
import static ch.mattrero.foldersync.SyncStatus.MODIFIED;
import static ch.mattrero.foldersync.SyncStatus.SYNCHRONIZED;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.commons.io.FileUtils;

import ch.mattrero.foldersync.ISynchronizer;
import ch.mattrero.foldersync.SyncStatus;

public class PathSynchronizer implements ISynchronizer<Path, Path> {

	private final long modifiedTimeTolerance;

	public PathSynchronizer() {
		this(0L);
	}

	public PathSynchronizer(final long modifiedTimeTolerance) {
		this.modifiedTimeTolerance = modifiedTimeTolerance;
	}

	@Override
	public SyncStatus getSyncStatus(final Path fromPath, final Path toPath) throws IOException {
		if (Files.exists(fromPath)) {
			if (!Files.exists(toPath)) {
				return ADDED;
			}

			if (Files.isDirectory(fromPath) != Files.isDirectory(toPath)) {
				return MODIFIED;
			}

			if (!Files.isDirectory(fromPath)) {
				final BasicFileAttributes fromAttributes = Files.readAttributes(fromPath, BasicFileAttributes.class);
				final BasicFileAttributes toAttributes = Files.readAttributes(toPath, BasicFileAttributes.class);

				if (Math.abs(fromAttributes.lastModifiedTime().toMillis() - toAttributes.lastModifiedTime().toMillis()) > modifiedTimeTolerance) {
					return MODIFIED;
				}

				if (fromAttributes.size() != toAttributes.size()) {
					return MODIFIED;
				}
			}
		} else if (Files.exists(toPath)) {
			return DELETED;
		}

		return SYNCHRONIZED;
	}

	@Override
	public void sync(final Path fromPath, final Path toPath) throws IOException {
		Files.walkFileTree(fromPath, new SourceFolderVisitor(this, fromPath, toPath));
		Files.walkFileTree(toPath, new DestinationFolderVisitor(this, fromPath, toPath));
	}

	@Override
	public SyncStatus syncLevel(final Path fromPath, final Path toPath) throws IOException {
		final SyncStatus syncStatus = getSyncStatus(fromPath, toPath);

		if (syncStatus == SYNCHRONIZED) {
			return SYNCHRONIZED;
		}

		if (syncStatus == DELETED || syncStatus == MODIFIED) {
			if (Files.isDirectory(toPath)) {
				FileUtils.deleteDirectory(toPath.toFile());
			} else {
				Files.delete(toPath);
			}
		}

		if (syncStatus == ADDED || syncStatus == MODIFIED) {
			Files.copy(fromPath, toPath, COPY_ATTRIBUTES, REPLACE_EXISTING);
		}

		return syncStatus;
	}
}
