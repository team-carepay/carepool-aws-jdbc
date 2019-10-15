package com.carepay.jdbc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import com.carepay.jdbc.aws.AWS4RdsIamTokenGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RdsIamHikariDataSourceTest {

    private RdsIamHikariDataSource rdsIamHikariDataSource;

    @Before
    public void setUp() {
        System.setProperty("aws.accessKeyId", "IAMKEYINSTANCE");
        System.setProperty("aws.secretAccessKey", "asdfqwertypolly");
        System.setProperty("aws.token", "ZYX12345");
        final Clock brokenClock = Clock.fixed(Instant.parse("2018-09-19T16:02:42.00Z"), ZoneId.of("UTC"));
        RdsIamHikariDataSource.clock = brokenClock;
        AWS4RdsIamTokenGenerator.clock = brokenClock;
        rdsIamHikariDataSource = new RdsIamHikariDataSource();
        rdsIamHikariDataSource.setDriverClassName(H2Driver.class.getName());
        rdsIamHikariDataSource.setJdbcUrl("jdbc:mysql://mydb.random.eu-west-1.rds.amazonaws.com/database");
        rdsIamHikariDataSource.setUsername("iamuser");
    }

    @After
    public void tearDown() {
        rdsIamHikariDataSource.close();
        System.clearProperty("aws.accessKeyId");
        System.clearProperty("aws.secretAccessKey");
        System.clearProperty("aws.token");
    }

    @Test
    public void configureSSL() {
        assertThat(rdsIamHikariDataSource.getDataSourceProperties().getProperty("requireSSL")).isEqualTo("true");
    }

    @Test
    public void getPasswordIsEqual() {
        String password = rdsIamHikariDataSource.getPassword();
        assertThat(password).contains("X-Amz");
        String password2 = rdsIamHikariDataSource.getPassword();
        assertThat(password).isEqualTo(password2);
    }

    @Test
    public void getPasswordIsDifferentWhenExpired() throws InterruptedException {
        String password = rdsIamHikariDataSource.getPassword();
        final Clock brokenClock = Clock.fixed(Instant.parse("2019-10-20T16:02:42.00Z"), ZoneId.of("UTC"));
        RdsIamHikariDataSource.clock = brokenClock;
        AWS4RdsIamTokenGenerator.clock = brokenClock;
        Thread.sleep(1000); // to ensure AWS4 signature is using a different timestamp
        String password2 = rdsIamHikariDataSource.getPassword();
        assertThat(password).isNotEqualTo(password2);
    }
}
