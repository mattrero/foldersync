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

import static ch.mattrero.foldersync.SyncStatus.ADDED;
import static ch.mattrero.foldersync.SyncStatus.DELETED;
import static ch.mattrero.foldersync.SyncStatus.MODIFIED;
import static ch.mattrero.foldersync.SyncStatus.SYNCHRONIZED;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FoldersSynchronizer {

	final Logger logger = LoggerFactory.getLogger(FoldersSynchronizer.class);

	private final Path sourceDir;
	private final Path backupDir;

	public FoldersSynchronizer(final Path sourceDir, final Path backupDir) {
		this.sourceDir = sourceDir;
		this.backupDir = backupDir;
	}

	public void sync() {
		syncTree(sourceDir);
	}

	private Path resolveBackupItemPath(final Path sourceItemPath) {
		return backupDir.resolve(sourceDir.relativize(sourceItemPath));
	}

	void syncTree(final Path sourceSubDir) {

		SyncStatus status = null;
		BasicFileAttributes fromAttributes = null;
		BasicFileAttributes toAttributes = null;

		try (final DirectoryStream<Path> sourceStream = Files.newDirectoryStream(sourceSubDir);
				DirectoryStream<Path> backupStream = Files.newDirectoryStream(resolveBackupItemPath(sourceSubDir))) {

			final Iterator<Path> sourceIterator = sourceStream.iterator();
			final Iterator<Path> backupIterator = backupStream.iterator();

			Path sourceItem = (sourceIterator.hasNext() ? sourceIterator.next() : null);
			Path backupItem = (backupIterator.hasNext() ? backupIterator.next() : null);

			while (sourceItem != null || backupItem != null) {
				if (sourceItem == null) {
					status = DELETED;
				} else if (backupItem == null) {
					status = ADDED;
				} else if (sourceDir.relativize(sourceItem).compareTo(backupDir.relativize(backupItem)) < 0) {
					status = ADDED;
				} else if (sourceDir.relativize(sourceItem).compareTo(backupDir.relativize(backupItem)) > 0) {
					status = DELETED;
				} else if (Files.isDirectory(sourceItem) != Files.isDirectory(backupItem)) {
					status = MODIFIED;
				} else if (Files.isDirectory(sourceItem)) {
					status = SYNCHRONIZED;
				} else {
					fromAttributes = Files.readAttributes(sourceItem, BasicFileAttributes.class);
					toAttributes = Files.readAttributes(backupItem, BasicFileAttributes.class);

					if (Math.abs(fromAttributes.lastModifiedTime().toMillis()
							- toAttributes.lastModifiedTime().toMillis()) > 0) {
						status = MODIFIED;
					} else if (fromAttributes.size() != toAttributes.size()) {
						status = MODIFIED;
					} else {
						status = SYNCHRONIZED;
					}
				}

				switch (status) {
					case ADDED:
						syncAdded(sourceItem);
						sourceItem = (sourceIterator.hasNext() ? sourceIterator.next() : null);
						break;
					case DELETED:
						syncDeleted(sourceDir.resolve(backupDir.relativize(backupItem)));
						backupItem = (backupIterator.hasNext() ? backupIterator.next() : null);
						break;
					case MODIFIED:
						syncModified(sourceItem);
					case SYNCHRONIZED:
					default:
						if (Files.isDirectory(sourceItem)) {
							syncTree(sourceItem);
						}
						sourceItem = (sourceIterator.hasNext() ? sourceIterator.next() : null);
						backupItem = (backupIterator.hasNext() ? backupIterator.next() : null);
						break;
				}
			}

		} catch (final IOException | SecurityException e) {
			logger.debug("Failed to sync tree " + sourceSubDir, e);
		}
	}

	boolean syncAdded(final Path sourceItem) {
		try {
			if (Files.isDirectory(sourceItem)) {
				FileUtils.copyDirectory(sourceItem.toFile(), resolveBackupItemPath(sourceItem).toFile(), true);
				logger.debug("Added directory " + sourceItem);
			} else {
				Files.copy(sourceItem, resolveBackupItemPath(sourceItem), COPY_ATTRIBUTES, REPLACE_EXISTING);
				logger.debug("Added file " + sourceItem);
			}
		} catch (final IOException e) {
			logger.warn("Failed to create " + resolveBackupItemPath(sourceItem), e);
			return false;
		}

		return true;
	}

	boolean syncModified(final Path sourceItem) {
		return syncDeleted(sourceItem) && syncAdded(sourceItem);
	}

	boolean syncDeleted(final Path sourceItem) {
		final Path backupItem = resolveBackupItemPath(sourceItem);

		try {
			if (Files.isDirectory(backupItem)) {
				FileUtils.deleteDirectory(backupItem.toFile());
				logger.debug("Deleted directory " + backupItem);
			} else {
				Files.delete(backupItem);
				logger.debug("Deleted file " + backupItem);
			}
		} catch (final IOException e) {
			logger.warn("Failed to delete " + backupItem, e);
			return false;
		}

		return true;
	}

	public Path getSourceDir() {
		return sourceDir;
	}

	public Path getBackupDir() {
		return backupDir;
	}

}
