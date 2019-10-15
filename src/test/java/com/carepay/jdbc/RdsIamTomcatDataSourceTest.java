package com.carepay.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import com.carepay.jdbc.aws.AWS4RdsIamTokenGenerator;
import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RdsIamTomcatDataSourceTest {

    private RdsIamTomcatDataSource rdsIamTomcatDataSource;

    private void init() {
        rdsIamTomcatDataSource.setDriverClassName(H2Driver.class.getName());
        rdsIamTomcatDataSource.setUrl("jdbc:mysql://mydb.random.eu-west-1.rds.amazonaws.com/database");
        rdsIamTomcatDataSource.setUsername("iamuser");
    }

    @Before
    public void setUp() {
        System.setProperty("aws.accessKeyId", "IAMKEYINSTANCE");
        System.setProperty("aws.secretAccessKey", "asdfqwertypolly");
        System.setProperty("aws.token", "ZYX12345");
        AWS4RdsIamTokenGenerator.clock = Clock.fixed(Instant.parse("2018-09-19T16:02:42.00Z"), ZoneId.of("UTC"));
        rdsIamTomcatDataSource = new RdsIamTomcatDataSource();
        init();
    }

    @After
    public void tearDown() {
        rdsIamTomcatDataSource.close();
        System.clearProperty("aws.accessKeyId");
        System.clearProperty("aws.secretAccessKey");
        System.clearProperty("aws.token");
    }

    @Test
    public void testGetConnectionCreatesToken() throws SQLException {
        rdsIamTomcatDataSource.getConnection();
        assertThat(rdsIamTomcatDataSource.getPoolProperties().getPassword()).contains("Amz");
    }

    @Test
    public void testBackgroundThreadCreatesNewPassword() throws SQLException {
        RdsIamTomcatDataSource.DEFAULT_TIMEOUT = 1000L;
        rdsIamTomcatDataSource = new RdsIamTomcatDataSource() {
            @Override
            protected synchronized ConnectionPool createPoolImpl() throws SQLException {
                pool = new RdsIamAuthConnectionPool(poolProperties) {
                    @Override
                    protected void createBackgroundThread() {
                    }
                };
                return pool;
            }
        };
        init();
        try (Connection c = rdsIamTomcatDataSource.getConnection()) {
            final String password = rdsIamTomcatDataSource.getPoolProperties().getPassword();
            AWS4RdsIamTokenGenerator.clock = Clock.fixed(Instant.parse("2019-10-20T16:02:42.00Z"), ZoneId.of("UTC"));
            ((RdsIamTomcatDataSource.RdsIamAuthConnectionPool) rdsIamTomcatDataSource.getPool()).run();
            assertThat(rdsIamTomcatDataSource.getPoolProperties().getPassword()).isNotEqualTo(password);
        }
    }
}
