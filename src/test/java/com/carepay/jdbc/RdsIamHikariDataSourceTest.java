package com.carepay.jdbc;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.carepay.jdbc.aws.AWS4RdsIamTokenGenerator;
import com.carepay.jdbc.aws.AWSCredentials;
import com.carepay.jdbc.aws.AWSCredentialsProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RdsIamHikariDataSourceTest {

    private RdsIamHikariDataSource rdsIamHikariDataSource;
    private Clock brokenClock;

    @Before
    public void setUp() {
        AWSCredentials credentials = new AWSCredentials("IAMKEYINSTANCE", "asdfqwertypolly", "ZYX12345");
        AWSCredentialsProvider credentialsProvider = () -> credentials;
        brokenClock = Clock.fixed(Instant.parse("2018-09-19T16:02:42.00Z"), ZoneId.of("UTC"));
        AWS4RdsIamTokenGenerator tokenGenerator = new AWS4RdsIamTokenGenerator(brokenClock) {
            @Override
            protected Instant getCurrentDateTime() {
                return brokenClock.instant();
            }
        };
        rdsIamHikariDataSource = new RdsIamHikariDataSource(tokenGenerator,credentialsProvider, brokenClock) {
            @Override
            protected LocalDateTime getCurrentDateTime() {
                return LocalDateTime.ofInstant(brokenClock.instant(), ZoneId.of("UTC"));
            }
        };
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
        brokenClock = Clock.fixed(Instant.parse("2019-10-20T16:02:42.00Z"), ZoneId.of("UTC"));
        String password2 = rdsIamHikariDataSource.getPassword();
        assertThat(password).isNotEqualTo(password2);
    }
}
