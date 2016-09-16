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

import static org.fest.assertions.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FolderSyncTest {

	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	Path sourceDir;
	Path backupDir;

	@Before
	public void setUp() throws IOException {
		sourceDir = temporaryFolder.newFolder().toPath();
		backupDir = temporaryFolder.newFolder().toPath();
	}

	@Test
	public void should_add_new_paths() throws IOException {
		//Given
		FileUtils.writeStringToFile(sourceDir.resolve("file").toFile(), "data");

		Files.createDirectory(sourceDir.resolve("folder"));
		FileUtils.writeStringToFile(sourceDir.resolve("folder").resolve("file2").toFile(), "data2");

		Files.createDirectory(sourceDir.resolve("folder").resolve("subfolder"));

		Files.createDirectories(backupDir.resolve("folder"));
		FileUtils.writeStringToFile(backupDir.resolve("folder").resolve("file3").toFile(), "data3");

		//When
		new FolderSync().sync(sourceDir, backupDir);

		//Then
		assertThat(Files.exists(backupDir.resolve("file"))).isTrue();
		assertThat(FileUtils.readFileToString(backupDir.resolve("file").toFile())).isEqualTo("data");

		assertThat(Files.exists(backupDir.resolve("folder"))).isTrue();
		assertThat(Files.exists(backupDir.resolve("folder").resolve("file2"))).isTrue();
		assertThat(FileUtils.readFileToString(backupDir.resolve("folder").resolve("file2").toFile()))
				.isEqualTo("data2");

		assertThat(Files.exists(backupDir.resolve("folder").resolve("subfolder"))).isTrue();
	}

	@Test
	public void should_delete_non_existing_paths() throws IOException {
		//Given
		Files.createDirectory(sourceDir.resolve("folder"));
		Files.createDirectory(sourceDir.resolve("folder3"));

		FileUtils.writeStringToFile(backupDir.resolve("file2").toFile(), "data2");
		Files.createDirectories(backupDir.resolve("folder2"));
		FileUtils.writeStringToFile(backupDir.resolve("folder2").resolve("file").toFile(), "data");
		Files.createDirectories(backupDir.resolve("folder").resolve("subfolder"));

		//When
		new FolderSync().sync(sourceDir, backupDir);

		//Then
		assertThat(Files.exists(backupDir.resolve("file2"))).isFalse();
		assertThat(Files.exists(backupDir.resolve("folder"))).isTrue();
		assertThat(Files.exists(backupDir.resolve("folder").resolve("subfolder"))).isFalse();
		assertThat(Files.exists(backupDir.resolve("folder2"))).isFalse();
		assertThat(Files.exists(backupDir.resolve("folder3"))).isTrue();
	}

	@Test
	public void should_replace_when_a_folder_is_now_a_file() throws IOException {
		//Given
		FileUtils.writeStringToFile(sourceDir.resolve("file").toFile(), "data");
		Files.createDirectory(backupDir.resolve("file"));

		//When
		new FolderSync().sync(sourceDir, backupDir);

		//Then
		assertThat(Files.exists(backupDir.resolve("file"))).isTrue();
		assertThat(Files.isDirectory(backupDir.resolve("file"))).isFalse();
		FileUtils.writeStringToFile(backupDir.resolve("file").toFile(), "data");
	}

	@Test
	public void should_replace_when_a_file_is_now_a_folder() throws IOException {
		//Given
		Files.createDirectory(sourceDir.resolve("file"));
		FileUtils.writeStringToFile(backupDir.resolve("file").toFile(), "data");

		//When
		new FolderSync().sync(sourceDir, backupDir);

		//Then
		assertThat(Files.exists(backupDir.resolve("file"))).isTrue();
		assertThat(Files.isDirectory(backupDir.resolve("file"))).isTrue();
	}

	@Test
	public void should_replace_when_file_size_are_different() throws IOException {
		//Given
		FileUtils.writeStringToFile(sourceDir.resolve("file").toFile(), "data");
		FileUtils.writeStringToFile(backupDir.resolve("file").toFile(), "olddata");
		Files.setLastModifiedTime(backupDir.resolve("file"),
				FileTime.fromMillis(Files.getLastModifiedTime(sourceDir.resolve("file")).toMillis()));

		//When
		new FolderSync().sync(sourceDir, backupDir);

		//Then
		assertThat(Files.exists(backupDir.resolve("file"))).isTrue();
		FileUtils.writeStringToFile(backupDir.resolve("file").toFile(), "data");
	}

	//In real-life we should never have : same size, same modificationDate and different content
	@Test
	public void should_do_nothing_when_file_have_same_size_and_age() throws IOException {
		//Given
		FileUtils.writeStringToFile(sourceDir.resolve("file").toFile(), "newdata");
		FileUtils.writeStringToFile(backupDir.resolve("file").toFile(), "olddata");
		Files.setLastModifiedTime(backupDir.resolve("file"),
				FileTime.fromMillis(Files.getLastModifiedTime(sourceDir.resolve("file")).toMillis()));

		//When
		new FolderSync().sync(sourceDir, backupDir);

		//Then
		assertThat(Files.exists(backupDir.resolve("file"))).isTrue();
		FileUtils.writeStringToFile(backupDir.resolve("file").toFile(), "olddata");
	}

	@Test
	public void should_replace_when_file_is_newer() throws IOException {
		//Given
		FileUtils.writeStringToFile(sourceDir.resolve("file").toFile(), "newdata");
		FileUtils.writeStringToFile(backupDir.resolve("file").toFile(), "olddata");
		Files.setLastModifiedTime(backupDir.resolve("file"),
				FileTime.fromMillis(Files.getLastModifiedTime(sourceDir.resolve("file")).toMillis() + 1000));

		//When
		new FolderSync().sync(sourceDir, backupDir);

		//Then
		assertThat(Files.exists(backupDir.resolve("file"))).isTrue();
		FileUtils.writeStringToFile(backupDir.resolve("file").toFile(), "newdata");
	}

	@Test
	public void should_replace_when_file_is_older() throws IOException {
		//Given
		FileUtils.writeStringToFile(sourceDir.resolve("file").toFile(), "newdata");
		FileUtils.writeStringToFile(backupDir.resolve("file").toFile(), "olddata");
		Files.setLastModifiedTime(backupDir.resolve("file"),
				FileTime.fromMillis(Files.getLastModifiedTime(sourceDir.resolve("file")).toMillis() - 1000));

		//When
		new FolderSync().sync(sourceDir, backupDir);

		//Then
		assertThat(Files.exists(backupDir.resolve("file"))).isTrue();
		FileUtils.writeStringToFile(backupDir.resolve("file").toFile(), "newdata");
	}

}
