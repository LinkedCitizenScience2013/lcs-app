/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */
package org.envirocar.core.logging;

import android.content.Context;
import android.util.Log;

import org.acra.collector.CrashReportData;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;

public class ACRACustomSender implements ReportSender {

	private static final Logger logger = Logger.getLogger(ACRACustomSender.class);
	
    public ACRACustomSender(){
    }

    @Override
    public void send(Context context, CrashReportData report) throws ReportSenderException {
    	Log.e("acra", "Receiving an app crash: "+ report.toString());
    	logger.severe(report.toString());
    	logger.severe("[END OF ACRA REPORT]");
    }
    
}