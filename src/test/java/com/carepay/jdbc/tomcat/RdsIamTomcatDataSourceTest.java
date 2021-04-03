package com.carepay.jdbc.tomcat;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.carepay.aws.auth.Credentials;
import com.carepay.jdbc.H2Driver;
import com.carepay.jdbc.RdsAWS4Signer;
import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class RdsIamTomcatDataSourceTest {

    private RdsIamTomcatDataSource rdsIamTomcatDataSource;
    private Clock brokenClock;
    private RdsAWS4Signer tokenGenerator;
    private ScheduledExecutorService scheduledExecutorService;
    private ArgumentCaptor<Runnable> runnableArgumentCaptor;
    private ScheduledFuture<?> backgroundFuture;
    private PoolProperties poolProperties;

    private void init() {
        rdsIamTomcatDataSource.setDriverClassName(H2Driver.class.getName());
        rdsIamTomcatDataSource.setUrl("jdbc:mysql://mydb.random.eu-west-1.rds.amazonaws.com/database");
        rdsIamTomcatDataSource.setUsername("iamuser");
    }

    @BeforeEach
    public void setUp() {
        poolProperties = new PoolProperties();
        this.brokenClock = mock(Clock.class);
        when(brokenClock.instant()).thenReturn(Instant.parse("2018-09-19T16:02:42.00Z"));
        tokenGenerator = new RdsAWS4Signer(() -> new Credentials("IAMKEYINSTANCE", "asdfqwertypolly", "ZYX12345"), () -> "eu-west-1", brokenClock);
        scheduledExecutorService = mock(ScheduledExecutorService.class);
        runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        backgroundFuture = mock(ScheduledFuture.class);
        when(scheduledExecutorService.scheduleWithFixedDelay(runnableArgumentCaptor.capture(), anyLong(), anyLong(), any(TimeUnit.class))).thenAnswer((Answer<ScheduledFuture<?>>) invocation -> backgroundFuture);
        rdsIamTomcatDataSource = new RdsIamTomcatDataSource(tokenGenerator, scheduledExecutorService, poolProperties);
        init();
    }

    @AfterEach
    public void tearDown() {
        rdsIamTomcatDataSource.close();
    }

    @Test
    public void testGetConnectionCreatesToken() throws SQLException {
        try (Connection conn = rdsIamTomcatDataSource.getConnection()) {
            runnableArgumentCaptor.getValue().run();
            assertThat(rdsIamTomcatDataSource.getPoolProperties().getPassword()).contains("Amz");
        }
    }

    @Test
    public void testBackgroundThreadCreatesNewPassword() throws SQLException {
        try (Connection c = rdsIamTomcatDataSource.getConnection()) {
            final String password = rdsIamTomcatDataSource.getPoolProperties().getPassword();
            reset(brokenClock);
            when(brokenClock.instant()).thenReturn(Instant.parse("2019-10-20T16:02:42.00Z"));
            runnableArgumentCaptor.getValue().run();
            assertThat(rdsIamTomcatDataSource.getPoolProperties().getPassword()).isNotEqualTo(password);
        }
    }

    @Test
    public void testCreatePoolImplOnce() throws SQLException {
        final ConnectionPool pool1 = rdsIamTomcatDataSource.createPool();
        final ConnectionPool pool2 = rdsIamTomcatDataSource.createPool();
        assertThat(pool1 == pool2).isTrue(); //NOSONAR
    }

    @Test
    public void testConstructor() {
        assertThat(new RdsIamTomcatDataSource(tokenGenerator, scheduledExecutorService)).isNotNull();
    }
}
