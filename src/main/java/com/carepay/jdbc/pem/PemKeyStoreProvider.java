package com.carepay.jdbc.pem;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Provider;
import java.security.Security;
import java.util.Optional;

import static com.carepay.jdbc.RdsIamConstants.CA_BUNDLE_PATH;
import static com.carepay.jdbc.RdsIamConstants.CLASSPATH_PROTOCOL;

/**
 * Adds support for PEM based keystore.
 */
public class PemKeyStoreProvider extends Provider {
    private static final long serialVersionUID = 1L;

    public PemKeyStoreProvider() {
        super("PEM", 1, "Provides PEM based KeyStore impl"); // NOSONAR
        setup();
    }

    private void setup() {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> { //NOSONAR
            performSetup();
            return null;
        });
    }

    private void performSetup() {
        put("KeyStore.PEM", PemKeyStore.class.getName());
    }

    private static void ensureClasspathURLSupported() {
        final String caBundleUrl = CLASSPATH_PROTOCOL + ":" + CA_BUNDLE_PATH;
        try {
            new URL(caBundleUrl);
        } catch (MalformedURLException e) {
            registerStreamHandler();
        }
    }

    private static void registerStreamHandler() {
        try {
            URL.setURLStreamHandlerFactory(p -> CLASSPATH_PROTOCOL.equals(p) ? new ClasspathURLStreamHandler() : null);
        } catch (Throwable t) {
            //NOSONAR ignore
        }
    }

    private static class ClasspathURLStreamHandler extends URLStreamHandler {
        @Override
        protected URLConnection openConnection(URL url) throws IOException {
            return getUrl(url).openConnection();
        }

        private URL getUrl(URL url) throws IOException {
            return Optional.ofNullable(getClass().getResource(url.getPath())).orElseThrow(() -> new IOException("Resource not found: " + url));
        }
    }

    public static void register() {
        if (Security.addProvider(new PemKeyStoreProvider()) < 0) {
            return; // return when already installed
        }
        ensureClasspathURLSupported();
    }
}
