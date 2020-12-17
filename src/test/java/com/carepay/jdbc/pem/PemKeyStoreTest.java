package com.carepay.jdbc.pem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Enumeration;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PemKeyStoreTest {
    private static final String PEM_KEY = "pem66";
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

    @Test
    public void testEngineGetKey() {
        Key key = pemKeyStore.engineGetKey("pem", null);
        assertThat(key).isNull();
    }

    @Test
    public void testEngineGetCertificateChain() {
        Certificate[] certs = pemKeyStore.engineGetCertificateChain("pem66");
        assertThat(certs).hasSize(1);
    }

    @Test
    public void testEngineGetCertificate() {
        Certificate cert = pemKeyStore.engineGetCertificate("pem66");
        assertThat(cert).isNotNull();
        assertThat(pemKeyStore.engineGetCertificateAlias(cert)).isEqualTo("pem66");
        assertThat(pemKeyStore.engineGetCertificateAlias(null)).isNull();
    }

    @Test
    public void testEngineIsKeyEntry() {
        assertThat(pemKeyStore.engineIsKeyEntry("pem66")).isFalse();
    }

    @Test
    public void testGetAliases() {
        Enumeration<String> aliases = pemKeyStore.engineAliases();
        assertThat(aliases.hasMoreElements()).isTrue();
        assertThat(aliases.nextElement()).isEqualTo("pem67");
    }

    @Test
    public void testEngineIsCertificateEntry() {
        assertThat(pemKeyStore.engineIsCertificateEntry("pem66")).isTrue();
    }

    @Test
    public void testEngineSize() {
        assertThat(pemKeyStore.engineSize()).isGreaterThan(1);
    }

    @Test
    public void testLastDate() {
        assertThat(pemKeyStore.engineGetCreationDate("pem66")).isEqualToIgnoringHours("2015-02-05");
        assertThat(pemKeyStore.engineGetCreationDate("other")).isNull();
    }

    @Test
    public void testEngineContainsAlias() {
        assertThat(pemKeyStore.engineContainsAlias("pem66")).isTrue();
        assertThat(pemKeyStore.engineContainsAlias("other")).isFalse();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testEngineSetKeyEntryWithKey() {
        pemKeyStore.engineSetKeyEntry("pem", null, null, null);
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testEngineSetKeyEntryWithByteArray() {
        pemKeyStore.engineSetKeyEntry("pem66", null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testEngineSetCertificate() {
        pemKeyStore.engineSetCertificateEntry(PEM_KEY, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testEngineDeleteEntry() {
        pemKeyStore.engineDeleteEntry("other");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testEngineStore() {
        pemKeyStore.engineStore(new ByteArrayOutputStream(), null);
    }


}
