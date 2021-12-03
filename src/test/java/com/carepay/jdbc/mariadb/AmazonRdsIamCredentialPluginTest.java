package com.carepay.jdbc.mariadb;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import com.carepay.aws.auth.Credentials;
import com.carepay.aws.util.URLOpener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mariadb.jdbc.HostAddress;
import org.mariadb.jdbc.credential.Credential;
import org.mariadb.jdbc.util.Options;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AmazonRdsIamCredentialPluginTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2018-09-19T16:02:42.00Z"), ZoneId.of("UTC"));
    private AmazonRdsIamCredentialPlugin plugin;

    @BeforeEach
    void setUp() throws IOException {
        plugin = new AmazonRdsIamCredentialPlugin(() -> new Credentials("IAMKEYINSTANCE", "asdfqwertypolly", "ZYX12345"), () -> "eu-west-1", CLOCK, new URLOpener.Default());
        plugin.initialize(new Options(), "testuser", new HostAddress("mysql-host-db.cluster-xxxxxxxxxx.eu-west-1.rds.amazonaws.com", 3306));
    }

    @Test
    void initializeUsingProps() throws IOException {
        HttpURLConnection uc = mock(HttpURLConnection.class);
        when(uc.getInputStream()).thenReturn(getClass().getResourceAsStream("/rds-combined-ca-bundle.pem"));
        AmazonRdsIamCredentialPlugin tempPlugin = new AmazonRdsIamCredentialPlugin(() -> new Credentials("IAMKEYINSTANCE", "asdfqwertypolly", "ZYX12345"), () -> "ap-southeast-1", CLOCK, u -> uc);
        Options options = new Options();
        options.serverSslCert = "https://s3.amazonaws.com/rds-downloads/rds-combined-ca-bundle.pem";
        options.nonMappedOptions.setProperty("accessKeyId", "IAMXXXXXXXXXX");
        options.nonMappedOptions.setProperty("secretKey", "PollyWantsACookie");
        options.nonMappedOptions.setProperty("region", "ap-southeast-1");
        tempPlugin.initialize(options, "anotheruser", new HostAddress("mysql-host-db.cluster-xxxxxxxxxx.ap-southeast-1.rds.amazonaws.com", 3306));
        Credential creds = tempPlugin.get();
        assertThat(creds.getUser()).isEqualTo("anotheruser");
        assertThat(creds.getPassword()).isEqualTo("mysql-host-db.cluster-xxxxxxxxxx.ap-southeast-1.rds.amazonaws.com:3306/?Action=connect&DBUser=anotheruser&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=IAMKEYINSTANCE%2F20180919%2Fap-southeast-1%2Frds-db%2Faws4_request&X-Amz-Date=20180919T160242Z&X-Amz-Expires=900&X-Amz-Security-Token=ZYX12345&X-Amz-SignedHeaders=host&X-Amz-Signature=0e2dddfa927d4a844c9c4799b0a8f766e479e1014230a52db7f50c72c922a8af");
    }

    @Test
    void get() {
        Credential creds = plugin.get();
        assertThat(creds.getUser()).isEqualTo("testuser");
        assertThat(creds.getPassword()).isEqualTo("mysql-host-db.cluster-xxxxxxxxxx.eu-west-1.rds.amazonaws.com:3306/?Action=connect&DBUser=testuser&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=IAMKEYINSTANCE%2F20180919%2Feu-west-1%2Frds-db%2Faws4_request&X-Amz-Date=20180919T160242Z&X-Amz-Expires=900&X-Amz-Security-Token=ZYX12345&X-Amz-SignedHeaders=host&X-Amz-Signature=73f05d152c9ebecd66f44d213f1fb11037143fd01c688c087ab1a82a08ce6ee0");
        Credential creds2 = plugin.get();
        assertThat(creds2.getPassword()).isEqualTo("mysql-host-db.cluster-xxxxxxxxxx.eu-west-1.rds.amazonaws.com:3306/?Action=connect&DBUser=testuser&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=IAMKEYINSTANCE%2F20180919%2Feu-west-1%2Frds-db%2Faws4_request&X-Amz-Date=20180919T160242Z&X-Amz-Expires=900&X-Amz-Security-Token=ZYX12345&X-Amz-SignedHeaders=host&X-Amz-Signature=73f05d152c9ebecd66f44d213f1fb11037143fd01c688c087ab1a82a08ce6ee0");
    }

    @Test
    void getExpired() {
        Clock fakeClock = mock(Clock.class);
        when(fakeClock.instant()).thenReturn(
                Instant.parse("2018-09-19T16:02:42.00Z"),
                Instant.parse("2018-09-19T16:02:42.00Z"),
                Instant.parse("2020-09-19T16:02:42.00Z"),
                Instant.parse("2020-09-19T16:02:42.00Z")
        );
        AmazonRdsIamCredentialPlugin tempPlugin = new AmazonRdsIamCredentialPlugin(() -> new Credentials("IAMKEYINSTANCE", "asdfqwertypolly", "ZYX12345"), () -> "eu-west-1", fakeClock, new URLOpener.Default());
        tempPlugin.initialize(new Options(), "testuser", new HostAddress("mysql-host-db.cluster-xxxxxxxxxx.eu-west-1.rds.amazonaws.com", 3306));
        Credential creds = tempPlugin.get();
        assertThat(creds.getUser()).isEqualTo("testuser");
        assertThat(creds.getPassword()).isEqualTo("mysql-host-db.cluster-xxxxxxxxxx.eu-west-1.rds.amazonaws.com:3306/?Action=connect&DBUser=testuser&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=IAMKEYINSTANCE%2F20180919%2Feu-west-1%2Frds-db%2Faws4_request&X-Amz-Date=20180919T160242Z&X-Amz-Expires=900&X-Amz-Security-Token=ZYX12345&X-Amz-SignedHeaders=host&X-Amz-Signature=73f05d152c9ebecd66f44d213f1fb11037143fd01c688c087ab1a82a08ce6ee0");
        Credential creds2 = tempPlugin.get();
        assertThat(creds2.getPassword()).isEqualTo("mysql-host-db.cluster-xxxxxxxxxx.eu-west-1.rds.amazonaws.com:3306/?Action=connect&DBUser=testuser&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=IAMKEYINSTANCE%2F20200919%2Feu-west-1%2Frds-db%2Faws4_request&X-Amz-Date=20200919T160242Z&X-Amz-Expires=900&X-Amz-Security-Token=ZYX12345&X-Amz-SignedHeaders=host&X-Amz-Signature=324232e908e9fe3ce5c7361ed04aef747ce4937fd858dca068ebb78381ed5519");
        Credential creds3 = tempPlugin.get();
        assertThat(creds3.getPassword()).isEqualTo("mysql-host-db.cluster-xxxxxxxxxx.eu-west-1.rds.amazonaws.com:3306/?Action=connect&DBUser=testuser&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=IAMKEYINSTANCE%2F20200919%2Feu-west-1%2Frds-db%2Faws4_request&X-Amz-Date=20200919T160242Z&X-Amz-Expires=900&X-Amz-Security-Token=ZYX12345&X-Amz-SignedHeaders=host&X-Amz-Signature=324232e908e9fe3ce5c7361ed04aef747ce4937fd858dca068ebb78381ed5519");
        verify(fakeClock, times(5)).instant();
    }
}