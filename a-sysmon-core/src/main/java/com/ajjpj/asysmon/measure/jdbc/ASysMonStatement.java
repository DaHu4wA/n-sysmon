package com.ajjpj.asysmon.measure.jdbc;

import com.ajjpj.asysmon.ASysMon;
import com.ajjpj.asysmon.measure.ACollectingMeasurement;
import com.ajjpj.asysmon.measure.AMeasureCallback;
import com.ajjpj.asysmon.measure.AWithParameters;

import java.sql.*;

/**
 * @author arno
 */
public class ASysMonStatement implements Statement {
    public static final String IDENT_PREFIX_JDBC = "jdbc:";
    public static final String IDENT_EXECUTE = "execute";

    private final Connection conn;
    private final Statement inner;
    private final ASysMon sysMon;

    private boolean closeOnCompletion = false;

    public ASysMonStatement(Connection conn, Statement inner, ASysMon sysMon) {
        this.conn = conn;
        this.inner = inner;
        this.sysMon = sysMon;
    }

    public static String ident(String sql) {
        return IDENT_PREFIX_JDBC + " " + sql;
    }

    //-------------------------- Wrapper interface

    @Override public <T> T unwrap(Class<T> iface) throws SQLException {
        return inner.unwrap(iface); //TODO wrap this in a dynamic proxy to allow instrumentation?
    }

    @Override public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return inner.isWrapperFor(iface);
    }

    //--------------------------- misc

    @Override public Connection getConnection() throws SQLException {
        return conn;
    }

    //-------------------------- execute

    @Override public ResultSet executeQuery(String sql) throws SQLException {
        final ACollectingMeasurement m = sysMon.startCollectingMeasurement(ident(sql));
        m.startDetail(IDENT_EXECUTE);

        ResultSet rs;
        try {
            rs = inner.executeQuery(sql);
        } finally {
            m.finishDetail();
        }

        return wrap(rs, m);
    }

    @Override public int executeUpdate(final String sql) throws SQLException {
        return sysMon.measure(ident(sql), new AMeasureCallback<Integer, SQLException>() {
            @Override
            public Integer call(AWithParameters m) throws SQLException {
                return inner.executeUpdate(sql);
            }
        });
    }

    @Override public boolean execute(final String sql) throws SQLException {
        return sysMon.measure(ident(sql), new AMeasureCallback<Boolean, SQLException>() {
            @Override
            public Boolean call(AWithParameters m) throws SQLException {
                return inner.execute(sql);
            }
        });
    }

    @Override public int executeUpdate(final String sql, final int autoGeneratedKeys) throws SQLException {
        return sysMon.measure(ident(sql), new AMeasureCallback<Integer, SQLException>() {
            @Override
            public Integer call(AWithParameters m) throws SQLException {
                return inner.executeUpdate(sql, autoGeneratedKeys);
            }
        });
    }

    @Override public int executeUpdate(final String sql, final int[] columnIndexes) throws SQLException {
        return sysMon.measure(ident(sql), new AMeasureCallback<Integer, SQLException>() {
            @Override
            public Integer call(AWithParameters m) throws SQLException {
                return inner.executeUpdate(sql, columnIndexes);
            }
        });
    }

    @Override public int executeUpdate(final String sql, final String[] columnNames) throws SQLException {
        return sysMon.measure(ident(sql), new AMeasureCallback<Integer, SQLException>() {
            @Override
            public Integer call(AWithParameters m) throws SQLException {
                return inner.executeUpdate(sql, columnNames);
            }
        });
    }

    @Override public boolean execute(final String sql, final int autoGeneratedKeys) throws SQLException {
        return sysMon.measure(ident(sql), new AMeasureCallback<Boolean, SQLException>() {
            @Override
            public Boolean call(AWithParameters m) throws SQLException {
                return inner.execute(sql, autoGeneratedKeys);
            }
        });
    }

    @Override public boolean execute(final String sql, final int[] columnIndexes) throws SQLException {
        return sysMon.measure(ident(sql), new AMeasureCallback<Boolean, SQLException>() {
            @Override
            public Boolean call(AWithParameters m) throws SQLException {
                return inner.execute(sql, columnIndexes);
            }
        });
    }

    @Override public boolean execute(final String sql, final String[] columnNames) throws SQLException {
        return sysMon.measure(ident(sql), new AMeasureCallback<Boolean, SQLException>() {
            @Override
            public Boolean call(AWithParameters m) throws SQLException {
                return inner.execute(sql, columnNames);
            }
        });
    }

    protected ResultSet wrap(ResultSet rs, ACollectingMeasurement m) {
        return new ASysMonResultSet(rs, this, m);
    }

    //------------------------- would be nice to support, but I do not fully understand the implications

    @Override
    public void addBatch(String sql) throws SQLException {
        inner.addBatch(sql);
    }

    @Override
    public void clearBatch() throws SQLException {
        inner.clearBatch();
    }

    @Override public int[] executeBatch() throws SQLException {
        return inner.executeBatch();
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        return inner.getResultSet();
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return inner.getUpdateCount();
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return inner.getMoreResults();
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return inner.getMoreResults(current);
    }


    //------------------------- ignored by ASysMon

    @Override
    public void close() throws SQLException {
        inner.close();
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        return inner.getMaxFieldSize();
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        inner.setMaxFieldSize(max);
    }

    @Override
    public int getMaxRows() throws SQLException {
        return inner.getMaxRows();
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        inner.setMaxRows(max);
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        inner.setEscapeProcessing(enable);
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return inner.getQueryTimeout();
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        inner.setQueryTimeout(seconds);
    }

    @Override
    public void cancel() throws SQLException {
        inner.cancel();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return inner.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        inner.clearWarnings();
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        inner.setCursorName(name);
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        inner.setFetchDirection(direction);
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return inner.getFetchDirection();
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        inner.setFetchSize(rows);
    }

    @Override
    public int getFetchSize() throws SQLException {
        return inner.getFetchSize();
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return inner.getResultSetConcurrency();
    }

    @Override
    public int getResultSetType() throws SQLException {
        return inner.getResultSetType();
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return inner.getGeneratedKeys();
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return inner.getResultSetHoldability();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return inner.isClosed();
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        inner.setPoolable(poolable);
    }

    @Override
    public boolean isPoolable() throws SQLException {
        return inner.isPoolable();
    }

    // introduced with JDK 1.7

    public void closeOnCompletion() throws SQLException {
        this.closeOnCompletion = true;
        inner.closeOnCompletion();
    }

    public boolean isCloseOnCompletion() throws SQLException {
        return inner.isCloseOnCompletion();
    }
}
