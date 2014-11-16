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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.mattrero.foldersync.impl.path.PathSynchronizer;
import ch.mattrero.foldersync.impl.path.SourceSyncVisitor;

public class SourceSyncVisitorTest {

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
	public void should_call_synchronizer() throws IOException {
		// Given
		Files.createDirectories(sourceDir.resolve("folder").resolve("subfolder"));
		FileUtils.writeStringToFile(sourceDir.resolve("file1").toFile(), "data1");
		FileUtils.writeStringToFile(sourceDir.resolve("folder").resolve("file2").toFile(), "data2");
		FileUtils
				.writeStringToFile(sourceDir.resolve("folder").resolve("subfolder").resolve("file3").toFile(), "data3");

		final PathSynchronizer pathSynchronizer = mock(PathSynchronizer.class);

		// When
		Files.walkFileTree(sourceDir, new SourceSyncVisitor(pathSynchronizer, sourceDir, destinationDir));

		// Then
		verify(pathSynchronizer).syncLevel(sourceDir.resolve("file1"), destinationDir.resolve("file1"));

		verify(pathSynchronizer).syncLevel(sourceDir.resolve("folder"), destinationDir.resolve("folder"));
		verify(pathSynchronizer).syncLevel(sourceDir.resolve("folder").resolve("file2"),
				destinationDir.resolve("folder").resolve("file2"));

		verify(pathSynchronizer).syncLevel(sourceDir.resolve("folder").resolve("subfolder"),
				destinationDir.resolve("folder").resolve("subfolder"));
		verify(pathSynchronizer).syncLevel(sourceDir.resolve("folder").resolve("subfolder").resolve("file3"),
				destinationDir.resolve("folder").resolve("subfolder").resolve("file3"));

		verifyNoMoreInteractions(pathSynchronizer);
	}

}
