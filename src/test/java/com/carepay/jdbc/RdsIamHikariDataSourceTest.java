package com.carepay.jdbc;

import java.time.Clock;
import java.time.Instant;

import com.carepay.aws.auth.AWS4Signer;
import com.carepay.aws.auth.Credentials;
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
    private Credentials credentials;
    private AWS4Signer tokenGenerator;

    @Before
    public void setUp() {
        brokenClock = mock(Clock.class);
        when(brokenClock.instant()).thenReturn(Instant.parse("2018-09-19T16:02:42.00Z"));
        credentials = new Credentials("IAMKEYINSTANCE", "asdfqwertypolly", "ZYX12345");
        tokenGenerator = new AWS4Signer(() -> credentials, () -> "eu-west-1", brokenClock);
        rdsIamHikariDataSource = new RdsIamHikariDataSource(tokenGenerator, brokenClock);
        rdsIamHikariDataSource.setDriverClassName(H2Driver.class.getName());
        rdsIamHikariDataSource.setJdbcUrl("jdbc:mysql://mydb.random.eu-west-1.rds.amazonaws.com/database");
        rdsIamHikariDataSource.setUsername("iamuser");
        rdsIamHikariDataSource.managedStart();
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
    public void getPasswordIsDifferentWhenExpired() {
        String password = rdsIamHikariDataSource.getPassword();
        reset(brokenClock);
        when(brokenClock.instant()).thenReturn(Instant.parse("2019-10-20T16:02:42.00Z"));
        String password2 = rdsIamHikariDataSource.getPassword();
        assertThat(password).isNotEqualTo(password2);
    }

    @Test
    public void unmanagedTest() {
        rdsIamHikariDataSource = new RdsIamHikariDataSource(tokenGenerator, brokenClock);
        rdsIamHikariDataSource.setDriverClassName(H2Driver.class.getName());
        rdsIamHikariDataSource.setJdbcUrl("jdbc:mysql://mydb.random.eu-west-1.rds.amazonaws.com/database");
        rdsIamHikariDataSource.setUsername("iamuser");
        rdsIamHikariDataSource.setPassword("Secret123");
        assertThat(rdsIamHikariDataSource.getPassword()).isEqualTo("Secret123");
        rdsIamHikariDataSource.managedStart();
        assertThat(rdsIamHikariDataSource.getPassword()).isEqualTo("mydb.random.eu-west-1.rds.amazonaws.com:3306/?Action=connect&DBUser=iamuser&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=IAMKEYINSTANCE%2F20180919%2Feu-west-1%2Frds-db%2Faws4_request&X-Amz-Date=20180919T160242Z&X-Amz-Expires=900&X-Amz-Security-Token=ZYX12345&X-Amz-SignedHeaders=host&X-Amz-Signature=e97dadda634d1efb530e381b8c5bccb10cf6ef824973cb28f44212cb2d6d24da");
    }
}
