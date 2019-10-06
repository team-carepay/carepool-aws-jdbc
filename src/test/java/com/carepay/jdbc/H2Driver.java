package com.carepay.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class H2Driver extends org.h2.Driver {
    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        info.setProperty("password","");
        return super.connect("jdbc:h2:mem:test;DB_CLOSE_ON_EXIT=false", info);
    }

    @Override
    public boolean acceptsURL(String url) {
        return url.startsWith("jdbc:mysql:") || super.acceptsURL(url);
    }
}
