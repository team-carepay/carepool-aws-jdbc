package com.carepay.jdbc.util;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class JdbcUrlUtilsTest {

    @Test
    public void defaultConstructor() {
        assertThat(new JdbcUrlUtils()).isNotNull(); // NOSONAR
    }

    @Test
    public void extractJdbcURL() {
        final URL url = JdbcUrlUtils.extractJdbcURL("jdbc:mysql://my-db.host.com/dbname?zeroDateTimeBehavior=convertToNull");
        assertThat(url.getHost()).isEqualTo("my-db.host.com");
        assertThat(url.getPort()).isEqualTo(3306);
    }

    @Test
    public void extractAuroraJdbcURL() {
        final URL url = JdbcUrlUtils.extractJdbcURL("jdbc:mysql:aurora//my-db.host.com:3306/dbname?zeroDateTimeBehavior=convertToNull");
        assertThat(url.getHost()).isEqualTo("my-db.host.com");
        assertThat(url.getPort()).isEqualTo(3306);
    }

    @Test
    public void extractInvalidJdbcURL() {
        assertThatThrownBy(() -> JdbcUrlUtils.extractJdbcURL("INVALID!my-db.host.com:3306/db"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void createJdbcURL() throws MalformedURLException {
        assertThat(JdbcUrlUtils.createJdbcURL("host", ":3306")).isEqualTo(new URL("http://host:3306"));
        assertThat(JdbcUrlUtils.createJdbcURL("host", null)).isEqualTo(new URL("http://host:3306"));
    }

    @Test
    public void createURL() throws MalformedURLException {
        assertThat(JdbcUrlUtils.createURL("http", "host", 3306, "/path")).isEqualTo(new URL("http://host:3306/path"));
        try {
            JdbcUrlUtils.createURL("jdbc", "host", 3306, "/path");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
}