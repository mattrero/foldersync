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

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DestinationFolderVisitorTest {

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
	public void should_delete_folder_not_existing_anymore() throws IOException {
		// Given
		Files.createDirectory(destinationDir.resolve("dummyfolder"));
		FileUtils
				.writeStringToFile(new File(destinationDir.resolve("dummyfolder").toFile(), "dummyfile"), "dummy data");

		// When
		Files.walkFileTree(destinationDir, new DestinationFolderVisitor(sourceDir, destinationDir));

		// Then
		final Path expectedFolder = destinationDir.resolve("dummyfolder");

		assertThat(Files.exists(expectedFolder)).isFalse();
	}

	@Test
	public void should_delete_file_not_existing_anymore() throws IOException {
		// Given
		final File expectedFile = new File(destinationDir.toFile(), "dummyfile");
		FileUtils.writeStringToFile(expectedFile, "dummy data");

		// When
		Files.walkFileTree(destinationDir, new DestinationFolderVisitor(sourceDir, destinationDir));

		// Then
		assertThat(expectedFile.exists()).isFalse();
	}

}
