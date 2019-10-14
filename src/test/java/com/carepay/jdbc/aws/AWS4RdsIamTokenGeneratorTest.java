package com.carepay.jdbc.aws;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AWS4RdsIamTokenGeneratorTest {
    private AWS4RdsIamTokenGenerator tokenGenerator;

    @Before
    public void setUp() {
        AWS4RdsIamTokenGenerator.clock = Clock.fixed(Instant.parse("2018-09-19T16:02:42.00Z"), ZoneId.of("UTC"));
        tokenGenerator = new AWS4RdsIamTokenGenerator();
    }

    @Test
    public void testGeneratorSignature() {
        AWSCredentials awsCredentials = new AWSCredentials("IAMKEYINSTANCE", "asdfqwertypolly", null);
        String token = tokenGenerator.createDbAuthToken("dbhost.xyz.eu-west-1.amazonaws.com", 3306, "iam_user", awsCredentials);
        assertThat(token).isEqualTo("dbhost.xyz.eu-west-1.amazonaws.com:3306/?Action=connect&DBUser=iam_user&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=IAMKEYINSTANCE%2F20180919%2Feu-west-1%2Frds-db%2Faws4_request&X-Amz-Date=20180919T160242Z&X-Amz-Expires=900&X-Amz-SignedHeaders=host&X-Amz-Signature=fd30489c945fcb138e608b61851e9759e7e46983abd206d79eb75013b9c58eeb");
    }
}
