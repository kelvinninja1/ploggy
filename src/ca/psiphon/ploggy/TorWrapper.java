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

public class TorWrapper {

    public static final String SOCKS_PROXY_HOSTNAME = "127.0.0.1";
    
	private HiddenService.KeyMaterial mHiddenServiceIdentity;
	private int mWebServerPort;
	private int mSocksProxyPort; // TODO: where selected?
	
	public TorWrapper(HiddenService.KeyMaterial hiddenServiceIdentity, int webServerPort) {
		mHiddenServiceIdentity = hiddenServiceIdentity;
		mWebServerPort = webServerPort;
	}
	
	public void start() {
		
	}
	
	public void stop() {
		
	}
	
	public int getSocksProxyPort() {
	    return mSocksProxyPort;
	}
}
