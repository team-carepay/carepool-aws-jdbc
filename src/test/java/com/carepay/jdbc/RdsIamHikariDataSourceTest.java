package com.carepay.jdbc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import com.amazonaws.services.rds.auth.GetIamAuthTokenRequest;
import com.amazonaws.services.rds.auth.RdsIamAuthTokenGenerator;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RdsIamHikariDataSourceTest {

    private RdsIamHikariDataSource rdsIamHikariDataSource;

    @Before
    public void setUp() {
        RdsIamHikariDataSource.clock = Clock.fixed(Instant.parse("2018-09-19T16:02:42.00Z"), ZoneId.of("UTC"));
        rdsIamHikariDataSource = new RdsIamHikariDataSource();
        rdsIamHikariDataSource.setDriverClassName(H2Driver.class.getName());
        rdsIamHikariDataSource.setJdbcUrl("jdbc:mysql://mydb.random.eu-west-1.rds.amazonaws.com/database");
        rdsIamHikariDataSource.setUsername("iamuser");
    }

    @Test
    public void configureSSL() {
        assertThat(rdsIamHikariDataSource.getDataSourceProperties().getProperty("requireSSL")).isEqualTo("true");
    }

    @Test
    public void getIamAuthTokenRequest() {
        GetIamAuthTokenRequest iamAuthTokenRequest = rdsIamHikariDataSource.getIamAuthTokenRequest("localhost",3306);
        assertThat(iamAuthTokenRequest.getHostname()).isEqualTo("localhost");
    }

    @Test
    public void getRdsIamAuthTokenGenerator() {
        RdsIamAuthTokenGenerator rdsIamAuthTokenGenerator = rdsIamHikariDataSource.getRdsIamAuthTokenGenerator("eu-west-1");
        assertThat(rdsIamAuthTokenGenerator).isNotNull();
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
        RdsIamHikariDataSource.clock = Clock.fixed(Instant.parse("2019-09-19T16:02:42.00Z"), ZoneId.of("UTC"));
        Thread.sleep(1000); // to ensure AWS4 signature is using a different timestamp
        String password2 = rdsIamHikariDataSource.getPassword();
        assertThat(password).isNotEqualTo(password2);
    }
}
