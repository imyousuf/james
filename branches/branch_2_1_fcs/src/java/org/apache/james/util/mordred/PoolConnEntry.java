/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.util.mordred;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.*;
import java.util.Map;

/**
 * An entry in a connection pool.
 *
 * @author Serge Knystautas <sergek@lokitech.com>
 */
public class PoolConnEntry implements java.sql.Connection{
    private static final boolean DEEP_DEBUG = false;

    // States for connections (in use, being tested, or active)
    public final static int     AVAILABLE = 0;
    public final static int     ACTIVE = 1;

    private JdbcDataSource      container;

    private Connection          connection;
    private int                 status;
    private long                lockTime;
    private long                createDate;
    private long                lastActivity;
    private int                 id;
    private java.lang.Throwable trace;

    /**
     * Insert the method's description here.
     * Creation date: (8/24/99 11:43:45 AM)
     * @param conn java.sql.Connection
     */
    public PoolConnEntry(JdbcDataSource container, Connection conn, int id) {
        this.container = container;
        this.connection = conn;
        status = AVAILABLE;
        createDate = System.currentTimeMillis();
        lastActivity = System.currentTimeMillis();
        this.id = id;
    }

    /**
     * Locks an entry for anybody else using it
     */
    public synchronized boolean lock() throws SQLException {
        if (DEEP_DEBUG) {
            System.out.println("Trying to lock");
        }

        if (status != PoolConnEntry.AVAILABLE) {
            return false;
        }

        if (DEEP_DEBUG) {
            System.out.println("Available");
        }

        if (false) {
            //There really is no sense in doing this...
            //  maybe make it a conf option at some point, but really slows
            //  down the pooling.
            if (connection.isClosed()) {
                throw new SQLException("Connection has been closed.");
            }

            if (DEEP_DEBUG) {
                System.out.println("not closed");
            }
        }


        status = PoolConnEntry.ACTIVE;
        lockTime = System.currentTimeMillis();
        lastActivity = lockTime;
        trace = new Throwable();
        clearWarnings();
        if (DEEP_DEBUG) {
            System.out.println("Returning");
        }
        return true;
    }

    /**
     * Resets flags on an entry for reuse in the pool
     */
    public synchronized void unlock() {
        lastActivity = System.currentTimeMillis();
        trace = null;
        status = AVAILABLE;
    }

    /**
     * Simple method to log any warnings on an entry (connection), and
     * then clear them.
     * @throws java.sql.SQLException
     */
    public void clearWarnings() {
        try {
            SQLWarning currSQLWarning = connection.getWarnings();
            while (currSQLWarning != null) {
                StringBuffer logBuffer =
                    new StringBuffer(256)
                            .append("Warnings on connection ")
                            .append(id)
                            .append(currSQLWarning);
                container.debug(logBuffer.toString());
                currSQLWarning = currSQLWarning.getNextWarning();
            }
            connection.clearWarnings();
        } catch (SQLException sqle) {
            container.debug("Error while clearing exceptions on " + id);
            // It will probably get killed by itself before too long if this failed
        }
    }


    /**
     * Insert the method's description here.
     * Creation date: (8/24/99 11:43:19 AM)
     * @return a long representing the time this entry was created
     */
    public long getCreateDate() {
        return createDate;
    }

    /**
     * Insert the method's description here.
     * Creation date: (8/24/99 12:09:01 PM)
     * @return int
     */
    public int getId() {
        return id;
    }

    /**
     * Insert the method's description here.
     * Creation date: (8/24/99 11:43:19 AM)
     * @return long
     */
    public long getLastActivity() {
        return lastActivity;
    }

    /**
     * Insert the method's description here.
     * Creation date: (8/24/99 11:43:19 AM)
     * @return long
     */
    public long getLockTime() {
        return lockTime;
    }

    /**
     * Insert the method's description here.
     * Creation date: (8/24/99 11:43:19 AM)
     * @return int
     */
    public int getStatus() {
        return status;
    }

    /**
     * Insert the method's description here.
     * Creation date: (8/24/99 2:33:38 PM)
     * @return java.lang.Throwable
     */
    public java.lang.Throwable getTrace() {
        return trace;
    }

    /**
     * Need to clean up the connection
     */
    protected void finalize() {
        container.debug("Closing connection " + id);
        try {
            connection.close();
        } catch (SQLException ex) {
            StringBuffer warnBuffer =
                new StringBuffer(64)
                    .append("Cannot close connection ")
                    .append(id)
                    .append(" on finalize");
            container.warn(warnBuffer.toString());
        }
        // Dump the stack trace of whoever created this connection
        if (getTrace() != null) {
            StringWriter sout = new StringWriter();
            trace.printStackTrace(new PrintWriter(sout, true));
            container.info(sout.toString());
        }
    }

    public String getString() {
        StringBuffer poolConnStringBuffer =
            new StringBuffer(64)
                    .append(getId())
                    .append(": ")
                    .append(connection.toString());
        return poolConnStringBuffer.toString();
    }


    /*
     * New approach now actually has this implement a connection, as a wrapper.
     * All calls will be passed to underlying connection.
     * Except when closed is called, which will instead cause the releaseConnection on
     * the parent to be executed.
     *
     * These are the methods from java.sql.Connection
     */

    public void close() throws SQLException {
        clearWarnings();
        container.releaseConnection(this);
    }

    /**
     * Returns whether this entry is closed.
     *
     * @return whether the underlying conntection is closed
     */
    public boolean isClosed() throws SQLException {
        return connection.isClosed();
    }


    public final Statement createStatement() throws SQLException {
        return connection.createStatement();
    }

    public final PreparedStatement prepareStatement(final String sql) throws SQLException {
        return connection.prepareStatement(sql);
    }

    public final CallableStatement prepareCall(final String sql) throws SQLException {
        return connection.prepareCall(sql);
    }

    public final String nativeSQL(final String sql) throws SQLException {
        return connection.nativeSQL( sql );
    }

    public final void setAutoCommit(final boolean autoCommit) throws SQLException {
        connection.setAutoCommit( autoCommit );
    }

    public final boolean getAutoCommit() throws SQLException {
        return connection.getAutoCommit();
    }

    public final void commit() throws SQLException {
        connection.commit();
    }

    public final void rollback() throws SQLException {
        connection.rollback();
    }

    public final DatabaseMetaData getMetaData() throws SQLException {
        return connection.getMetaData();
    }

    public final void setReadOnly( final boolean readOnly ) throws SQLException {
        connection.setReadOnly( readOnly );
    }

    public final boolean isReadOnly() throws SQLException {
        return connection.isReadOnly();
    }

    public final void setCatalog( final String catalog ) throws SQLException {
        connection.setCatalog( catalog );
    }

    public final String getCatalog() throws SQLException {
        return connection.getCatalog();
    }

    public final void setTransactionIsolation( final int level ) throws SQLException {
        connection.setTransactionIsolation(level);
    }

    public final int getTransactionIsolation() throws SQLException {
        return connection.getTransactionIsolation();
    }

    public final SQLWarning getWarnings() throws SQLException {
        return connection.getWarnings();
    }

    public final Statement createStatement( final int resultSetType,
                                            final int resultSetConcurrency )
            throws SQLException {
        return connection.createStatement(resultSetType, resultSetConcurrency);
    }

    public final PreparedStatement prepareStatement( final String sql,
                                               final int resultSetType,
                                               final int resultSetConcurrency )
            throws SQLException {
        return connection.prepareStatement( sql, resultSetType, resultSetConcurrency);
    }

    public final CallableStatement prepareCall( final String sql,
                                          final int resultSetType,
                                          final int resultSetConcurrency )
            throws SQLException {
        return connection.prepareCall( sql, resultSetType, resultSetConcurrency );
    }

    public final Map getTypeMap() throws SQLException {
        return connection.getTypeMap();
    }

    public final void setTypeMap( final Map map ) throws SQLException {
        connection.setTypeMap( map );
    }

    /* JDBC_3_ANT_KEY
    public final void setHoldability(int holdability)
        throws SQLException
    {
        throw new SQLException("This is not a Jdbc 3.0 Compliant Connection");
    }

    public final int getHoldability()
        throws SQLException
    {
        throw new SQLException("This is not a Jdbc 3.0 Compliant Connection");
    }

    public final java.sql.Savepoint setSavepoint()
        throws SQLException
    {
        throw new SQLException("This is not a Jdbc 3.0 Compliant Connection");
    }

    public final java.sql.Savepoint setSavepoint(String savepoint)
        throws SQLException
    {
        throw new SQLException("This is not a Jdbc 3.0 Compliant Connection");
    }

    public final void rollback(java.sql.Savepoint savepoint)
        throws SQLException
    {
        throw new SQLException("This is not a Jdbc 3.0 Compliant Connection");
    }

    public final void releaseSavepoint(java.sql.Savepoint savepoint)
        throws SQLException
    {
        throw new SQLException("This is not a Jdbc 3.0 Compliant Connection");
    }

    public final Statement createStatement(int resulSetType,
                                           int resultSetConcurrency,
                                           int resultSetHoldability)
        throws SQLException
    {
        throw new SQLException("This is not a Jdbc 3.0 Compliant Connection");
    }

    public final PreparedStatement prepareStatement(String sql,
                                        int resulSetType,
                                        int resultSetConcurrency,
                                        int resultSetHoldability)
        throws SQLException
    {
        throw new SQLException("This is not a Jdbc 3.0 Compliant Connection");
    }

    public final CallableStatement prepareCall(String sql,
                                        int resulSetType,
                                        int resultSetConcurrency,
                                        int resultSetHoldability)
        throws SQLException
    {
        throw new SQLException("This is not a Jdbc 3.0 Compliant Connection");
    }

    public final PreparedStatement prepareStatement(String sql,
                                        int autoGeneratedKeys)
        throws SQLException
    {
        throw new SQLException("This is not a Jdbc 3.0 Compliant Connection");
    }

    public final PreparedStatement prepareStatement(String sql,
                                        int[] columnIndexes)
        throws SQLException
    {
        throw new SQLException("This is not a Jdbc 3.0 Compliant Connection");
    }

    public final PreparedStatement prepareStatement(String sql,
                                        String[] columnNames)
        throws SQLException
    {
        throw new SQLException("This is not a Jdbc 3.0 Compliant Connection");
    }
    JDBC_3_ANT_KEY */

}
