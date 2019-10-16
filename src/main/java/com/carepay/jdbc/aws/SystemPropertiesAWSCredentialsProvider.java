package com.carepay.jdbc.aws;

public class SystemPropertiesAWSCredentialsProvider implements AWSCredentialsProvider {
    @Override
    public AWSCredentials getCredentials() {
        return new AWSCredentials(
                System.getProperty("aws.accessKeyId"),
                System.getProperty("aws.secretAccessKey"),
                System.getProperty("aws.token")
        );
    }
}
