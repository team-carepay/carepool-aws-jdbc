package com.carepay.jdbc.pem;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStoreSpi;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * KeyStore, which supports reading .PEM files. Only loads entries once to improve performance.
 */
public class PemKeyStore extends KeyStoreSpi {
    private static final String UNSUPPORTED_OPERATION = "Unsupported operation";
    protected final ConcurrentMap<String, Entry> entries = new ConcurrentHashMap<>();

    protected Optional<Entry> getEntry(final String alias) {
        return Optional.ofNullable(entries.get(alias));
    }

    @Override
    public Key engineGetKey(final String alias, final char[] password) {
        return getEntry(alias).map(Entry::getKey).orElse(null);
    }

    @Override
    public boolean engineIsKeyEntry(final String alias) {
        return getEntry(alias).map(Entry::isKey).orElse(false);
    }

    @Override
    public Certificate[] engineGetCertificateChain(final String alias) {
        return getEntry(alias).map(Entry::getCertificateChain).map(l -> l.toArray(new Certificate[]{})).orElse(null);
    }

    @Override
    public Certificate engineGetCertificate(final String alias) {
        return getEntry(alias).map(Entry::getCertificate).orElse(null);
    }

    @Override
    public Date engineGetCreationDate(final String alias) {
        return getEntry(alias)
                .map(Entry::getCertificate)
                .filter(cert -> cert instanceof X509Certificate)
                .map(X509Certificate.class::cast)
                .map(X509Certificate::getNotBefore)
                .orElse(null);
    }

    @Override
    public Enumeration<String> engineAliases() {
        return Collections.enumeration(entries.keySet());
    }

    @Override
    public boolean engineContainsAlias(final String alias) {
        return entries.containsKey(alias);
    }

    @Override
    public int engineSize() {
        return entries.size();
    }

    @Override
    public boolean engineIsCertificateEntry(final String alias) {
        return getEntry(alias).map(Entry::isCertificate).orElse(false);
    }

    @Override
    public String engineGetCertificateAlias(final Certificate cert) {
        for (final Map.Entry<String, Entry> entry : entries.entrySet()) {
            if (entry.getValue() != null && Objects.equals(cert, entry.getValue().getCertificate())) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public void engineLoad(final InputStream stream, final char[] password) throws CertificateException {
        if (stream != null && entries.isEmpty()) {
            for (final Certificate c : CertificateFactory.getInstance("X.509").generateCertificates(stream)) {
                entries.put("pem" + ((X509Certificate) c).getSerialNumber(), new Entry(null, Collections.singletonList(c)));
            }
        }
    }

    @Override
    public void engineSetKeyEntry(final String alias, final Key key, final char[] password, final Certificate[] chain) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    @Override
    public void engineSetKeyEntry(final String alias, final byte[] key, final Certificate[] chain) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    @Override
    public void engineSetCertificateEntry(final String alias, final Certificate cert) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    @Override
    public void engineDeleteEntry(final String alias) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    @Override
    public void engineStore(final OutputStream stream, final char[] password) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    private static final class Entry {

        private final Key key;
        private final List<Certificate> certificateChain;

        public Entry(final Key key, final List<Certificate> certificateChain) {
            this.key = key;
            this.certificateChain = certificateChain;
        }

        public Key getKey() {
            return this.key;
        }

        public boolean isKey() {
            return this.key != null;
        }

        public List<Certificate> getCertificateChain() {
            return this.certificateChain;
        }

        public Certificate getCertificate() {
            return certificateChain.isEmpty() ? null : certificateChain.get(0);
        }

        public boolean isCertificate() {
            return !this.certificateChain.isEmpty();
        }
    }
}
