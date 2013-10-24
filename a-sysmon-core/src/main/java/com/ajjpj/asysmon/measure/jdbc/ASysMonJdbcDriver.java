package com.ajjpj.asysmon.measure.jdbc;


import com.ajjpj.asysmon.ASysMon;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;


/**
 * @author arno
 */
public class ASysMonJdbcDriver implements Driver {
    public static final String URL_PREFIX = "asysmon:";
    public static final ASysMonJdbcDriver INSTANCE = new ASysMonJdbcDriver();

    static {
        try {
            DriverManager.registerDriver(INSTANCE);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deregister() throws SQLException {
        DriverManager.deregisterDriver(INSTANCE);
    }

    @Override public Connection connect(String url, Properties info) throws SQLException {
        if(! acceptsURL(url)) {
            return null;
        }

        final Connection inner = DriverManager.getConnection(url.substring(URL_PREFIX.length()), info);

        final ASysMon sysMon = ASysMon.get(); //TODO make this configurable - but how best to do that?!
        return new ASysMonConnection(inner, sysMon);
    }

    @Override public boolean acceptsURL(String url) throws SQLException {
        return url != null && url.startsWith(URL_PREFIX);
    }

    @Override public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return new DriverPropertyInfo[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public int getMajorVersion() {
        return 1;
    }

    @Override public int getMinorVersion() {
        return 0;
    }

    @Override public boolean jdbcCompliant() {
        return true; //TODO what to return here?!
    }

    // introduced with JDK 1.7
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }
}
