package com.carepay.jdbc;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Wrapper class for HttpURLConnection, used for RDS-DB signing requests.
 */
class DBHttpURLConnection extends HttpURLConnection {

    public DBHttpURLConnection(final URL u) {
        super(u);
    }

    @Override
    public void disconnect() {
        // not needed for DB wrapper
    }

    @Override
    public boolean usingProxy() {
        return false;
    }

    @Override
    public void connect() {
        // not needed for DB wrapper
    }
}
