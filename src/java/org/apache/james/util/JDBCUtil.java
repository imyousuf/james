/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.james.util;


import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;


/**
 * <p>Helper class for managing common JDBC tasks.</p>
 *
 * <p>This class is abstract to allow implementations to
 * take advantage of different logging capabilities/interfaces in
 * different parts of the code.</p>
 *
 *
 */
public abstract class JDBCUtil {

    /**
     * An abstract method which child classes override to handle logging of
     * errors in their particular environments.
     *
     * @param errorString the error message generated
     */
    protected abstract void delegatedLog(String errorString);

    /**
     * Checks database metadata to see if a table exists.
     * Try UPPER, lower, and MixedCase, to see if the table is there.
     *
     * @param dbMetaData the database metadata to be used to look up this table
     * @param tableName the table name
     *
     * @throws SQLException if an exception is encountered while accessing the database
     */
    public boolean tableExists(DatabaseMetaData dbMetaData, String tableName)
            throws SQLException {
        return (tableExistsCaseSensitive(dbMetaData, tableName)
                || tableExistsCaseSensitive(dbMetaData, tableName.toUpperCase(Locale.US))
                || tableExistsCaseSensitive(dbMetaData, tableName.toLowerCase(Locale.US)));
    }

    /**
     * Checks database metadata to see if a table exists.  This method
     * is sensitive to the case of the provided table name.
     *
     * @param dbMetaData the database metadata to be used to look up this table
     * @param tableName the case sensitive table name
     *
     * @throws SQLException if an exception is encountered while accessing the database
     */
    public boolean tableExistsCaseSensitive(DatabaseMetaData dbMetaData, String tableName)
            throws SQLException {
        ResultSet rsTables = dbMetaData.getTables(null, null, tableName, null);

        try {
            boolean found = rsTables.next();

            return found;
        } finally {
            closeJDBCResultSet(rsTables);
        }
    }

    /**
     * Closes database connection and logs if an error
     * is encountered
     *
     * @param conn the connection to be closed
     */
    public void closeJDBCConnection(Connection conn) {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException sqle) {
            // Log exception and continue
            subclassLogWrapper("Unexpected exception while closing database connection.");
        }
    }

    /**
     * Closes database statement and logs if an error
     * is encountered
     *
     * @param stmt the statement to be closed
     */
    public void closeJDBCStatement(Statement stmt) {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException sqle) {
            // Log exception and continue
            subclassLogWrapper("Unexpected exception while closing database statement.");
        }
    }

    /**
     * Closes database result set and logs if an error
     * is encountered
     *
     * @param aResultSet the result set to be closed
     */
    public void closeJDBCResultSet(ResultSet aResultSet) {
        try {
            if (aResultSet != null) {
                aResultSet.close();
            }
        } catch (SQLException sqle) {
            // Log exception and continue
            subclassLogWrapper("Unexpected exception while closing database result set.");
        }
    }

    /**
     * Wraps the delegated call to the subclass logging method with a Throwable
     * wrapper.  All throwables generated by the subclass logging method are
     * caught and ignored.
     *
     * @param logString the raw string to be passed to the logging method implemented
     *                  by the subclass
     */
    private void subclassLogWrapper(String logString) {
        try {
            delegatedLog(logString);
        } catch (Throwable t) {// Throwables generated by the logging system are ignored
        }
    }

}
