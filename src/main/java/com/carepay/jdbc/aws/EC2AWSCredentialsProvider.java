package com.carepay.jdbc.aws;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static java.time.ZoneOffset.UTC;

/**
 * EC2 implementation of AWS credentials provider. (using http://169.254.169.254/latest/meta-data)
 */
public class EC2AWSCredentialsProvider implements AWSCredentialsProvider {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);
    protected static Clock clock = Clock.systemUTC();
    private static final URL SECURITY_CREDENTIALS_URL;
    private static final String ROLE;
    private AWSCredentials lastCredentials;
    private LocalDateTime expiryDate;

    static {
        try {
            URL url = new URL("http://169.254.169.254/latest/meta-data/iam/security-credentials/");
            ROLE = EC2MetadataUtils.queryMetaDataAsString(url);
            SECURITY_CREDENTIALS_URL = ROLE != null ? new URL(url, ROLE) : null;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    @Override
    public AWSCredentials getCredentials() {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(),UTC);
        if (SECURITY_CREDENTIALS_URL != null && (expiryDate == null || expiryDate.isBefore(now))) {
            Map<String, String> map = EC2MetadataUtils.queryMetaData(SECURITY_CREDENTIALS_URL);
            lastCredentials = new AWSCredentials(map.get("AccessKeyId"), map.get("SecretAccessKey"), map.get("Token"));
            final String expirationString = map.get("Expiration");
            if (lastCredentials.isValid() && expirationString != null) {
                expiryDate = LocalDateTime.parse(expirationString, DATE_FORMATTER);
            }
        }
        return lastCredentials;
    }
}
