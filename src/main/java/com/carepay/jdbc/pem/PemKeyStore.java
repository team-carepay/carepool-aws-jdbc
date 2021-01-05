package com.carepay.jdbc.pem;

import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * KeyStore, which supports reading .PEM files. Only loads entries once to improve performance.
 */
public class PemKeyStore extends AbstractKeyStore {

    @Override
    public void engineLoad(final InputStream stream, final char[] password) throws CertificateException {
        if (stream != null && entries.isEmpty()) {
            loadCertificates(stream);
        }
    }

    private void loadCertificates(final InputStream stream) throws CertificateException {
        for (final Certificate c : getCertificates(stream)) {
            putEntry(c);
        }
    }

    private Collection<? extends Certificate> getCertificates(final InputStream stream) throws CertificateException {
        return CertificateFactory.getInstance("X.509").generateCertificates(stream);
    }

    private void putEntry(final Certificate c) {
        entries.put("pem" + ((X509Certificate) c).getSerialNumber(), new Entry(null, Collections.singletonList(c)));
    }

    @Override
    protected Date getCreationDate(final Certificate certificate) {
        return certificate instanceof X509Certificate ? ((X509Certificate) certificate).getNotBefore() : null;
    }

    @Override
    public String engineGetCertificateAlias(final Certificate cert) {
        return entries.entrySet().stream()
                .filter(entryPredicate(cert))
                .findFirst()
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private Predicate<Map.Entry<String, Entry>> entryPredicate(final Certificate cert) {
        return e -> e.getValue() != null && Objects.equals(cert, e.getValue().getCertificate());
    }
}
