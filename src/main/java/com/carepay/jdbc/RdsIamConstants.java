package com.carepay.jdbc;

public interface RdsIamConstants {
     String CA_BUNDLE_URL = "https://s3.amazonaws.com/rds-downloads/rds-combined-ca-bundle.pem";
    String REQUIRE_SSL = "requireSSL";
    String VERIFY_SERVER_CERTIFICATE = "verifyServerCertificate";
    String SSL_MODE = "sslMode";
    String VERIFY_CA = "VERIFY_CA";
    String TRUST_CERTIFICATE_KEY_STORE_URL = "trustCertificateKeyStoreUrl";
    String TRUST_CERTIFICATE_KEY_STORE_TYPE = "trustCertificateKeyStoreType";
    String PEM = "PEM";
}
