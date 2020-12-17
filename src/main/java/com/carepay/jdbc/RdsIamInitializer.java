package com.carepay.jdbc;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.Security;

import com.carepay.jdbc.pem.PemKeyStoreProvider;

import static com.carepay.jdbc.RdsIamConstants.CA_BUNDLE_PATH;
import static com.carepay.jdbc.RdsIamConstants.CLASSPATH_PROTOCOL;

/**
 * Initializes the PEM keystore provider. In case the 'classpath:' prefix is not supported, a custom
 * URLStreamHandler is registered.
 */
public interface RdsIamInitializer {

    static void init() {
        init(CLASSPATH_PROTOCOL, CA_BUNDLE_PATH);
    }

    static void init(final String protocol, final String caBundlePath) {
        if (Security.addProvider(new PemKeyStoreProvider()) < 0) {
            return; // return when already installed
        }
        final String caBundleUrl = protocol + ":" + caBundlePath;
        try {
            new URL(caBundleUrl);
        } catch (MalformedURLException e) {
            URL.setURLStreamHandlerFactory(p -> protocol.equals(p) ? new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL url) throws IOException {
                    final URL classpathUrl = getClass().getResource(url.getPath());
                    if (classpathUrl == null) {
                        throw new IOException("Resource not found: " + url);
                    }
                    return classpathUrl.openConnection();
                }
            } : null);
        }
    }
}
