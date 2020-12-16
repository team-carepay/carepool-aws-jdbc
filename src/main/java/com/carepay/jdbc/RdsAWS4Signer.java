package com.carepay.jdbc;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Clock;

import com.carepay.aws.auth.AWS4Signer;
import com.carepay.aws.auth.CredentialsProvider;
import com.carepay.aws.auth.RegionProvider;

public class RdsAWS4Signer extends AWS4Signer {
    public RdsAWS4Signer() {
        super("rds-db");
    }

    public RdsAWS4Signer(CredentialsProvider credentialsProvider, RegionProvider regionProvider, Clock clock) {
        super("rds-db", credentialsProvider, regionProvider, clock);
    }

    /**
     * @param host     database hostname (dbname.xxxx.eu-west-1.rds.amazonaws.com)
     * @param port     database port (MySQL uses 3306)
     * @param username database username
     * @return the DB token
     */
    public String generateToken(final String host, final int port, final String username) {
        return new SignRequest(new DBHttpURLConnection(createURL("https", host, port, "/?Action=connect&DBUser=" + username))).signQuery();
    }

    static URL createURL(final String protocol, final String host, final int port, final String file) {
        try {
            return new URL(protocol, host, port, file);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Wrapper class for HttpURLConnection, used for RDS-DB signing requests.
     */
    static class DBHttpURLConnection extends HttpURLConnection {

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
}
