package com.carepay.jdbc.aws;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.WeakHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZoneOffset.UTC;

/**
 * Supports signing AWS RDS requests. See https://docs.aws.amazon.com/general/latest/gr/signing_aws_api_requests.html
 * and https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/UsingWithRDS.IAMDBAuth.Connecting.Java.html
 */
public class AWS4RdsIamTokenGenerator {
    /**
     * the date-format used by AWS
     */
    private static final DateTimeFormatter AWS_DATE_FMT = DateTimeFormatter
            .ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(UTC);
    private static final char[] HEX_DIGITS_LOWER = "0123456789abcdef".toCharArray();
    private static final char[] HEX_DIGITS_UPPER = "0123456789ABCDEF".toCharArray();

    /**
     * for performance reasons we cache the signing key, TTL is 24 hours
     */
    private static final Map<String, SigningKey> keyCache = new WeakHashMap<>();
    private static final String RDS_DB = "rds-db";
    private static final String AWS_4_REQUEST = "aws4_request";
    private static final String EMPTY_STRING_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private final Clock clock;

    public AWS4RdsIamTokenGenerator() {
        this(Clock.systemUTC());
    }

    public AWS4RdsIamTokenGenerator(Clock clock) {
        this.clock = clock;
    }

    @SuppressWarnings("squid:S00112")
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
            sb.append(HEX_DIGITS_LOWER[(b >> 4) & 0xf]).append(HEX_DIGITS_LOWER[b & 0xf]);
        }
        return sb.toString();
    }

    protected static void appendHex(StringBuilder sb, char ch) {
        sb.append('%').append(HEX_DIGITS_LOWER[(ch >> 4) & 0xf]).append(HEX_DIGITS_LOWER[ch & 0xf]);
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

    public static String uriEncode(CharSequence input) {
        StringBuilder result = new StringBuilder();
        final int len = input.length();
        for (int i = 0; i < len; i++) {
            char ch = input.charAt(i);
            if ((ch >= 'A' && ch <= 'Z')
                    || (ch >= 'a' && ch <= 'z')
                    || (ch >= '0' && ch <= '9')
                    || ch == '_' || ch == '-' || ch == '~' || ch == '.') {
                result.append(ch);
            } else {
                result.append('%').append(HEX_DIGITS_UPPER[(ch >> 4) & 0xf]).append(HEX_DIGITS_UPPER[ch & 0xf]);
            }
        }
        return result.toString();
    }

    /**
     * @param host        database hostname (dbname.xxxx.eu-west-1.rds.amazonaws.com)
     * @param port        database port (MySQL uses 3306)
     * @param dbuser      database username
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
                .append("&X-Amz-Credential=").append(String.join("%2F", credentials.getAccessKeyId(), dateStr, region, RDS_DB, AWS_4_REQUEST))
                .append("&X-Amz-Date=").append(dateTimeStr)
                .append("&X-Amz-Expires=900");
        if (credentials.hasToken()) {
            queryBuilder.append("&X-Amz-Security-Token=").append(uriEncode(credentials.getToken()));
        }
        queryBuilder
                .append("&X-Amz-SignedHeaders=host");
        final String canonicalRequestStr = String.join("\n",
                "GET",
                "/", // path
                queryBuilder.toString(),
                "host:" + host + ":" + port,
                "", // indicates end of signed headers
                "host", // signed header names
                EMPTY_STRING_SHA256); // sha256 hash of empty string
        try {
            final String stringToSign = String.join("\n",
                    "AWS4-HMAC-SHA256", // algorithm
                    dateTimeStr, // timestamp
                    String.join("/", dateStr, region, RDS_DB, AWS_4_REQUEST), // scope
                    hash(canonicalRequestStr)); // hash of the request
            final byte[] signingKeyBytes = getSigningKey(credentials, RDS_DB, region, dateStr);
            final byte[] signature = sign(stringToSign, signingKeyBytes);
            queryBuilder.append("&X-Amz-Signature=").append(hex(signature));
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
            final byte[] signingKeyBytes = sign(AWS_4_REQUEST, kService);
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
