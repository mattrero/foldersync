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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderSync {

	//TODO handle rename

	public void sync(final Path sourceDir, final Path backupDir) {
		new FoldersSynchronizer(sourceDir, backupDir).sync();
	}

	public IRealTimeSynchronizer realTimeSync(final Path sourceDir, final Path backupDir) throws IOException {
		final FoldersRealTimeSynchronizer foldersRealTimeSynchronizer = new FoldersRealTimeSynchronizer(
				new FoldersSynchronizer(sourceDir, backupDir));
		foldersRealTimeSynchronizer.start();

		while (!foldersRealTimeSynchronizer.isRunning()) {
			try {
				Thread.sleep(1000);
			} catch (final InterruptedException e) {
			}
		}

		/*
		 * call this to shutdown cleanly the thread
		 * 
		 * IRealTimeSynchronizer.shutdownNow();
		 * 
		 */

		return foldersRealTimeSynchronizer;
	}

	public static void main(final String[] args) {
		final Logger logger = LoggerFactory.getLogger(FolderSync.class);

		if (args[0].startsWith("-")) {
			final Path fromDir = Paths.get(args[1]);
			final Path toDir = Paths.get(args[2]);

			logger.info("Starting real-time synchronizer ...");

			try {
				new FolderSync().realTimeSync(fromDir, toDir);
			} catch (final IOException e) {
				logger.warn(
						"Failed to start real-time sync for folders " + fromDir.toAbsolutePath() + " => "
								+ toDir.toAbsolutePath(), e);
			}

			logger.info("Real-time synchronizer started");

			return;
		}

		final Path fromDir = Paths.get(args[0]);
		final Path toDir = Paths.get(args[1]);

		logger.info("Start synchronizing folders ...");
		logger.info("  => from : {}", fromDir.toAbsolutePath());
		logger.info("  => to   : {}", toDir.toAbsolutePath());

		final long startDate = System.currentTimeMillis();

		new FolderSync().sync(fromDir, toDir);

		final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss:SSS");
		format.setTimeZone(TimeZone.getTimeZone("GMT"));

		logger.info("Finished synchronizing folders in {} secs", format.format(System.currentTimeMillis() - startDate));
	}
}
