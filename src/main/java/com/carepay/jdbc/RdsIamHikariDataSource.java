package com.carepay.jdbc;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.DefaultAwsRegionProviderChain;
import com.amazonaws.services.rds.auth.GetIamAuthTokenRequest;
import com.amazonaws.services.rds.auth.RdsIamAuthTokenGenerator;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang3.StringUtils;

/**
 * DataSource based on Hikari connection pool that supports IAM authentication to RDS
 */
public class RdsIamHikariDataSource extends HikariDataSource {

    public static final int DEFAULT_PORT = 3306;

    private static Clock clock = Clock.systemDefaultZone();
    private RdsIamAuthTokenGenerator rdsIamAuthTokenGenerator;
    private LocalDateTime expiryDate;
    private String authToken;
    private GetIamAuthTokenRequest rdsIamAuthTokenRequest;

    public RdsIamHikariDataSource() {
        addDataSourceProperty("requireSSL","true"); // for MySQL 5.x and before
        addDataSourceProperty("sslMode","REQUIRED"); // for MySQL 8.x and higher
        addDataSourceProperty("trustCertificateKeyStoreUrl","classpath:rds-combined-ca-bundle.pem");
        addDataSourceProperty("trustCertificateKeyStoreType", "PEM");
    }

    /**
     * Initializes the token generator. Uses the hostname to determine the region, otherwise uses
     * the default AWS region-provider.
     */
    protected void initTokenGenerator() {
        try {
            final URI uri = new URI(this.getJdbcUrl().substring(5)); // jdbc:
            final String host = Optional.ofNullable(uri.getHost()).orElse("localhost");
            final int port = uri.getPort() > 0 ? uri.getPort() : DEFAULT_PORT;
            final String region = host != null && host.endsWith(".rds.amazonaws.com") ? StringUtils.split(host, '.')[2] :  new DefaultAwsRegionProviderChain().getRegion();
            this.rdsIamAuthTokenGenerator = getRdsIamAuthTokenGenerator(region);
            this.rdsIamAuthTokenRequest = getIamAuthTokenRequest(host, port);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(),e);
        }
    }

    /**
     * Creates a new token-request
     *
     * @param host host-name of the RDS server
     * @param port port-number (defaults to 3306)
     * @return the request object
     */
    protected GetIamAuthTokenRequest getIamAuthTokenRequest(String host, int port) {
        return GetIamAuthTokenRequest.builder().hostname(host).port(port).userName(this.getUsername()).build();
    }

    /**
     * Creates a new token generator
     *
     * @param region the AWS region-name
     * @return the token generator
     */
    protected RdsIamAuthTokenGenerator getRdsIamAuthTokenGenerator(String region) {
        return RdsIamAuthTokenGenerator.builder().credentials(new DefaultAWSCredentialsProviderChain()).region(region).build();
    }

    /**
     * Gets the IAM token. Creates a new token when the token is expired.
     * @return the IAM RDS token
     */
    @Override
    public String getPassword() {
        LocalDateTime now = LocalDateTime.now(clock);
        if (this.rdsIamAuthTokenGenerator == null) {
            initTokenGenerator();
        }
        if (this.expiryDate == null || expiryDate.isBefore(now)) {
            this.authToken = this.rdsIamAuthTokenGenerator.getAuthToken(rdsIamAuthTokenRequest);
            this.expiryDate = now.plusMinutes(10); // Token expires after 15 min, so renew after 10 min
        }
        return this.authToken;
    }
}

