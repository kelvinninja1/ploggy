/*
 * Copyright (c) 2014, Psiphon Inc.
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

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Date;

import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

/**
 * Helpers for reporting bytes sent to and received from peers.
 */
public class DataTransferStats {

    private static final String LOG_TAG = "Data Transfer Stats";

    private static final int REPORT_FREQUENCY_IN_MILLISECONDS = 1000; // 1 sec.

    public static class SSLSocketWrapper extends SSLSocket {
        private final Data mData;
        private final SSLSocket mSocket;

        public SSLSocketWrapper(Data data, SSLSocket socket) {
            mData = data;
            mSocket = socket;
        }

        private String getFriendId() {
            try {
                String certificate = TransportSecurity.getPeerCertificate(mSocket);
                Data.Friend friend = mData.getFriendByCertificateOrThrow(certificate);
                return friend.mId;
            } catch (PloggyError e) {
                Log.addEntry(LOG_TAG, "no friend for certificate");
            }
            return null;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            String friendId = getFriendId();
            if (friendId == null) {
                return mSocket.getInputStream();
            }
            return new InputStreamWrapper(mData, getFriendId(), mSocket.getInputStream());
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            String friendId = getFriendId();
            if (friendId == null) {
                return mSocket.getOutputStream();
            }
            return new OutputStreamWrapper(mData, getFriendId(), mSocket.getOutputStream());
        }

        // Required overrides -- just pass through
        @Override public void addHandshakeCompletedListener(HandshakeCompletedListener listener) { mSocket.addHandshakeCompletedListener(listener); }
        @Override public boolean getEnableSessionCreation() { return mSocket.getEnableSessionCreation(); }
        @Override public String[] getEnabledCipherSuites() { return mSocket.getEnabledCipherSuites(); }
        @Override public String[] getEnabledProtocols() { return mSocket.getEnabledProtocols(); }
        @Override public boolean getNeedClientAuth() { return mSocket.getNeedClientAuth(); }
        @Override public SSLSession getSession() { return mSocket.getSession(); }
        @Override public String[] getSupportedCipherSuites() { return mSocket.getSupportedCipherSuites(); }
        @Override public String[] getSupportedProtocols() { return mSocket.getSupportedProtocols(); }
        @Override public boolean getUseClientMode() { return mSocket.getUseClientMode(); }
        @Override public boolean getWantClientAuth() { return mSocket.getWantClientAuth(); }
        @Override public void removeHandshakeCompletedListener( HandshakeCompletedListener listener) { mSocket.removeHandshakeCompletedListener(listener); }
        @Override public void setEnableSessionCreation(boolean flag) { mSocket.setEnableSessionCreation(flag); }
        @Override public void setEnabledCipherSuites(String[] suites) { mSocket.setEnabledCipherSuites(suites); }
        @Override public void setEnabledProtocols(String[] protocols) { mSocket.setEnabledProtocols(protocols); }
        @Override public void setNeedClientAuth(boolean need) { mSocket.setNeedClientAuth(need); }
        @Override public void setUseClientMode(boolean mode) { mSocket.setUseClientMode(mode); }
        @Override public void setWantClientAuth(boolean want) { mSocket.setWantClientAuth(want); }
        @Override public void startHandshake() throws IOException { mSocket.startHandshake(); }
    }

    public static class SSLServerSocketWrapper extends SSLServerSocket {
        private final Data mData;
        private final SSLServerSocket mServerSocket;

        public SSLServerSocketWrapper(Data data, SSLServerSocket serverSocket) throws IOException {
            mData = data;
            mServerSocket = serverSocket;
        }

        @Override
        public Socket accept() throws IOException {
            SSLSocket socket = (SSLSocket) super.accept();
            if (mData == null) {
                return socket;
            }
            return new SSLSocketWrapper(mData, socket);
        }

        // Required overrides -- just pass through
        @Override public boolean getEnableSessionCreation() { return mServerSocket.getEnableSessionCreation(); }
        @Override public String[] getEnabledCipherSuites() { return mServerSocket.getEnabledCipherSuites(); }
        @Override public String[] getEnabledProtocols() { return mServerSocket.getEnabledProtocols(); }
        @Override public boolean getNeedClientAuth() { return mServerSocket.getNeedClientAuth(); }
        @Override public String[] getSupportedCipherSuites() { return mServerSocket.getSupportedCipherSuites(); }
        @Override public String[] getSupportedProtocols() { return mServerSocket.getSupportedProtocols(); }
        @Override public boolean getUseClientMode() { return mServerSocket.getUseClientMode(); }
        @Override public boolean getWantClientAuth() { return mServerSocket.getWantClientAuth(); }
        @Override public void setEnableSessionCreation(boolean flag) { mServerSocket.setEnableSessionCreation(flag); }
        @Override public void setEnabledCipherSuites(String[] suites) { mServerSocket.setEnabledCipherSuites(suites); }
        @Override public void setEnabledProtocols(String[] protocols) { mServerSocket.setEnabledProtocols(protocols); }
        @Override public void setNeedClientAuth(boolean need) { mServerSocket.setNeedClientAuth(need); }
        @Override public void setUseClientMode(boolean mode) { mServerSocket.setUseClientMode(mode); }
        @Override public void setWantClientAuth(boolean want) { mServerSocket.setWantClientAuth(want); }
    }

    public static class InputStreamWrapper extends FilterInputStream {
        private final Data mData;
        private final String mFriendId;
        private Date mLastReport;
        private long mByteCount;

        public InputStreamWrapper(Data data, String friendId, InputStream inputStream) {
            super(inputStream);
            mData = data;
            mFriendId = friendId;
            mLastReport = null;
            mByteCount = 0;
        }

        @Override
        public void close() throws IOException {
            updateByteCount(0, true);
        }

        @Override
        public int read() throws IOException {
            int result = super.read();
            updateByteCount(1, false);
            return result;
        }

        @Override
        public int read(byte[] b) throws IOException {
            // *TODO* check: does this call the super class read(b,off,len) and double count?
            int result = super.read(b);
            updateByteCount(result, false);
            return result;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int result = super.read(b, off, len);
            updateByteCount(result, false);
            return result;
        }

        @Override
        public long skip(long n) throws IOException {
            long result = super.skip(n);
            updateByteCount(result, false);
            return result;
        }

        private void updateByteCount(long count, boolean forceReport) {
            mByteCount += count;
            Date now = new Date();
            if (mLastReport == null
                    || mLastReport.getTime() - now.getTime() < REPORT_FREQUENCY_IN_MILLISECONDS
                    || forceReport) {
                try {
                    // Note: at this point, we only actually know that the bytes have been
                    // accepted into the system network buffers. This is one reason why
                    // this stat is only informational.
                    mData.updateFriendReceivedOrThrow(mFriendId, now, mByteCount);
                    mByteCount = 0;
                } catch (PloggyError e) {
                    Log.addEntry(LOG_TAG, "failed to update bytes received");
                }
                // Update this even when the data update failed, to avoid retrying too frequently
                mLastReport = now;
            }
        }
    }

    public static class OutputStreamWrapper extends FilterOutputStream {
        private final Data mData;
        private final String mFriendId;
        private Date mLastReport;
        private long mByteCount;

        public OutputStreamWrapper(Data data, String friendId, OutputStream outputStream) {
            super(outputStream);
            mData = data;
            mFriendId = friendId;
            mLastReport = null;
            mByteCount = 0;
        }

        @Override
        public void close() throws IOException {
            updateByteCount(0, true);
        }

        @Override
        public void write(int b) throws IOException {
            super.write(b);
            updateByteCount(1, false);
        }

        @Override
        public void write(byte[] b) throws IOException {
            super.write(b);
            updateByteCount(b.length, false);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            super.write(b, off, len);
            updateByteCount(len, false);
        }

        private void updateByteCount(long count, boolean forceReport) {
            mByteCount += count;
            Date now = new Date();
            if (mLastReport == null
                    || mLastReport.getTime() - now.getTime() < REPORT_FREQUENCY_IN_MILLISECONDS
                    || forceReport) {
                try {
                    // Note: at this point, we only actually know that the bytes have been
                    // accepted into the system network buffers. This is one reason why
                    // this stat is only informational.
                    mData.updateFriendSentOrThrow(mFriendId, now, mByteCount);
                    mByteCount = 0;
                } catch (PloggyError e) {
                    Log.addEntry(LOG_TAG, "failed to update bytes sent");
                }
                // Update this even when the data update failed, to avoid retrying too frequently
                mLastReport = now;
            }
        }

    }
}
