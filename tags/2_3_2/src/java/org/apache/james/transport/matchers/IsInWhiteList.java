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

package org.apache.james.transport.matchers;

import org.apache.mailet.*;

import org.apache.avalon.cornerstone.services.datasources.*;
import org.apache.avalon.excalibur.datasource.*;
import org.apache.avalon.framework.service.*;

import org.apache.james.*;
import org.apache.james.core.*;
import org.apache.james.services.*;
import org.apache.james.util.*;

import javax.mail.*;
import javax.mail.internet.*;

import java.util.Collection;
import java.util.StringTokenizer;

import java.sql.*;
import java.util.*;
import java.text.*;
import java.io.*;

/**
 * <P>Matches recipients having the mail sender in the recipient's private whitelist .</P>
 * <P> The recipient name is always converted to its primary name (handling aliases).</P>
 * <P>Configuration string: The database name containing the white list table.</P>
 * <P>Example:</P>
 * <PRE><CODE>
 *    &lt;mailet match="IsInWhiteList=db://maildb" class="ToProcessor"&gt;
 *       &lt;processor&gt; transport &lt;/processor&gt;
 *    &lt;/mailet&gt;
 * </CODE></PRE>
 * @see org.apache.james.transport.mailets.WhiteListManager
 * @version SVN $Revision: $ $Date: $
 * @since 2.3.0
 */
public class IsInWhiteList extends GenericMatcher {

    private String selectByPK;
    
    private DataSourceComponent datasource;
    
    /** The store containing the local user repository. */
    private UsersStore usersStore;

    /** The user repository for this mail server.  Contains all the users with inboxes
     * on this server.
     */
    private UsersRepository localusers;

    /**
     * The JDBCUtil helper class
     */
    private final JDBCUtil theJDBCUtil = new JDBCUtil() {
        protected void delegatedLog(String logString) {
            log("IsInWhiteList: " + logString);
        }
    };
    
    /**
     * Contains all of the sql strings for this component.
     */
    private SqlResources sqlQueries = new SqlResources();

    /**
     * Holds value of property sqlFile.
     */
    private File sqlFile;

     /**
     * Holds value of property sqlParameters.
     */
    private Map sqlParameters = new HashMap();

    /**
     * Getter for property sqlParameters.
     * @return Value of property sqlParameters.
     */
    private Map getSqlParameters() {

        return this.sqlParameters;
    }

    /**
     * Setter for property sqlParameters.
     * @param sqlParameters New value of property sqlParameters.
     */
    private void setSqlParameters(Map sqlParameters) {

        this.sqlParameters = sqlParameters;
    }

    public void init() throws javax.mail.MessagingException {
        String repositoryPath = null;
        StringTokenizer st = new StringTokenizer(getCondition(), ", \t", false);
        if (st.hasMoreTokens()) {
            repositoryPath = st.nextToken().trim();
        }
        if (repositoryPath != null) {
            log("repositoryPath: " + repositoryPath);
        }
        else {
            throw new MessagingException("repositoryPath is null");
        }

        ServiceManager serviceManager = (ServiceManager) getMailetContext().getAttribute(Constants.AVALON_COMPONENT_MANAGER);

        try {
            // Get the DataSourceSelector block
            DataSourceSelector datasources = (DataSourceSelector) serviceManager.lookup(DataSourceSelector.ROLE);
            // Get the data-source required.
            int stindex =   repositoryPath.indexOf("://") + 3;
            String datasourceName = repositoryPath.substring(stindex);
            datasource = (DataSourceComponent) datasources.select(datasourceName);
        } catch (Exception e) {
            throw new MessagingException("Can't get datasource", e);
        }

         try {
            // Get the UsersRepository
            usersStore = (UsersStore)serviceManager.lookup(UsersStore.ROLE);
            localusers = (UsersRepository)usersStore.getRepository("LocalUsers");
        } catch (Exception e) {
            throw new MessagingException("Can't get the local users repository", e);
        }

        try {
            initSqlQueries(datasource.getConnection(), getMailetContext());
        } catch (Exception e) {
            throw new MessagingException("Exception initializing queries", e);
        }        
        
        selectByPK = sqlQueries.getSqlString("selectByPK", true);
    }

    public Collection match(Mail mail) throws MessagingException {
        // check if it's a local sender
        MailAddress senderMailAddress = mail.getSender();
        if (senderMailAddress == null) {
            return null;
        }
        String senderUser = senderMailAddress.getUser();
        String senderHost = senderMailAddress.getHost();
        if (   getMailetContext().isLocalServer(senderHost)
            && getMailetContext().isLocalUser(senderUser)) {
            // is a local sender, so return
            return null;
        }
        
        senderUser = senderUser.toLowerCase(Locale.US);
        senderHost = senderHost.toLowerCase(Locale.US);
        
        Collection recipients = mail.getRecipients();
                
        Collection inWhiteList = new java.util.HashSet();
        
        Connection conn = null;
        PreparedStatement selectStmt = null;
        ResultSet selectRS = null;
        try {
            
            for (Iterator i = recipients.iterator(); i.hasNext(); ) {
                try {
                    MailAddress recipientMailAddress = (MailAddress)i.next();
                    String recipientUser = recipientMailAddress.getUser().toLowerCase(Locale.US);
                    String recipientHost = recipientMailAddress.getHost().toLowerCase(Locale.US);
                    
                    if (!getMailetContext().isLocalServer(recipientHost)) {
                        // not a local recipient, so skip
                        continue;
                    }
                    
                    recipientUser = getPrimaryName(recipientUser);
                    
                    if (conn == null) {
                        conn = datasource.getConnection();
                    }
                    
                    if (selectStmt == null) {
                        selectStmt = conn.prepareStatement(selectByPK);
                    }
                    selectStmt.setString(1, recipientUser);
                    selectStmt.setString(2, recipientHost);
                    selectStmt.setString(3, senderUser);
                    selectStmt.setString(4, senderHost);
                    selectRS = selectStmt.executeQuery();
                    if (selectRS.next()) {
                        //This address was already in the list
                        inWhiteList.add(recipientMailAddress);
                    }
                                        
                } finally {
                    theJDBCUtil.closeJDBCResultSet(selectRS);
                }
                
            }
            
            return inWhiteList;
            
        } catch (SQLException sqle) {
            log("Error accessing database", sqle);
            throw new MessagingException("Exception thrown", sqle);
        } finally {
            theJDBCUtil.closeJDBCStatement(selectStmt);
            theJDBCUtil.closeJDBCConnection(conn);
        }
    }

    /** Gets the main name of a local customer, handling alias */
    private String getPrimaryName(String originalUsername) {
        String username;
        try {
            username = localusers.getRealName(originalUsername);
            JamesUser user = (JamesUser) localusers.getUserByName(username);
            if (user.getAliasing()) {
                username = user.getAlias();
            }
        }
        catch (Exception e) {
            username = originalUsername;
        }
        return username;
    }
    
    /**
     * Initializes the sql query environment from the SqlResources file.
     * Will look for conf/sqlResources.xml.
     * Will <I>not</I> create the database resources, if missing
     * (this task is done, if needed, in the {@link WhiteListManager}
     * initialization routine).
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
            sqlQueries.init(this.sqlFile, "WhiteList" , conn, getSqlParameters());
            
        } finally {
            theJDBCUtil.closeJDBCConnection(conn);
        }
    }
    
}
