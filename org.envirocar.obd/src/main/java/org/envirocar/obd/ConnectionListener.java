/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.obd;

import org.envirocar.obd.OBDController;

import java.io.IOException;

/**
 * Interface is used by the {@link OBDController} to provide
 * information on the connection state.
 * 
 * @author matthes rieke
 *
 */
public interface ConnectionListener {
	
	/**
	 * a serious exception occured while using the connection
	 * 
	 * @param e the exception
	 */
//	public void onConnectionException(IOException e);

	/**
	 * called when the connetion is verified (= a useful response
	 * was received)
	 */
    void onConnectionVerified();

	/**
	 * called when the connection is established but no PIDs have been returned (i.e. engine not runngin)
	 */
	void onEngineNotRunning();

	/**
	 * called when all registered adapters failed to create a connection
	 */
    void onAllAdaptersFailed();

	/**
	 * @param message the status update message
	 */
    void onStatusUpdate(String message);

	/**
	 * called when the underlying adpater wants to re-establish the connection
	 * @param reason 
	 */
    void requestConnectionRetry(IOException reason);

}
