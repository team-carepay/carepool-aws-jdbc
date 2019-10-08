package com.carepay.jdbc;

import com.amazonaws.services.rds.auth.GetIamAuthTokenRequest;
import com.amazonaws.services.rds.auth.RdsIamAuthTokenGenerator;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RdsIamHikariDataSourceTest {

    private RdsIamHikariDataSource rdsIamHikariDataSource;

    @Before
    public void setUp() {
        rdsIamHikariDataSource = new RdsIamHikariDataSource();
        rdsIamHikariDataSource.setDriverClassName(H2Driver.class.getName());
        rdsIamHikariDataSource.setJdbcUrl("jdbc:mysql://localhost/database");
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
    public void getPassword() {
        String password = rdsIamHikariDataSource.getPassword();
        assertThat(password).contains("X-Amz");
        String password2 = rdsIamHikariDataSource.getPassword();
        assertThat(password).isEqualTo(password2);
    }

}
