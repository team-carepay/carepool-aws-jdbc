package com.carepay.jdbc.aws;

/**
 * Provides access to AWS credentials
 */
public interface AWSCredentialsProvider {
    AWSCredentials getCredentials();
}
