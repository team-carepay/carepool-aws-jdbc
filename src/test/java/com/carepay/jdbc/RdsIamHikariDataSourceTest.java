package com.carepay.jdbc;

import java.time.Clock;
import java.time.Instant;

import com.carepay.aws.AWS4Signer;
import com.carepay.aws.AWSCredentials;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class RdsIamHikariDataSourceTest {

    private RdsIamHikariDataSource rdsIamHikariDataSource;
    private Clock brokenClock;
    private AWSCredentials credentials;

    @Before
    public void setUp() {
        brokenClock = mock(Clock.class);
        when(brokenClock.instant()).thenReturn(Instant.parse("2018-09-19T16:02:42.00Z"));
        credentials = new AWSCredentials("IAMKEYINSTANCE", "asdfqwertypolly", "ZYX12345");
        AWS4Signer tokenGenerator = new AWS4Signer(brokenClock, () -> credentials);
        rdsIamHikariDataSource = new RdsIamHikariDataSource(tokenGenerator, brokenClock);
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
        reset(brokenClock);
        when(brokenClock.instant()).thenReturn(Instant.parse("2019-10-20T16:02:42.00Z"));
        String password2 = rdsIamHikariDataSource.getPassword();
        assertThat(password).isNotEqualTo(password2);
    }
}
