package com.carepay.jdbc.pem;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * KeyStore which supports reading .PEM files.
 */
public class PemKeyStore extends KeyStoreSpi {
    private static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----"; // TODO: replace with regex
    private static final String END_CERT = "-----END CERTIFICATE-----";
    private static final Pattern COMMENT_OR_EMPTY = Pattern.compile("^\\s*(#|$)");
    protected Map<String, Entry> entries = new HashMap<>();

    protected Optional<Entry> getEntry(final String alias) {
        return Optional.of(this.entries.get(alias));
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
        return (Certificate[]) getEntry(alias).map(Entry::getCertificateChain).map(List::toArray).orElse(null);
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
            throws IOException, CertificateException {
        if (stream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                List<Certificate> certificateChain = new ArrayList<>();
                byte[] bytes;
                while ((bytes = readCert(reader)) != null) {
                    certificateChain.addAll(cf.generateCertificates(new ByteArrayInputStream(bytes)));
                }
                entries.put("pem",new Entry(null, certificateChain));
            }
        }
    }

    private byte[] readCert(BufferedReader reader) throws IOException {
        final StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null && !BEGIN_CERT.equals(line)) {
            if (!COMMENT_OR_EMPTY.matcher(line).matches()) {
                throw new IllegalArgumentException(line);
            }
        }
        if (line == null) {
            return null;
        }
        while ((line = reader.readLine()) != null && !END_CERT.equals(line)) {
            if (!COMMENT_OR_EMPTY.matcher(line).matches()) {
                sb.append(line);
            }
        }
        if (line == null) {
            throw new IllegalStateException("END CERTIFICATE not found");
        }
        return Base64.getDecoder().decode(sb.toString());
    }

    @Override
    public void engineSetKeyEntry(String alias, Key key, char[] password, Certificate[] chain) throws KeyStoreException {
        throw new KeyStoreException("Unsupported operation");
    }

    @Override
    public void engineSetKeyEntry(String alias, byte[] key, Certificate[] chain) throws KeyStoreException {
        throw new KeyStoreException("Unsupported operation");
    }

    @Override
    public void engineSetCertificateEntry(String alias, Certificate cert) throws KeyStoreException {
        throw new KeyStoreException("Unsupported operation");
    }

    @Override
    public void engineDeleteEntry(String alias) throws KeyStoreException {
        throw new KeyStoreException("Unsupported operation");
    }

    @Override
    public void engineStore(OutputStream stream, char[] password) throws IOException {
        throw new IOException("Unsupported operation");
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
