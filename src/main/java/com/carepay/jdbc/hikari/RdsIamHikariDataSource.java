package com.carepay.jdbc.hikari;

import java.net.URL;
import java.time.Clock;
import java.time.LocalDateTime;

import com.carepay.jdbc.RdsAWS4Signer;
import com.carepay.jdbc.pem.PemKeyStoreProvider;
import com.carepay.jdbc.util.JdbcUrlUtils;
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

    static {
        PemKeyStoreProvider.register();
    }

    private final RdsAWS4Signer signer;
    private final Clock clock;
    protected String host;
    protected int port;
    private LocalDateTime expiryDate;
    private String authToken;

    public RdsIamHikariDataSource() {
        this(new RdsAWS4Signer(), Clock.systemUTC());
    }

    public RdsIamHikariDataSource(final RdsAWS4Signer signer, final Clock clock) {
        this.signer = signer;
        this.clock = clock;
        addDataSourceProperty(USE_SSL, "true");     // for MySQL 5.x and before
        addDataSourceProperty(REQUIRE_SSL, "true"); // for MySQL 5.x and before
        addDataSourceProperty(VERIFY_SERVER_CERTIFICATE, "true");
        addDataSourceProperty(SSL_MODE, VERIFY_CA);       // for MySQL 8.x and higher
        addDataSourceProperty(TRUST_CERTIFICATE_KEY_STORE_URL, CA_BUNDLE_URL);
        addDataSourceProperty(TRUST_CERTIFICATE_KEY_STORE_TYPE, PEM);
    }

    protected void extractHostFromUrl() {
        final URL uri = JdbcUrlUtils.extractJdbcURL(getJdbcUrl());
        this.host = uri.getHost();
        this.port = uri.getPort();
    }

    /**
     * Gets the IAM token. Creates a new token when the token is expired.
     *
     * @return the IAM RDS token.
     */
    @Override
    public String getPassword() {
        refreshToken();
        return this.authToken;
    }

    private void refreshToken() {
        final LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), UTC);
        if (this.expiryDate == null || expiryDate.isBefore(now)) {
            if (host == null) {
                extractHostFromUrl();
            }
            this.authToken = signer.generateToken(host, port, getUsername());
            this.expiryDate = now.plusMinutes(10L); // Token expires after 15 min, so renew after 10 min
        }
    }
}

