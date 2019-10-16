package com.carepay.jdbc.aws;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * EC2 implementation of AWS credentials provider. (using http://169.254.169.254/latest/meta-data)
 */
public class EC2AWSCredentialsProvider implements AWSCredentialsProvider {
    protected static Clock clock = Clock.systemUTC();
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final URL SECURITY_CREDENTIALS_URL;
    private AWSCredentials lastCredentials;
    private LocalDateTime expiryDate;

    static {
        try {
            SECURITY_CREDENTIALS_URL = new URL("http://169.254.169.254/latest/meta-data/identity-credentials/ec2/security-credentials/ec2-instance");
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    @Override
    public AWSCredentials getCredentials() {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(),UTC);
        if (expiryDate == null || expiryDate.isBefore(now)) {
            Map<String, String> map = EC2MetadataUtils.queryMetaData(SECURITY_CREDENTIALS_URL);
            lastCredentials = new AWSCredentials(map.get("AccessKeyId"), map.get("SecretAccessKey"), map.get("Token"));
            final String expirationString = map.get("Expiration");
            if (lastCredentials.isValid() && expirationString != null) {
                expiryDate = LocalDateTime.parse(map.get("Expiration"), DateTimeFormatter.ISO_INSTANT);
            }
        }
        return lastCredentials;
    }
}
