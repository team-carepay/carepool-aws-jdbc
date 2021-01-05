package com.carepay.jdbc.pem;

import java.security.Key;
import java.security.cert.Certificate;
import java.util.List;

public class Entry {
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
