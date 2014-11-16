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
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import ch.mattrero.foldersync.SyncStatus;
import ch.mattrero.foldersync.impl.path.PathSynchronizer;

public class PathSynchronizerTest {

	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();

	public Path fromPath;
	public Path toPath;

	@Before
	public void setUp() throws IOException {
		fromPath = Paths.get(testFolder.newFolder().toURI()).resolve("dummy");
		toPath = Paths.get(testFolder.newFolder().toURI()).resolve("dummy");
	}

	// -------------------------------------------------------------------------
	// getSyncStatus tests
	// -------------------------------------------------------------------------

	@Test
	public void should_return_status_SYNCHRONIZED_when_folders_are_same() throws IOException {
		Files.createDirectory(fromPath);
		Files.createDirectory(toPath);

		final SyncStatus syncStatus = new PathSynchronizer(1000L).getSyncStatus(fromPath, toPath);

		assertThat(syncStatus).isEqualTo(SYNCHRONIZED);
	}

	@Test
	public void should_return_status_SYNCHRONIZED_when_files_are_same() throws IOException {
		FileUtils.writeStringToFile(fromPath.toFile(), "dummy data");
		FileUtils.writeStringToFile(toPath.toFile(), "dummy data");

		final SyncStatus syncStatus = new PathSynchronizer(1000L).getSyncStatus(fromPath, toPath);

		assertThat(syncStatus).isEqualTo(SYNCHRONIZED);
	}

	@Test
	public void should_return_status_ADDED_when_new_folder_exist() throws IOException {
		Files.createDirectory(fromPath);

		final SyncStatus syncStatus = new PathSynchronizer(1000L).getSyncStatus(fromPath, toPath);

		assertThat(syncStatus).isEqualTo(ADDED);
	}

	@Test
	public void should_return_status_ADDED_when_new_file_exist() throws IOException {
		FileUtils.writeStringToFile(fromPath.toFile(), "dummy data");

		final SyncStatus syncStatus = new PathSynchronizer(1000L).getSyncStatus(fromPath, toPath);

		assertThat(syncStatus).isEqualTo(ADDED);
	}

	@Test
	public void should_return_status_MODIFIED_when_a_folder_is_now_a_file() throws IOException {
		FileUtils.writeStringToFile(fromPath.toFile(), "dummy data");
		Files.createDirectory(toPath);

		final SyncStatus syncStatus = new PathSynchronizer(1000L).getSyncStatus(fromPath, toPath);

		assertThat(syncStatus).isEqualTo(MODIFIED);
	}

	@Test
	public void should_return_status_MODIFIED_when_a_file_is_now_a_folder() throws IOException {
		Files.createDirectory(fromPath);
		FileUtils.writeStringToFile(toPath.toFile(), "dummy data");

		final SyncStatus syncStatus = new PathSynchronizer(1000L).getSyncStatus(fromPath, toPath);

		assertThat(syncStatus).isEqualTo(MODIFIED);
	}

	@Test
	public void should_return_status_MODIFIED_when_a_file_size_are_different() throws IOException {
		FileUtils.writeStringToFile(fromPath.toFile(), "dummy data");
		FileUtils.writeStringToFile(toPath.toFile(), "dummy data 2");

		final SyncStatus syncStatus = new PathSynchronizer(1000L).getSyncStatus(fromPath, toPath);

		assertThat(syncStatus).isEqualTo(MODIFIED);
	}

	@Test
	public void should_return_status_MODIFIED_when_file_is_newer() throws IOException, InterruptedException {
		FileUtils.writeStringToFile(toPath.toFile(), "dummy data");
		Thread.sleep(100);
		FileUtils.writeStringToFile(fromPath.toFile(), "dummy data");

		final SyncStatus syncStatus = new PathSynchronizer(0L).getSyncStatus(fromPath, toPath);

		assertThat(syncStatus).isEqualTo(MODIFIED);
	}

	@Test
	public void should_return_status_MODIFIED_when_file_is_older() throws IOException, InterruptedException {
		FileUtils.writeStringToFile(fromPath.toFile(), "dummy data");
		Thread.sleep(100);
		FileUtils.writeStringToFile(toPath.toFile(), "dummy data");

		final SyncStatus syncStatus = new PathSynchronizer(0L).getSyncStatus(fromPath, toPath);

		assertThat(syncStatus).isEqualTo(MODIFIED);
	}

	@Test
	public void should_return_status_SYNCHRONIZED_when_file_is_newer_but_in_tolerance_range() throws IOException,
			InterruptedException {
		FileUtils.writeStringToFile(fromPath.toFile(), "dummy data");
		Thread.sleep(100);
		FileUtils.writeStringToFile(toPath.toFile(), "dummy data");

		final SyncStatus syncStatus = new PathSynchronizer(500L).getSyncStatus(fromPath, toPath);

		assertThat(syncStatus).isEqualTo(SYNCHRONIZED);
	}

	@Test
	public void should_return_status_DELETED_when_folder_does_not_exist_anymore() throws IOException {
		Files.createDirectory(toPath);

		final SyncStatus syncStatus = new PathSynchronizer(1000L).getSyncStatus(fromPath, toPath);

		assertThat(syncStatus).isEqualTo(DELETED);
	}

	@Test
	public void should_return_status_DELETED_when_file_does_not_exist_anymore() throws IOException {
		FileUtils.writeStringToFile(toPath.toFile(), "dummy data");

		final SyncStatus syncStatus = new PathSynchronizer(1000L).getSyncStatus(fromPath, toPath);

		assertThat(syncStatus).isEqualTo(DELETED);
	}

	// -------------------------------------------------------------------------
	// syncLevel tests
	// -------------------------------------------------------------------------

	@Test
	public void should_do_nothing_when_status_is_SYNCHRONIZED() throws IOException {
		FileUtils.writeStringToFile(fromPath.toFile(), "dummy data");
		FileUtils.writeStringToFile(toPath.toFile(), "dummy data2");

		final PathSynchronizer pathSynchronizer = mock(PathSynchronizer.class, Mockito.CALLS_REAL_METHODS);
		doReturn(SYNCHRONIZED).when(pathSynchronizer).getSyncStatus(any(Path.class), any(Path.class));

		final SyncStatus syncLevel = pathSynchronizer.syncLevel(fromPath, toPath);

		assertThat(syncLevel).isEqualTo(SYNCHRONIZED);

		assertThat(Files.exists(toPath)).isTrue();
		assertThat(Files.isDirectory(toPath)).isFalse();
		assertThat(FileUtils.readFileToString(toPath.toFile())).isEqualTo("dummy data2");
	}

	@Test
	public void should_delete_folder_when_status_is_DELETED() throws IOException {
		FileUtils.writeStringToFile(fromPath.toFile(), "dummy data");
		Files.createDirectory(toPath);
		FileUtils.writeStringToFile(toPath.resolve("dummysubfile").toFile(), "dummy data2");

		final PathSynchronizer pathSynchronizer = mock(PathSynchronizer.class, Mockito.CALLS_REAL_METHODS);
		doReturn(DELETED).when(pathSynchronizer).getSyncStatus(any(Path.class), any(Path.class));

		final SyncStatus syncLevel = pathSynchronizer.syncLevel(fromPath, toPath);

		assertThat(syncLevel).isEqualTo(DELETED);
		assertThat(Files.exists(toPath)).isFalse();
	}

	@Test
	public void should_delete_file_when_status_is_DELETED() throws IOException {
		FileUtils.writeStringToFile(fromPath.toFile(), "dummy data");
		FileUtils.writeStringToFile(toPath.toFile(), "dummy data2");

		final PathSynchronizer pathSynchronizer = mock(PathSynchronizer.class, Mockito.CALLS_REAL_METHODS);
		doReturn(DELETED).when(pathSynchronizer).getSyncStatus(any(Path.class), any(Path.class));

		final SyncStatus syncLevel = pathSynchronizer.syncLevel(fromPath, toPath);

		assertThat(syncLevel).isEqualTo(DELETED);
		assertThat(Files.exists(toPath)).isFalse();
	}

	@Test
	public void should_add_file_when_status_is_ADDED() throws IOException {
		FileUtils.writeStringToFile(fromPath.toFile(), "dummy data");

		final PathSynchronizer pathSynchronizer = mock(PathSynchronizer.class, Mockito.CALLS_REAL_METHODS);
		doReturn(ADDED).when(pathSynchronizer).getSyncStatus(any(Path.class), any(Path.class));

		final SyncStatus syncLevel = pathSynchronizer.syncLevel(fromPath, toPath);

		assertThat(syncLevel).isEqualTo(ADDED);

		assertThat(Files.exists(toPath)).isTrue();
		assertThat(Files.isDirectory(toPath)).isFalse();
		assertThat(FileUtils.readFileToString(toPath.toFile())).isEqualTo("dummy data");
	}

	@Test
	public void should_add_folder_when_status_is_ADDED() throws IOException {
		Files.createDirectory(fromPath);

		final PathSynchronizer pathSynchronizer = mock(PathSynchronizer.class, Mockito.CALLS_REAL_METHODS);
		doReturn(ADDED).when(pathSynchronizer).getSyncStatus(any(Path.class), any(Path.class));

		final SyncStatus syncLevel = pathSynchronizer.syncLevel(fromPath, toPath);

		assertThat(syncLevel).isEqualTo(ADDED);

		assertThat(Files.exists(toPath)).isTrue();
		assertThat(Files.isDirectory(toPath)).isTrue();
	}

	@Test
	public void should_modified_file_when_status_is_MODIFIED() throws IOException {
		FileUtils.writeStringToFile(fromPath.toFile(), "dummy data1");
		FileUtils.writeStringToFile(toPath.toFile(), "dummy data2");

		final PathSynchronizer pathSynchronizer = mock(PathSynchronizer.class, Mockito.CALLS_REAL_METHODS);
		doReturn(MODIFIED).when(pathSynchronizer).getSyncStatus(any(Path.class), any(Path.class));

		final SyncStatus syncLevel = pathSynchronizer.syncLevel(fromPath, toPath);

		assertThat(syncLevel).isEqualTo(MODIFIED);

		assertThat(Files.exists(toPath)).isTrue();
		assertThat(Files.isDirectory(toPath)).isFalse();
		assertThat(FileUtils.readFileToString(toPath.toFile())).isEqualTo("dummy data1");
	}

}
