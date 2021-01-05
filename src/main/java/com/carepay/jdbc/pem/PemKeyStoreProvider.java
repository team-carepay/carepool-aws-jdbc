package com.carepay.jdbc.pem;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Provider;

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
}
