package com.carepay.jdbc.aws;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.Jsoner;

/**
 * Utility class to extract the AWS EC2 instance-id
 */
public class EC2MetadataUtils {

    private static final URL META_DATA_URL;

    static {
        try {
            META_DATA_URL = new URL("http://169.254.169.254/latest/dynamic/instance-identity/document");
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }
    private static Map<String, String> metaData;

    public static String getInstanceId() {
        return getMetaData().get("instanceId");
    }

    public static String getRegion() {
        return getMetaData().getOrDefault("region", "us-east-1");
    }

    public static Map<String, String> getMetaData() {
        if (metaData == null) {
            metaData = queryMetaData(META_DATA_URL);
        }
        return metaData;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> queryMetaData(final URL url) {
        try {
            final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(1000);
            urlConnection.setReadTimeout(1000);
            try (final InputStream is = urlConnection.getInputStream();
                 final InputStreamReader reader = new InputStreamReader(is)) {
                return (Map<String, String>) Jsoner.deserialize(reader);
            }
        } catch (IOException | JsonException e) { // NOSONAR
            return Collections.emptyMap();
        }
    }
}
