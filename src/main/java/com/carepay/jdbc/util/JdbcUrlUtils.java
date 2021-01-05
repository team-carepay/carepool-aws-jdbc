package com.carepay.jdbc.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JdbcUrlUtils {

    private static final Pattern JDBC_PATTERN = Pattern.compile("jdbc:[\\w:]+(?://|@)([\\w.-]+)(:\\d+)?.*");
    private static final int DEFAULT_PORT = 3306;

    protected JdbcUrlUtils() {
        /* Utility class */
    }

    public static URL extractJdbcURL(final String url) {
        final Matcher matcher = JDBC_PATTERN.matcher(url);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(url);
        }
        final String host = matcher.group(1);
        final String str = matcher.group(2);
        return createJdbcURL(host, str);
    }

    public static URL createJdbcURL(String host, String portStr) {
        final int port = portStr != null ? Integer.parseInt(portStr.substring(1)) : DEFAULT_PORT;
        return createURL("http", host, port, "");
    }

    public static URL createURL(final String protocol, final String host, final int port, final String file) {
        try {
            return new URL(protocol, host, port, file);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }
}
