package com.carepay.jdbc.pem;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStoreSpi;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * KeyStore which supports reading .PEM files. Only loads entries once to improve performance.
 */
public class PemKeyStore extends KeyStoreSpi {
    protected static Map<String, Entry> entries = new HashMap<>();

    protected Optional<Entry> getEntry(final String alias) {
        return Optional.ofNullable(this.entries.get(alias));
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
                .map(cert -> cert instanceof X509Certificate ? (X509Certificate) cert : null)
                .map(X509Certificate::getNotBefore)
                .orElse(null);
    }

    @Override
    public Enumeration<String> engineAliases() {
        final Iterator<String> keys = this.entries.keySet().iterator();
        return new Enumeration<String>() {
            @Override
            public String nextElement() {
                return keys.next();
            }

            @Override
            public boolean hasMoreElements() {
                return keys.hasNext();
            }
        };
    }

    @Override
    public boolean engineContainsAlias(final String alias) {
        return this.entries.containsKey(alias);
    }

    @Override
    public int engineSize() {
        return this.entries.size();
    }

    @Override
    public boolean engineIsCertificateEntry(final String alias) {
        return getEntry(alias).map(Entry::isCertificate).orElse(false);
    }

    @Override
    public String engineGetCertificateAlias(final Certificate cert) {
        for (final Map.Entry<String, Entry> entry : this.entries.entrySet()) {
            if (cert == entry.getValue().getCertificate()) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public void engineLoad(final InputStream stream, final char[] password)
            throws CertificateException {
        if (stream != null && entries.isEmpty()) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            entries.put("pem", new Entry(null, new ArrayList<>(cf.generateCertificates(stream))));
        }
    }

    @Override
    public void engineSetKeyEntry(String alias, Key key, char[] password, Certificate[] chain) {
        throw new UnsupportedOperationException("Unsupported operation");
    }

    @Override
    public void engineSetKeyEntry(String alias, byte[] key, Certificate[] chain) {
        throw new UnsupportedOperationException("Unsupported operation");
    }

    @Override
    public void engineSetCertificateEntry(String alias, Certificate cert) {
        throw new UnsupportedOperationException("Unsupported operation");
    }

    @Override
    public void engineDeleteEntry(String alias) {
        throw new UnsupportedOperationException("Unsupported operation");
    }

    @Override
    public void engineStore(OutputStream stream, char[] password) {
        throw new UnsupportedOperationException("Unsupported operation");
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
