package com.carepay.jdbc;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDateTime;
import javax.annotation.PostConstruct;

import com.carepay.aws.auth.AWS4Signer;
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
import static java.time.ZoneOffset.UTC;

/**
 * DataSource based on Hikari connection pool that supports IAM authentication to RDS
 */
public class RdsIamHikariDataSource extends HikariDataSource {

    private static final int DEFAULT_PORT = 3306;

    private final AWS4Signer signer;
    private final Clock clock;
    private String host;
    private int port;
    private LocalDateTime expiryDate;
    private String authToken;
    private boolean managed;

    public RdsIamHikariDataSource() {
        this(new AWS4Signer(), Clock.systemUTC());
    }

    public RdsIamHikariDataSource(final AWS4Signer signer, final Clock clock) {
        this.signer = signer;
        this.clock = clock;
        RdsIamInitializer.init();
    }

    /**
     * RDS IAM authentication sends the token as a plaintext password. SSL must be enabled.
     */
    @PostConstruct
    public void managedStart() {
        managed = true;
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
     * @return the IAM RDS token.
     */
    @Override
    public String getPassword() {
        if (!managed) {
            return super.getPassword();
        }
        if (host == null) {
            final URI uri = URI.create(this.getJdbcUrl().substring(5)); // jdbc:
            host = uri.getHost();
            port = uri.getPort() > 0 ? uri.getPort() : DEFAULT_PORT;
        }
        final LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), UTC);
        if (this.expiryDate == null || expiryDate.isBefore(now)) {
            this.authToken = signer.createDbAuthToken(host, port, getUsername());
            this.expiryDate = now.plusMinutes(10L); // Token expires after 15 min, so renew after 10 min
        }
        return this.authToken;
    }
}

