package com.carepay.jdbc;

import java.net.MalformedURLException;
import java.net.URL;

import com.carepay.jdbc.util.DBHttpURLConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DBHttpURLConnectionTest {

    private TestDBHttpURLConnection urlConnection;

    @BeforeEach
    public void setUp() throws MalformedURLException {
        this.urlConnection = new TestDBHttpURLConnection(new URL("http://nonexistinghost.nonexistingdomain31415926535.tld/test"));
    }

    @Test
    public void connectAndDisconnect() {
        assertThat(urlConnection.isConnected()).isFalse();
        urlConnection.connect();
        assertThat(urlConnection.isConnected()).isFalse();
        urlConnection.disconnect();
        assertThat(urlConnection.isConnected()).isFalse();
    }

    @Test
    public void usingProxy() {
        assertThat(urlConnection.usingProxy()).isFalse();
    }

    static class TestDBHttpURLConnection extends DBHttpURLConnection {
        public TestDBHttpURLConnection(URL u) {
            super(u);
        }

        boolean isConnected() {
            return connected;
        }
    }
}