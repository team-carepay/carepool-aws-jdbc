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

import static com.carepay.jdbc.RdsIamConstants.CA_BUNDLE_URL;
import static com.carepay.jdbc.RdsIamConstants.PEM;
import static com.carepay.jdbc.RdsIamConstants.REQUIRE_SSL;
import static com.carepay.jdbc.RdsIamConstants.SSL_MODE;
import static com.carepay.jdbc.RdsIamConstants.TRUST_CERTIFICATE_KEY_STORE_TYPE;
import static com.carepay.jdbc.RdsIamConstants.TRUST_CERTIFICATE_KEY_STORE_URL;
import static com.carepay.jdbc.RdsIamConstants.USE_SSL;
import static com.carepay.jdbc.RdsIamConstants.VERIFY_CA;
import static com.carepay.jdbc.RdsIamConstants.VERIFY_SERVER_CERTIFICATE;

/**
 * DataSource based on Hikari connection pool that supports IAM authentication to RDS
 */
public class RdsIamHikariDataSource extends HikariDataSource {

    private static final int DEFAULT_PORT = 3306;
    private static final ZoneId UTC = ZoneId.of("UTC");

    /**
     * Registers the PEM keystore provider
     */
    static {
        Security.addProvider(new PemKeyStoreProvider());
    }

    private String host;
    private int port;
    private LocalDateTime expiryDate;
    private String authToken;
    private final AWS4RdsIamTokenGenerator rdsIamTokenGenerator;
    private final AWSCredentialsProvider credentialsProvider;
    private final Clock clock;

    /**
     * Default constructor, uses the default AWS provider chain.
     */
    public RdsIamHikariDataSource() {
        this(new AWS4RdsIamTokenGenerator(), new DefaultAWSCredentialsProviderChain(), Clock.systemUTC());
    }

    /**
     * RDS IAM authentication sends the token as a plaintext password. SSL must be enabled.
     */
    public RdsIamHikariDataSource(final AWS4RdsIamTokenGenerator rdsIamTokenGenerator, AWSCredentialsProvider credentialsProvider, Clock clock) {
        this.rdsIamTokenGenerator = rdsIamTokenGenerator;
        this.credentialsProvider = credentialsProvider;
        this.clock = clock;
        addDataSourceProperty(USE_SSL, "true");     // for MySQL 5.x and before
        addDataSourceProperty(REQUIRE_SSL, "true"); // for MySQL 5.x and before
        addDataSourceProperty(VERIFY_SERVER_CERTIFICATE, "true");
        addDataSourceProperty(SSL_MODE, VERIFY_CA);       // for MySQL 8.x and higher
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
        LocalDateTime now = getCurrentDateTime();
        if (this.expiryDate == null || expiryDate.isBefore(now)) {
            this.authToken = rdsIamTokenGenerator.createDbAuthToken(host, port, getUsername(), credentialsProvider.getCredentials());
            this.expiryDate = now.plusMinutes(10); // Token expires after 15 min, so renew after 10 min
        }
        return this.authToken;
    }

    protected LocalDateTime getCurrentDateTime() {
        return LocalDateTime.ofInstant(clock.instant(), UTC);
    }
}

