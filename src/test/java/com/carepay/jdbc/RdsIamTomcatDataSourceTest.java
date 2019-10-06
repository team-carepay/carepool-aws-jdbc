package com.carepay.jdbc;

import java.sql.SQLException;

import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RdsIamTomcatDataSourceTest {

    private RdsIamTomcatDataSource rdsIamTomcatDataSource;

    private void init() {
        rdsIamTomcatDataSource.setDriverClassName(H2Driver.class.getName());
        rdsIamTomcatDataSource.setUrl("jdbc:mysql://localhost/database");
        rdsIamTomcatDataSource.setUsername("iamuser");
    }

    @Before
    public void setUp() throws SQLException {
        rdsIamTomcatDataSource = new RdsIamTomcatDataSource();
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
        RdsIamTomcatDataSource.DEFAULT_TIMEOUT = 1L;
        init();
        rdsIamTomcatDataSource.getConnection();
        final String password = rdsIamTomcatDataSource.getPoolProperties().getPassword();
        ((RdsIamTomcatDataSource.RdsIamAuthConnectionPool)rdsIamTomcatDataSource.getPool()).run();
        assertThat(rdsIamTomcatDataSource.getPoolProperties().getPassword()).isNotEqualTo(password);
    }
}
