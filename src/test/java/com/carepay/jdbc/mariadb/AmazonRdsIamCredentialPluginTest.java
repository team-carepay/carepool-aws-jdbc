package com.carepay.jdbc.mariadb;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import com.carepay.aws.auth.Credentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mariadb.jdbc.HostAddress;
import org.mariadb.jdbc.credential.Credential;
import org.mariadb.jdbc.util.Options;

import static org.assertj.core.api.Assertions.assertThat;

class AmazonRdsIamCredentialPluginTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2018-09-19T16:02:42.00Z"), ZoneId.of("UTC"));
    private AmazonRdsIamCredentialPlugin plugin;

    @BeforeEach
    void setUp() {
        plugin = new AmazonRdsIamCredentialPlugin(CLOCK, () -> new Credentials("IAMKEYINSTANCE", "asdfqwertypolly", "ZYX12345"), () -> "eu-west-1");
        plugin.initialize(new Options(),"testuser", new HostAddress("mysql-host-db.cluster-xxxxxxxxxx.eu-west-1.rds.amazonaws.com", 3306));
    }

    @Test
    void initializeUsingProps() {
        AmazonRdsIamCredentialPlugin tempPlugin = new AmazonRdsIamCredentialPlugin(CLOCK, null, null);
        Options options = new Options();
        options.nonMappedOptions.setProperty("accessKeyId", "IAMXXXXXXXXXX");
        options.nonMappedOptions.setProperty("secretKey", "PollyWantsACookie");
        options.nonMappedOptions.setProperty("region", "ap-southeast-1");
        tempPlugin.initialize(options,"anotheruser", new HostAddress("mysql-host-db.cluster-xxxxxxxxxx.ap-southeast-1.rds.amazonaws.com", 3306));
        Credential creds = tempPlugin.get();
        assertThat(creds.getUser()).isEqualTo("anotheruser");
        assertThat(creds.getPassword()).isEqualTo("mysql-host-db.cluster-xxxxxxxxxx.ap-southeast-1.rds.amazonaws.com:3306/?Action=connect&DBUser=anotheruser&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=IAMXXXXXXXXXX%2F20180919%2Fap-southeast-1%2Frds-db%2Faws4_request&X-Amz-Date=20180919T160242Z&X-Amz-Expires=900&X-Amz-SignedHeaders=host&X-Amz-Signature=58ee4aed2efba3cc2aaa43abdb319a884a2c42c8bc034ba1806ab87f78689974");
    }

    @Test
    void get() {
        Credential creds = plugin.get();
        assertThat(creds.getUser()).isEqualTo("testuser");
        assertThat(creds.getPassword()).isEqualTo("mysql-host-db.cluster-xxxxxxxxxx.eu-west-1.rds.amazonaws.com:3306/?Action=connect&DBUser=testuser&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=IAMKEYINSTANCE%2F20180919%2Feu-west-1%2Frds-db%2Faws4_request&X-Amz-Date=20180919T160242Z&X-Amz-Expires=900&X-Amz-Security-Token=ZYX12345&X-Amz-SignedHeaders=host&X-Amz-Signature=73f05d152c9ebecd66f44d213f1fb11037143fd01c688c087ab1a82a08ce6ee0");
    }
}