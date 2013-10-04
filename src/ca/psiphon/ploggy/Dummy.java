/*
 * Copyright (c) 2013, Psiphon Inc.
 * All rights reserved.
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
 *
 */

package ca.psiphon.ploggy;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class Dummy {
	
	private static Timer mTimer;

	public static void doDummyData() {
	    /*
		try {
			Data data = Data.getInstance();
			for (int i = 0; i < 10; i++) {
			    String nickname = String.format("Nickname%02d", i);
                X509.KeyMaterial x509KeyMaterial = X509.generateKeyMaterial();
                //HiddenService.KeyMaterial hiddenServiceKeyMaterial = HiddenService.generateKeyMaterial();
                HiddenService.KeyMaterial hiddenServiceKeyMaterial = new HiddenService.KeyMaterial(Utils.getRandomHexString(1024), Utils.getRandomHexString(1024));
			    data.insertOrUpdateFriend(
			            new Data.Friend(new Identity.PublicIdentity(nickname, x509KeyMaterial.mCertificate, hiddenServiceKeyMaterial.mHostname, "")));
			}
		} catch (Utils.ApplicationError e) {
			// TODO: ...
		}
		*/

		mTimer = new Timer();
		/*
		mTimer.schedule(
                new TimerTask() {          
                    @Override
                    public void run() {
                    	Log.addEntry("tag", "message");
                    }
                },
                1000,
                1000);
        */
        mTimer.schedule(
                new TimerTask() {          
                    @Override
                    public void run() {
                        try {
                            Log.addEntry("DUMMY", "GENERATE X509");
                            X509.KeyMaterial x509KeyMaterial = X509.generateKeyMaterial();
                            Log.addEntry("DUMMY", "COMPLETED GENERATE X509");
                            Log.addEntry("DUMMY", "START WEB SERVER");
                            WebServer webServer = new WebServer(x509KeyMaterial);
                            try {
                                webServer.start();
                            } catch (IOException e) {
                                throw new Utils.ApplicationError(e);
                            }
                            Log.addEntry("DUMMY", "COMPLETED START WEB SERVER");
                            Log.addEntry("DUMMY", "GENERATE HIDDEN SERVICE");
                            HiddenService.KeyMaterial hiddenServiceKeyMaterial = HiddenService.generateKeyMaterial();
                            Log.addEntry("DUMMY", "COMPLETED GENERATE HIDDEN SERVICE");
                            Log.addEntry("DUMMY", "RUN HIDDEN SERVICE");
                            TorWrapper tor = new TorWrapper(TorWrapper.Mode.MODE_RUN_SERVICES, hiddenServiceKeyMaterial, 8443);
                            tor.start();
                            Log.addEntry("DUMMY", "COMPLETED RUN HIDDEN SERVICE");
                            try {
                                Thread.sleep(30000);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            Log.addEntry("DUMMY", "STOP HIDDEN SERVICE");
                            tor.stop();
                            Log.addEntry("DUMMY", "STOP WEB SERVER");
                            webServer.stop();
                            Log.addEntry("DUMMY", "COMPLETED");                            
                        } catch (Utils.ApplicationError e) {
                            Log.addEntry("DUMMY", e.getMessage());
                        }
                    }
                },
                2000,
                300000);
	}
}
