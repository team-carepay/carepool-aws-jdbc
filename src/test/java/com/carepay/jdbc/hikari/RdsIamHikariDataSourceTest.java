package com.carepay.jdbc.hikari;

import java.time.Clock;
import java.time.Instant;

import com.carepay.aws.auth.Credentials;
import com.carepay.jdbc.H2Driver;
import com.carepay.jdbc.RdsAWS4Signer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

class RdsIamHikariDataSourceTest {

    private RdsIamHikariDataSource rdsIamHikariDataSource;
    private Clock brokenClock;
    private Credentials credentials;
    private RdsAWS4Signer tokenGenerator;

    @BeforeEach
    void setUp() {
        brokenClock = mock(Clock.class);
        when(brokenClock.instant()).thenReturn(Instant.parse("2018-09-19T16:02:42.00Z"));
        credentials = new Credentials("IAMKEYINSTANCE", "asdfqwertypolly", "ZYX12345");
        tokenGenerator = new RdsAWS4Signer(() -> credentials, () -> "eu-west-1", brokenClock);
        rdsIamHikariDataSource = new RdsIamHikariDataSource(tokenGenerator, brokenClock);
        rdsIamHikariDataSource.setDriverClassName(H2Driver.class.getName());
        rdsIamHikariDataSource.setJdbcUrl("jdbc:mysql://mydb.random.eu-west-1.rds.amazonaws.com/database");
        rdsIamHikariDataSource.setUsername("iamuser");
    }

    @AfterEach
    void tearDown() {
        rdsIamHikariDataSource.close();
        System.clearProperty("aws.accessKeyId");
        System.clearProperty("aws.secretAccessKey");
        System.clearProperty("aws.token");
    }

    @Test
    void configureSSL() {
        assertThat(rdsIamHikariDataSource.getDataSourceProperties().getProperty("requireSSL")).isEqualTo("true");
    }

    @Test
    void getPasswordIsEqual() {
        String password = rdsIamHikariDataSource.getPassword();
        assertThat(password).contains("X-Amz");
        String password2 = rdsIamHikariDataSource.getPassword();
        assertThat(password).isEqualTo(password2);
    }

    @Test
    void getPasswordIsDifferentWhenExpired() {
        String password = rdsIamHikariDataSource.getPassword();
        reset(brokenClock);
        when(brokenClock.instant()).thenReturn(Instant.parse("2019-10-20T16:02:42.00Z"));
        String password2 = rdsIamHikariDataSource.getPassword();
        assertThat(password).isNotEqualTo(password2);
    }

    @Test
    void testExtractHostFromUrl() {
        rdsIamHikariDataSource.setJdbcUrl("jdbc:mysql://mydb.random.eu-west-1.rds.amazonaws.com/schema");
        rdsIamHikariDataSource.extractHostFromUrl();
        assertThat(rdsIamHikariDataSource.host).isEqualTo("mydb.random.eu-west-1.rds.amazonaws.com");
        assertThat(rdsIamHikariDataSource.port).isEqualTo(3306);
        rdsIamHikariDataSource.setJdbcUrl("jdbc:mysql://mydb.random.eu-west-1.rds.amazonaws.com:13306/schema");
        rdsIamHikariDataSource.extractHostFromUrl();
        assertThat(rdsIamHikariDataSource.host).isEqualTo("mydb.random.eu-west-1.rds.amazonaws.com");
        assertThat(rdsIamHikariDataSource.port).isEqualTo(13306);
    }

    @Test
    void testExtractHostFromAuroraUrl() {
        rdsIamHikariDataSource.setJdbcUrl("jdbc:mysql:aurora//mydb.random.eu-west-1.rds.amazonaws.com/schema");
        rdsIamHikariDataSource.extractHostFromUrl();
        assertThat(rdsIamHikariDataSource.host).isEqualTo("mydb.random.eu-west-1.rds.amazonaws.com");
        assertThat(rdsIamHikariDataSource.port).isEqualTo(3306);
    }


    @Test
    void testExtractHostFromOracleUrl() {
        rdsIamHikariDataSource.setJdbcUrl("jdbc:oracle:thin@dbhostname.domain-name.com:1521");
        rdsIamHikariDataSource.extractHostFromUrl();
        assertThat(rdsIamHikariDataSource.host).isEqualTo("dbhostname.domain-name.com");
        assertThat(rdsIamHikariDataSource.port).isEqualTo(1521);
    }
}
