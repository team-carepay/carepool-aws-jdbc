package com.carepay.jdbc.pem;

import java.io.IOException;
import java.security.cert.CertificateException;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PemKeyStoreTest {
    private PemKeyStore pemKeyStore;

    @Before
    public void setUp() throws IOException, CertificateException {
        pemKeyStore = new PemKeyStore();
        pemKeyStore.engineLoad(getClass().getResourceAsStream("/rds-combined-ca-bundle.pem"), null);
    }

    @Test
    public void testBundleLoaded() {
        assertThat(pemKeyStore.entries).isNotEmpty();
    }
}
