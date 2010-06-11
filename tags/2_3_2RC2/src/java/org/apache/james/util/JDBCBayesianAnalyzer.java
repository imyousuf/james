/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import java.io.File;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;

/**
 * Manages the persistence of the spam bayesian analysis corpus using a JDBC database.
 *
 * <p>This class is abstract to allow implementations to 
 * take advantage of different logging capabilities/interfaces in
 * different parts of the code.</p>

 * @version CVS $Revision: $ $Date: $
 * @since 2.3.0
 */

abstract public class JDBCBayesianAnalyzer
extends BayesianAnalyzer {
    
    /**
     *Public object representing a lock on database activity.
     */
    public final static String DATABASE_LOCK = "database lock";
    
    /**
     * An abstract method which child classes override to handle logging of
     * errors in their particular environments.
     *
     * @param errorString the error message generated
     */
    abstract protected void delegatedLog(String errorString);

    /**
     * The JDBCUtil helper class
     */
    private final JDBCUtil theJDBCUtil = new JDBCUtil() {
        protected void delegatedLog(String logString) {
            this.delegatedLog(logString);
        }
    };
    
    /**
     * Contains all of the sql strings for this component.
     */
    private SqlResources sqlQueries = new SqlResources();

    /**
     * Holds value of property sqlFileName.
     */
    private String sqlFileName;
    
    private File sqlFile;

    /**
     * Holds value of property sqlParameters.
     */
    private Map sqlParameters = new HashMap();

    /**
     * Holds value of property lastDatabaseUpdateTime.
     */
    private static long lastDatabaseUpdateTime;
    
    /**
     * Getter for property sqlFileName.
     * @return Value of property sqlFileName.
     */
    public String getSqlFileName() {

        return this.sqlFileName;
    }

    /**
     * Setter for property sqlFileName.
     * @param sqlFileName New value of property sqlFileName.
     */
    public void setSqlFileName(String sqlFileName) {

        this.sqlFileName = sqlFileName;
    }

    /**
     * Getter for property sqlParameters.
     * @return Value of property sqlParameters.
     */
    public Map getSqlParameters() {

        return this.sqlParameters;
    }

    /**
     * Setter for property sqlParameters.
     * @param sqlParameters New value of property sqlParameters.
     */
    public void setSqlParameters(Map sqlParameters) {

        this.sqlParameters = sqlParameters;
    }

    /**
     * Getter for static lastDatabaseUpdateTime.
     * @return Value of property lastDatabaseUpdateTime.
     */
    public static long getLastDatabaseUpdateTime() {

        return lastDatabaseUpdateTime;
    }

    /**
     * Sets static lastDatabaseUpdateTime to System.currentTimeMillis().
     */
    public static void touchLastDatabaseUpdateTime() {

        lastDatabaseUpdateTime = System.currentTimeMillis();
    }

    /**
     * Default constructor.
     */
    public JDBCBayesianAnalyzer() {
    }
        
    /**
     * Loads the token frequencies from the database.
     * @param conn The connection for accessing the database
     * @throws SQLException If a database error occurs
     */
    public void loadHamNSpam(Connection conn)
    throws java.sql.SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            pstmt = conn.prepareStatement(sqlQueries.getSqlString("selectHamTokens", true));
            rs = pstmt.executeQuery();
            
            Map ham = getHamTokenCounts();
            while (rs.next()) {
                String token = rs.getString(1);
                int count = rs.getInt(2);
                // to reduce memory, use the token only if the count is > 1
                if (count > 1) {
                    ham.put(token, new Integer(count));
                }
            }
            //Verbose.
            delegatedLog("Ham tokens count: " + ham.size());
            
            rs.close();
            pstmt.close();
                        
            //Get the spam tokens/counts.
            pstmt = conn.prepareStatement(sqlQueries.getSqlString("selectSpamTokens", true));
            rs = pstmt.executeQuery();
            
            Map spam = getSpamTokenCounts();
            while (rs.next()) {
                String token = rs.getString(1);
                int count = rs.getInt(2);
                // to reduce memory, use the token only if the count is > 1
                if (count > 1) {
                    spam.put(token, new Integer(count));
                }
            }
            
            //Verbose.
            delegatedLog("Spam tokens count: " + spam.size());
            
            rs.close();
            pstmt.close();
                        
            //Get the ham/spam message counts.
            pstmt = conn.prepareStatement(sqlQueries.getSqlString("selectMessageCounts", true));
            rs = pstmt.executeQuery();
            if (rs.next()) {
                setHamMessageCount(rs.getInt(1));
                setSpamMessageCount(rs.getInt(2));
            }
            
            rs.close();
            pstmt.close();
            
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (java.sql.SQLException se) {
                }
                
                rs = null;
            }
            
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (java.sql.SQLException se) {
                }
                
                pstmt = null;
            }
        }
    }
    
    /**
     * Updates the database with new "ham" token frequencies.
     * @param conn The connection for accessing the database
     * @throws SQLException If a database error occurs
     */
    public void updateHamTokens(Connection conn)
    throws java.sql.SQLException {
        updateTokens(conn, getHamTokenCounts(),
                sqlQueries.getSqlString("insertHamToken", true),
                sqlQueries.getSqlString("updateHamToken", true));
        
        setMessageCount(conn, sqlQueries.getSqlString("updateHamMessageCounts", true), getHamMessageCount());
    }
    
    /**
     * Updates the database with new "spam" token frequencies.
     * @param conn The connection for accessing the database
     * @throws SQLException If a database error occurs
     */
    public void updateSpamTokens(Connection conn)
    throws java.sql.SQLException {
         updateTokens(conn, getSpamTokenCounts(),
                sqlQueries.getSqlString("insertSpamToken", true),
                sqlQueries.getSqlString("updateSpamToken", true));
       
        setMessageCount(conn, sqlQueries.getSqlString("updateSpamMessageCounts", true), getSpamMessageCount());
    }
    
    private void setMessageCount(Connection conn, String sqlStatement, int count)
    throws java.sql.SQLException {
        PreparedStatement init = null;
        PreparedStatement update = null;
        
        try {
            //set the ham/spam message counts.
            init = conn.prepareStatement(sqlQueries.getSqlString("initializeMessageCounts", true));
            update = conn.prepareStatement(sqlStatement);
            
            update.setInt(1, count);
            
            if (update.executeUpdate() == 0) {
                init.executeUpdate();
                update.executeUpdate();
            }

        } finally {
            if (init != null) {
                try {
                    init.close();
                } catch (java.sql.SQLException ignore) {
                }
            }
            if (update != null) {
                try {
                    update.close();
                } catch (java.sql.SQLException ignore) {
                }
            }
        }
    }
    
    private void updateTokens(Connection conn, Map tokens, String insertSqlStatement, String updateSqlStatement)
    throws java.sql.SQLException {
        PreparedStatement insert = null;
        PreparedStatement update = null;
        
        try {
            //Used to insert new token entries.
            insert = conn.prepareStatement(insertSqlStatement);
            
            //Used to update existing token entries.
            update = conn.prepareStatement(updateSqlStatement);
            
            Iterator i = tokens.keySet().iterator();
            while (i.hasNext()) {
                String key = (String) i.next();
                int value = ((Integer) tokens.get(key)).intValue();
                
                update.setInt(1, value);
                update.setString(2, key);
                
                //If the update affected 0 (zero) rows, then the token hasn't been
                //encountered before, and we need to add it to the corpus.
                if (update.executeUpdate() == 0) {
                    insert.setString(1, key);
                    insert.setInt(2, value);
                    
                    insert.executeUpdate();
                }
            }
        } finally {
            if (insert != null) {
                try {
                    insert.close();
                } catch (java.sql.SQLException ignore) {
                }
                
                insert = null;
            }
            
            if (update != null) {
                try {
                    update.close();
                } catch (java.sql.SQLException ignore) {
                }
                
                update = null;
            }
        }
    }
    
    /**
     * Initializes the sql query environment from the SqlResources file.
     * Will look for conf/sqlResources.xml.
     * @param conn The connection for accessing the database
     * @param mailetContext The current mailet context,
     * for finding the conf/sqlResources.xml file
     * @throws Exception If any error occurs
     */
    public void initSqlQueries(Connection conn, org.apache.mailet.MailetContext mailetContext) throws Exception {
        try {
            if (conn.getAutoCommit()) {
                conn.setAutoCommit(false);
            }
            
            this.sqlFile = new File((String) mailetContext.getAttribute("confDir"), "sqlResources.xml").getCanonicalFile();
            sqlQueries.init(this.sqlFile, JDBCBayesianAnalyzer.class.getName() , conn, getSqlParameters());
            
            checkTables(conn);
        } finally {
            theJDBCUtil.closeJDBCConnection(conn);
        }
    }
    
    private void checkTables(Connection conn) throws SQLException {
        DatabaseMetaData dbMetaData = conn.getMetaData();
        // Need to ask in the case that identifiers are stored, ask the DatabaseMetaInfo.
        // Try UPPER, lower, and MixedCase, to see if the table is there.
        
        boolean dbUpdated = false;
        
        dbUpdated = createTable(conn, "hamTableName", "createHamTable");
        
        dbUpdated = createTable(conn, "spamTableName", "createSpamTable");
        
        dbUpdated = createTable(conn, "messageCountsTableName", "createMessageCountsTable");
        
        //Commit our changes if necessary.
        if (conn != null && dbUpdated && !conn.getAutoCommit()) {
            conn.commit();
            dbUpdated = false;
        }
            
    }
    
    private boolean createTable(Connection conn, String tableNameSqlStringName, String createSqlStringName) throws SQLException {
        String tableName = sqlQueries.getSqlString(tableNameSqlStringName, true);
        
        DatabaseMetaData dbMetaData = conn.getMetaData();

        // Try UPPER, lower, and MixedCase, to see if the table is there.
        if (theJDBCUtil.tableExists(dbMetaData, tableName)) {
            return false;
        }
        
        PreparedStatement createStatement = null;
        
        try {
            createStatement =
                    conn.prepareStatement(sqlQueries.getSqlString(createSqlStringName, true));
            createStatement.execute();
            
            StringBuffer logBuffer = null;
            logBuffer =
                    new StringBuffer(64)
                    .append("Created table '")
                    .append(tableName)
                    .append("' using sqlResources string '")
                    .append(createSqlStringName)
                    .append("'.");
            delegatedLog(logBuffer.toString());
            
        } finally {
            theJDBCUtil.closeJDBCStatement(createStatement);
        }
        
        return true;
    }
    
}
