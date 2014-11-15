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

public interface ISynchronizer<FROM, TO> {

	public SyncStatus getSyncStatus(FROM from, TO to) throws Exception;

	public void sync(FROM from, TO to) throws Exception;

	/**
	 * Synchronize at this level only.<br/>
	 * Use {@link #sync(FROM, TO)} to sync the whole tree in depth.
	 * 
	 * 
	 * @param from
	 * @param to
	 * @return
	 * @throws Exception
	 */
	public SyncStatus syncLevel(FROM from, TO to) throws Exception;

}
