package com.carepay.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import com.carepay.jdbc.aws.AWS4RdsIamTokenGenerator;
import com.carepay.jdbc.aws.AWSCredentials;
import com.carepay.jdbc.aws.AWSCredentialsProvider;
import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RdsIamTomcatDataSourceTest {

    private RdsIamTomcatDataSource rdsIamTomcatDataSource;
    private Clock brokenClock;
    private AWS4RdsIamTokenGenerator tokenGenerator;
    private AWSCredentialsProvider credentialsProvider;

    private void init() {
        rdsIamTomcatDataSource.setDriverClassName(H2Driver.class.getName());
        rdsIamTomcatDataSource.setUrl("jdbc:mysql://mydb.random.eu-west-1.rds.amazonaws.com/database");
        rdsIamTomcatDataSource.setUsername("iamuser");
    }

    @Before
    public void setUp() {
        this.brokenClock = Clock.fixed(Instant.parse("2018-09-19T16:02:42.00Z"), ZoneId.of("UTC"));
        tokenGenerator = new AWS4RdsIamTokenGenerator(brokenClock) {
            @Override
            protected Instant getCurrentDateTime() {
                return brokenClock.instant();
            }
        };
        credentialsProvider = () -> new AWSCredentials("IAMKEYINSTANCE", "asdfqwertypolly", "ZYX12345");
        rdsIamTomcatDataSource = new RdsIamTomcatDataSource(tokenGenerator, credentialsProvider) {
            protected void sleep() throws InterruptedException {
            }
        };
        init();
    }

    @After
    public void tearDown() {
        rdsIamTomcatDataSource.close();
    }

    @Test
    public void testGetConnectionCreatesToken() throws SQLException {
        rdsIamTomcatDataSource.getConnection();
        assertThat(rdsIamTomcatDataSource.getPoolProperties().getPassword()).contains("Amz");
    }

    @Test
    public void testBackgroundThreadCreatesNewPassword() throws SQLException {
        rdsIamTomcatDataSource = new RdsIamTomcatDataSource(tokenGenerator,credentialsProvider) {
            @Override
            protected synchronized ConnectionPool createPoolImpl() throws SQLException {
                poolProperties.setRemoveAbandonedTimeout(10);
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
            brokenClock = Clock.fixed(Instant.parse("2019-10-20T16:02:42.00Z"), ZoneId.of("UTC"));
            ((Runnable)rdsIamTomcatDataSource.getPool()).run();
            assertThat(rdsIamTomcatDataSource.getPoolProperties().getPassword()).isNotEqualTo(password);
        }
    }
}
