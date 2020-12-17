package com.carepay.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;

import com.carepay.aws.auth.Credentials;
import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class RdsIamTomcatDataSourceTest {

    private RdsIamTomcatDataSource rdsIamTomcatDataSource;
    private Clock brokenClock;
    private RdsAWS4Signer tokenGenerator;

    private void init() {
        rdsIamTomcatDataSource.setDriverClassName(H2Driver.class.getName());
        rdsIamTomcatDataSource.setUrl("jdbc:mysql://mydb.random.eu-west-1.rds.amazonaws.com/database");
        rdsIamTomcatDataSource.setUsername("iamuser");
    }

    @Before
    public void setUp() {
        RdsIamTomcatDataSource.defaultTimeout = 10L;
        this.brokenClock = mock(Clock.class);
        when(brokenClock.instant()).thenReturn(Instant.parse("2018-09-19T16:02:42.00Z"));
        tokenGenerator = new RdsAWS4Signer(() -> new Credentials("IAMKEYINSTANCE", "asdfqwertypolly", "ZYX12345"), () -> "eu-west-1", brokenClock);
        rdsIamTomcatDataSource = new RdsIamTomcatDataSource(tokenGenerator);
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
        PoolProperties newPoolProperties = new PoolProperties();
        rdsIamTomcatDataSource = new RdsIamTomcatDataSource(tokenGenerator, newPoolProperties) {
            @Override
            protected synchronized ConnectionPool createPoolImpl() throws SQLException {
                pool = new RdsIamAuthConnectionPool(newPoolProperties) {
                    @Override
                    protected void createBackgroundThread() {
                    }
                };
                return pool;
            }
        };
        init();
        rdsIamTomcatDataSource.setUrl("jdbc:mysql://mydb.random.eu-west-1.rds.amazonaws.com:3306/database");
        try (Connection c = rdsIamTomcatDataSource.getConnection()) {
            final String password = rdsIamTomcatDataSource.getPoolProperties().getPassword();
            reset(brokenClock);
            when(brokenClock.instant()).thenReturn(Instant.parse("2019-10-20T16:02:42.00Z"));
            ((Runnable) rdsIamTomcatDataSource.getPool()).run();
            assertThat(rdsIamTomcatDataSource.getPoolProperties().getPassword()).isNotEqualTo(password);
        }
    }
}
