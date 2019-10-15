package com.carepay.jdbc.aws;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.WeakHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Supports signing AWS RDS requests.
 * See https://docs.aws.amazon.com/general/latest/gr/signing_aws_api_requests.html
 * and https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/UsingWithRDS.IAMDBAuth.Connecting.Java.html
 */
public class AWS4RdsIamTokenGenerator {
    private static final ZoneId UTC = ZoneId.of("UTC");
    /**
     * the date-format used by AWS
     */
    private static final DateTimeFormatter AWS_DATE_FMT = DateTimeFormatter
            .ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(UTC);
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();
    /**
     * for performance reasons we cache the signing key, TTL is 24 hours
     */
    private static final Map<String, SigningKey> keyCache = new WeakHashMap<>();
    private final Clock clock;

    public AWS4RdsIamTokenGenerator() {
        this(Clock.systemUTC());
    }

    public AWS4RdsIamTokenGenerator(Clock clock) {
        this.clock = clock;
    }

    protected static MessageDigest getMessageDigest() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256");
    }

    protected static Mac getMac() throws NoSuchAlgorithmException {
        return Mac.getInstance("HmacSHA256");
    }

    /**
     * HEX encode an array of bytes to lowercase hex string.
     *
     * @param bytes the byte array
     * @return a hex representation of the bytes
     */
    protected static String hex(byte[] bytes) {
        final StringBuilder sb = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            sb.append(HEX_DIGITS[(b >> 4) & 0xf]).append(HEX_DIGITS[b & 0xf]);
        }
        return sb.toString();
    }

    /**
     * Applies Sha256 hashing on a string
     *
     * @param value the input string
     * @return the Sha256 digest in hex format
     */
    protected static String hash(String value) throws NoSuchAlgorithmException {
        return hex(getMessageDigest().digest(value.getBytes(UTF_8)));
    }

    /**
     * Calculates a signature for a String input
     *
     * @param value the String to sign
     * @param key   the key to use
     * @return the signature
     */
    private static byte[] sign(String value, byte[] key) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = getMac();
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(value.getBytes(UTF_8));
    }

    /**
     *
     * @param host database hostname (dbname.xxxx.eu-west-1.rds.amazonaws.com)
     * @param port database port (MySQL uses 3306)
     * @param dbuser database username
     * @param credentials the credentials to use for signing
     * @return the DB token
     */
    public String createDbAuthToken(final String host, final int port, final String dbuser, final AWSCredentials credentials) {
        final String[] parts = host.split("\\."); // e.g. xxxx.yyyy.eu-west-1.rds.amazonaws.com
        final String region = parts[2]; // extract region from hostname
        final String dateTimeStr = AWS_DATE_FMT.format(getCurrentDateTime());
        final String dateStr = dateTimeStr.substring(0, 8);
        StringBuilder queryBuilder = new StringBuilder("Action=connect")
            .append("&DBUser=").append(dbuser)
            .append("&X-Amz-Algorithm=AWS4-HMAC-SHA256")
            .append("&X-Amz-Credential=").append(String.join("%2F", credentials.getAccessKeyId(), dateStr, region, "rds-db", "aws4_request"))
            .append("&X-Amz-Date=").append(dateTimeStr)
            .append("&X-Amz-Expires=900")
            .append("&X-Amz-SignedHeaders=host");
        final String canonicalRequestStr = String.join("\n",
                "GET",
                "/", // path
                queryBuilder.toString(),
                "host:" + host+":"+port,
                "", // indicates end of signed headers
                "host", // signed header names
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"); // sha256 hash of empty string
        try {
            final String stringToSign = String.join("\n",
                    "AWS4-HMAC-SHA256", // algorithm
                    dateTimeStr, // timestamp
                    String.join("/", dateStr, region, "rds-db", "aws4_request"), // scope
                    hash(canonicalRequestStr)); // hash of the request
            final byte[] signingKeyBytes = getSigningKey(credentials, "rds-db", region, dateStr);
            final byte[] signature = sign(stringToSign, signingKeyBytes);
            queryBuilder.append("&X-Amz-Signature=").append(hex(signature));
            if (credentials.getToken() != null) {
                queryBuilder.append("&X-Amz-Security-Token").append(credentials.getToken());
            }
            return host + ":" + port + "/?" + queryBuilder.toString();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    protected Instant getCurrentDateTime() {
        return clock.instant();
    }

    /**
     * Returns the signing key, or creates a new signing-key if required.
     *
     * @param credentials the AWS credentials to use
     * @param service     the name of the service
     * @param region      the AWS region (e.g. us-east-1)
     * @param dateStr     the date-string in YYYYMMDD format
     */
    protected byte[] getSigningKey(AWSCredentials credentials, String service, String region, String dateStr) throws InvalidKeyException, NoSuchAlgorithmException {
        final String cacheKey = credentials.getAccessKeyId() + region;
        SigningKey signingKey = keyCache.get(cacheKey);
        if (signingKey == null || !signingKey.date.equals(dateStr)) {
            final byte[] kSecret = ("AWS4" + credentials.getSecretAccessKey()).getBytes(UTF_8);
            final byte[] kDate = sign(dateStr, kSecret);
            final byte[] kRegion = sign(region, kDate);
            final byte[] kService = sign(service, kRegion);
            final byte[] signingKeyBytes = sign("aws4_request", kService);
            signingKey = new SigningKey(dateStr, signingKeyBytes);
            keyCache.put(cacheKey, signingKey);
        }
        return signingKey.key;
    }

    /**
     * Utility class to cache signing keys
     */
    private static class SigningKey {
        private String date;
        private byte[] key;

        SigningKey(String date, byte[] key) {
            this.date = date;
            this.key = key;
        }
    }
}
