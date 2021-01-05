package com.carepay.jdbc.pem;

import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStoreSpi;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractKeyStore extends KeyStoreSpi {
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
        return getEntry(alias).map(Entry::getCertificate).map(this::getCreationDate).orElse(null);
    }

    protected abstract Date getCreationDate(Certificate certificate);

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

}
