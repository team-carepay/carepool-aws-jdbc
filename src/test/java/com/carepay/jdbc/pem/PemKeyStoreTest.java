package com.carepay.jdbc.pem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Enumeration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PemKeyStoreTest {
    private static final String PEM_KEY = "pem66";
    private PemKeyStore pemKeyStore;

    @BeforeEach
    void setUp() throws IOException, CertificateException {
        pemKeyStore = new PemKeyStore();
        pemKeyStore.engineLoad(getClass().getResourceAsStream("/rds-combined-ca-bundle.pem"), null);
    }

    @Test
    void testBundleLoaded() {
        assertThat(pemKeyStore.entries).isNotEmpty();
    }

    @Test
    void testEngineGetKey() {
        Key key = pemKeyStore.engineGetKey("pem", null);
        assertThat(key).isNull();
    }

    @Test
    void testEngineGetCertificateChain() {
        Certificate[] certs = pemKeyStore.engineGetCertificateChain("pem66");
        assertThat(certs).hasSize(1);
    }

    @Test
    void testEngineGetCertificate() {
        Certificate cert = pemKeyStore.engineGetCertificate("pem66");
        assertThat(cert).isNotNull();
        assertThat(pemKeyStore.engineGetCertificateAlias(cert)).isEqualTo("pem66");
        assertThat(pemKeyStore.engineGetCertificateAlias(null)).isNull();
    }

    @Test
    void testEngineIsKeyEntry() {
        assertThat(pemKeyStore.engineIsKeyEntry("pem66")).isFalse();
    }

    @Test
    void testGetAliases() {
        Enumeration<String> aliases = pemKeyStore.engineAliases();
        assertThat(aliases.hasMoreElements()).isTrue();
        assertThat(aliases.nextElement()).isEqualTo("pem67");
    }

    @Test
    void testEngineIsCertificateEntry() {
        assertThat(pemKeyStore.engineIsCertificateEntry("pem66")).isTrue();
    }

    @Test
    void testEngineSize() {
        assertThat(pemKeyStore.engineSize()).isGreaterThan(1);
    }

    @Test
    void testLastDate() {
        assertThat(pemKeyStore.engineGetCreationDate("pem66")).isEqualToIgnoringHours("2015-02-05");
        assertThat(pemKeyStore.engineGetCreationDate("other")).isNull();
    }

    @Test
    void testEngineContainsAlias() {
        assertThat(pemKeyStore.engineContainsAlias("pem66")).isTrue();
        assertThat(pemKeyStore.engineContainsAlias("other")).isFalse();
    }

    @Test
    void testEngineSetKeyEntryWithKey() {
        assertThatThrownBy(() -> pemKeyStore.engineSetKeyEntry("pem", null, null, null))
                .isInstanceOf(UnsupportedOperationException.class);
    }


    @Test
    void testEngineSetKeyEntryWithByteArray() {
        assertThatThrownBy(() -> pemKeyStore.engineSetKeyEntry("pem66", null, null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testEngineSetCertificate() {
        assertThatThrownBy(() -> pemKeyStore.engineSetCertificateEntry(PEM_KEY, null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testEngineDeleteEntry() {
        assertThatThrownBy(() -> pemKeyStore.engineDeleteEntry("other"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testEngineStore() {
        assertThatThrownBy(() -> pemKeyStore.engineStore(new ByteArrayOutputStream(), null))
                .isInstanceOf(UnsupportedOperationException.class);
    }


}
