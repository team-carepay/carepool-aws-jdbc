package com.carepay.jdbc.aws;

/**
 * Environment implementation of AWS credentials providers. (AWS_ACCESS_KEY_ID /
 * AWS_SECRET_ACCESS_KEY / AWS_TOKEN)
 */
public class EnvironmentAWSCredentialsProvider implements AWSCredentialsProvider {
    @Override
    public AWSCredentials getCredentials() {
        return new AWSCredentials(
                System.getenv("AWS_ACCESS_KEY_ID"),
                System.getenv("AWS_SECRET_ACCESS_KEY"),
                System.getenv("AWS_TOKEN")
        );
    }
}
