package com.carepay.jdbc;

import java.net.URI;
import java.security.Security;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.carepay.jdbc.aws.AWS4RdsIamTokenGenerator;
import com.carepay.jdbc.aws.AWSCredentialsProvider;
import com.carepay.jdbc.aws.DefaultAWSCredentialsProviderChain;
import com.carepay.jdbc.pem.PemKeyStoreProvider;
import com.zaxxer.hikari.HikariDataSource;

/**
 * DataSource based on Hikari connection pool that supports IAM authentication to RDS
 */
public class RdsIamHikariDataSource extends HikariDataSource implements RdsIamConstants {

    public static final int DEFAULT_PORT = 3306;

    static Clock clock = Clock.systemDefaultZone();

    static {
        Security.addProvider(new PemKeyStoreProvider());
    }

    private String host;
    private int port;
    private LocalDateTime expiryDate;
    private String authToken;
    private AWS4RdsIamTokenGenerator rdsIamTokenGenerator;
    private AWSCredentialsProvider credentialsProvider;

    public RdsIamHikariDataSource() {
        this(new AWS4RdsIamTokenGenerator(), new DefaultAWSCredentialsProviderChain());
    }

    /**
     * RDS IAM authentication sends the token as a plaintext password. SSL must be enabled.
     */
    public RdsIamHikariDataSource(final AWS4RdsIamTokenGenerator rdsIamTokenGenerator, AWSCredentialsProvider credentialsProvider) {
        this.rdsIamTokenGenerator = rdsIamTokenGenerator;
        this.credentialsProvider = credentialsProvider;
        addDataSourceProperty(REQUIRE_SSL, "true"); // for MySQL 5.x and before
        addDataSourceProperty(VERIFY_SERVER_CERTIFICATE, "true");
        addDataSourceProperty(SSL_MODE, VERIFY_CA); // for MySQL 8.x and higher
        addDataSourceProperty(TRUST_CERTIFICATE_KEY_STORE_URL, CA_BUNDLE_URL);
        addDataSourceProperty(TRUST_CERTIFICATE_KEY_STORE_TYPE, PEM);
    }

    /**
     * Gets the IAM token. Creates a new token when the token is expired.
     *
     * @return the IAM RDS token
     */
    @Override
    public String getPassword() {
        if (host == null) {
            final URI uri = URI.create(this.getJdbcUrl().substring(5)); // jdbc:
            host = uri.getHost();
            port = uri.getPort() > 0 ? uri.getPort() : DEFAULT_PORT;
        }
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneId.of("UTC"));
        if (this.expiryDate == null || expiryDate.isBefore(now)) {
            credentialsProvider = new DefaultAWSCredentialsProviderChain();
            this.authToken = rdsIamTokenGenerator.createDbAuthToken(host, port, getUsername(), credentialsProvider.getCredentials());
            this.expiryDate = now.plusMinutes(10); // Token expires after 15 min, so renew after 10 min
        }
        return this.authToken;
    }
}

