package com.carepay.jdbc;

public enum RdsIamConstants {;
    public static final String CLASSPATH_PROTOCOL = "classpath";
    public static final String CA_BUNDLE_PATH = "/rds-combined-ca-bundle.pem";
    public static final String CA_BUNDLE_URL = CLASSPATH_PROTOCOL + ":" + CA_BUNDLE_PATH;
    public static final String USE_SSL = "useSSL";
    public static final String REQUIRE_SSL = "requireSSL";
    public static final String VERIFY_SERVER_CERTIFICATE = "verifyServerCertificate";
    public static final String SSL_MODE = "sslMode";
    public static final String VERIFY_CA = "VERIFY_CA";
    public static final String TRUST_CERTIFICATE_KEY_STORE_URL = "trustCertificateKeyStoreUrl";
    public static final String TRUST_CERTIFICATE_KEY_STORE_TYPE = "trustCertificateKeyStoreType";
    public static final String PEM = "PEM";
}
