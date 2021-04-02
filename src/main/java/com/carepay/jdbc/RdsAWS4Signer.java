package com.carepay.jdbc;

import java.time.Clock;

import com.carepay.aws.auth.AWS4Signer;
import com.carepay.aws.auth.CredentialsProvider;
import com.carepay.aws.auth.RegionProvider;
import com.carepay.jdbc.util.DBHttpURLConnection;

import static com.carepay.jdbc.util.JdbcUrlUtils.createURL;

public class RdsAWS4Signer extends AWS4Signer {
    public RdsAWS4Signer() {
        super("rds-db");
    }

    public RdsAWS4Signer(CredentialsProvider credentialsProvider, RegionProvider regionProvider, Clock clock) {
        super("rds-db", credentialsProvider, regionProvider, clock);
    }

    /**
     * @param host     database hostname (dbname.xxxx.eu-west-1.rds.amazonaws.com)
     * @param port     database port (MySQL uses 3306)
     * @param username database username
     * @return the DB token
     */
    public String generateToken(final String host, final int port, final String username) {
        return new SignRequest(new DBHttpURLConnection(createURL("https", host, port, "/?Action=connect&DBUser=" + username))).signQuery();
    }

}
