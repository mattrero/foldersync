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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SourceFolderVisitorTest {

	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();

	public Path sourceDir;
	public Path destinationDir;

	@Before
	public void setUp() throws IOException {
		sourceDir = Paths.get(testFolder.newFolder().toURI());
		destinationDir = Paths.get(testFolder.newFolder().toURI());
	}

	@Test
	public void should_copy_non_existing_file() throws IOException {
		// Given
		FileUtils.writeStringToFile(new File(sourceDir.toFile(), "dummyfile"), "dummy data");

		// When
		Files.walkFileTree(sourceDir, new SourceFolderVisitor(sourceDir, destinationDir));

		// Then
		final File expectedFile = new File(destinationDir.toFile(), "dummyfile");

		assertThat(expectedFile.exists()).isTrue();
		assertThat(FileUtils.readFileToString(expectedFile)).isEqualTo("dummy data");
	}

	@Test
	public void should_copy_non_existing_folder() throws IOException {
		// Given
		Files.createDirectory(sourceDir.resolve("dummyfolder"));

		// When
		Files.walkFileTree(sourceDir, new SourceFolderVisitor(sourceDir, destinationDir));

		// Then
		final Path expectedFolder = destinationDir.resolve("dummyfolder");

		assertThat(Files.exists(expectedFolder)).isTrue();
	}

	@Test
	public void should_copy_recursively_non_existing_files() throws IOException {
		// Given
		final Path subfolder = sourceDir.resolve("dummyfolder").resolve("dummyfolder2");
		Files.createDirectories(subfolder);
		FileUtils.writeStringToFile(new File(subfolder.toFile(), "dummyfile"), "dummy data");

		// When
		Files.walkFileTree(sourceDir, new SourceFolderVisitor(sourceDir, destinationDir));

		// Then
		final File expectedFile = new File(destinationDir.resolve("dummyfolder").resolve("dummyfolder2").toFile(),
				"dummyfile");

		assertThat(expectedFile.exists()).isTrue();
		assertThat(FileUtils.readFileToString(expectedFile)).isEqualTo("dummy data");
	}

	@Test
	public void should_replace_directory_by_file() throws IOException {
		// Given
		FileUtils.writeStringToFile(new File(sourceDir.toFile(), "dummyfile"), "dummy data");
		Files.createDirectory(destinationDir.resolve("dummyfile"));
		FileUtils
				.writeStringToFile(new File(destinationDir.resolve("dummyfile").toFile(), "dummyfile2"), "dummy data2");

		// When
		Files.walkFileTree(sourceDir, new SourceFolderVisitor(sourceDir, destinationDir));

		// Then
		final File expectedFile = new File(destinationDir.toFile(), "dummyfile");

		assertThat(expectedFile.exists()).isTrue();
		assertThat(expectedFile.isFile()).isTrue();
		assertThat(FileUtils.readFileToString(expectedFile)).isEqualTo("dummy data");
	}

	@Test
	public void should_replace_file_by_directory() throws IOException {
		// Given
		Files.createDirectory(sourceDir.resolve("dummyfolder"));
		FileUtils.writeStringToFile(new File(destinationDir.toFile(), "dummyfolder"), "dummy data");

		// When
		Files.walkFileTree(sourceDir, new SourceFolderVisitor(sourceDir, destinationDir));

		// Then
		final File expectedFile = new File(destinationDir.toFile(), "dummyfolder");

		assertThat(expectedFile.exists()).isTrue();
		assertThat(expectedFile.isDirectory()).isTrue();
	}

	@Test
	public void should_replace_old_file() throws IOException, InterruptedException {
		// Given
		final Path expectedPath = destinationDir.resolve("dummyfile");

		FileUtils.writeStringToFile(expectedPath.toFile(), "dummy data");
		Thread.sleep(100);
		FileUtils.writeStringToFile(new File(sourceDir.toFile(), "dummyfile"), "dummy data");

		final long oldModificationTime = Files.readAttributes(expectedPath, BasicFileAttributes.class)
				.lastModifiedTime().toMillis();

		// When
		Files.walkFileTree(sourceDir, new SourceFolderVisitor(sourceDir, destinationDir));

		// Then
		assertThat(expectedPath.toFile().exists()).isTrue();

		final long newModificationTime = Files.readAttributes(expectedPath, BasicFileAttributes.class)
				.lastModifiedTime().toMillis();
		assertThat(newModificationTime).isGreaterThan(oldModificationTime);
	}

	@Test
	public void should_not_replace_old_file_within_tolerance_range() throws IOException, InterruptedException {
		// Given
		final Path expectedPath = destinationDir.resolve("dummyfile");

		FileUtils.writeStringToFile(expectedPath.toFile(), "dummy data");
		Thread.sleep(100);
		FileUtils.writeStringToFile(new File(sourceDir.toFile(), "dummyfile"), "dummy data");

		final long oldModificationTime = Files.readAttributes(expectedPath, BasicFileAttributes.class)
				.lastModifiedTime().toMillis();

		// When
		Files.walkFileTree(sourceDir, new SourceFolderVisitor(sourceDir, destinationDir, 500L));

		// Then
		assertThat(expectedPath.toFile().exists()).isTrue();

		final long newModificationTime = Files.readAttributes(expectedPath, BasicFileAttributes.class)
				.lastModifiedTime().toMillis();
		assertThat(newModificationTime).isEqualTo(oldModificationTime);
	}

	@Test
	public void should_replace_file_with_different_size() throws IOException {
		// Given
		FileUtils.writeStringToFile(new File(destinationDir.toFile(), "dummyfile"), "dummy data");
		FileUtils.writeStringToFile(new File(sourceDir.toFile(), "dummyfile"), "new dummy data");

		// When
		Files.walkFileTree(sourceDir, new SourceFolderVisitor(sourceDir, destinationDir));

		// Then
		final File expectedFile = new File(destinationDir.toFile(), "dummyfile");

		assertThat(expectedFile.exists()).isTrue();
		assertThat(FileUtils.readFileToString(expectedFile)).isEqualTo("new dummy data");
	}
}
