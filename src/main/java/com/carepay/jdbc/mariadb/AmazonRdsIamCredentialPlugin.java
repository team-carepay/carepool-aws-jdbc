/*
 * MariaDB Client for Java
 *
 * Copyright (c) 2012-2014 Monty Program Ab.
 * Copyright (c) 2015-2020 MariaDB Corporation Ab.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along
 * with this library; if not, write to Monty Program Ab info@montyprogram.com.
 *
 */

package com.carepay.jdbc.mariadb;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Properties;

import com.carepay.aws.auth.Credentials;
import com.carepay.aws.auth.CredentialsProvider;
import com.carepay.aws.auth.DefaultCredentialsProviderChain;
import com.carepay.aws.auth.RegionProvider;
import com.carepay.aws.region.DefaultRegionProviderChain;
import com.carepay.jdbc.RdsAWS4Signer;
import org.mariadb.jdbc.HostAddress;
import org.mariadb.jdbc.credential.Credential;
import org.mariadb.jdbc.credential.CredentialPlugin;
import org.mariadb.jdbc.util.Options;

import static java.time.ZoneOffset.UTC;

/**
 *
 */
public class AmazonRdsIamCredentialPlugin implements CredentialPlugin {

    private Clock clock = Clock.systemUTC();
    private RdsAWS4Signer signer;
    private LocalDateTime expiryDate;
    private HostAddress hostAddress;
    private String username;
    private Credential authToken;

    @Override
    public String type() {
        return "AWS4RDS";
    }

    @Override
    public String name() {
        return "Amazon RDS IAM Authentication plugin";
    }

    @Override
    public boolean mustUseSsl() {
        return true;
    }

    @Override
    public CredentialPlugin initialize(final Options options, final String username, final HostAddress hostAddress) {
        this.hostAddress = hostAddress;
        this.username = username;

        final Properties nonMappedOptions = options.nonMappedOptions;
        final String accessKeyId = nonMappedOptions.getProperty("accessKeyId");
        final String secretKey = nonMappedOptions.getProperty("secretKey");
        final String region = nonMappedOptions.getProperty("region");

        final CredentialsProvider awsCredentialsProvider = accessKeyId != null && secretKey != null ? () -> new Credentials(accessKeyId, secretKey, null) : new DefaultCredentialsProviderChain();
        final RegionProvider regionProvider = region != null ? () -> region : new DefaultRegionProviderChain();

        this.signer = new RdsAWS4Signer(awsCredentialsProvider, regionProvider, Clock.systemUTC());
        return this;
    }

    @Override
    public Credential get() {
        final LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), UTC);
        if (this.expiryDate == null || expiryDate.isBefore(now)) {
            this.authToken = new Credential(this.username, signer.generateToken(hostAddress.host, hostAddress.port, username));
            this.expiryDate = now.plusMinutes(10L); // Token expires after 15 min, so renew after 10 min
        }
        return authToken;
    }
}
