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

package org.apache.james.transport.mailets;

import org.apache.avalon.cornerstone.services.datasources.DataSourceSelector;
import org.apache.avalon.excalibur.datasource.DataSourceComponent;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.Constants;
import org.apache.james.util.JDBCUtil;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetException;

import javax.mail.MessagingException;
import javax.mail.internet.ParseException;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Vector;

/**
 * Rewrites recipient addresses based on a database table.  The connection
 * is configured by passing the URL to a conn definition.  You need to set
 * the table name to check (or view) along with the source and target columns
 * to use.  For example,
 * &lt;mailet match="All" class="JDBCListserv"&gt;
 *   &lt;data_source&gt;maildb&lt;/datasource&gt;
 *   &lt;listserv_id&gt;mylistserv&lt;/listserv_id&gt;
 *   &lt;listserv_table&gt;source_email_address&lt;/listserv_table&gt;
 *   &lt;members_table&gt;target_email_address&lt;/members_table&gt;
 * &lt;/mailet&gt;
 *
 * This mailet will cache the settings available when first initialized.  If you wish
 * it to reload for each message, add the init parameter
 * &lt;cache_settings&gt;false&lt;/cache_settings&gt;
 *
 */
public class JDBCListserv extends GenericListserv {

    protected DataSourceComponent datasource;
    protected String listservID = null;
    protected String listservTable = null;
    protected String membersTable = null;
    protected boolean cacheSettings = true;

    //Settings for this listserv
    protected Collection members = null;
    protected boolean membersOnly = true;
    protected boolean attachmentsAllowed = true;
    protected boolean replyToList = true;
    protected MailAddress listservAddress = null;
    protected String subjectPrefix = null;

    //Queries to DB
    protected String listservQuery = null;
    protected String membersQuery = null;

    /**
     * The JDBCUtil helper class
     */
    private final JDBCUtil theJDBCUtil =
            new JDBCUtil() {
                protected void delegatedLog(String logString) {
                    log("JDBCListserv: " + logString);
                }
            };

    /**
     * Initialize the mailet
     */
    public void init() throws MessagingException {
        if (getInitParameter("data_source") == null) {
            throw new MailetException("data_source not specified for JDBCListserv");
        }
        if (getInitParameter("listserv_id") == null) {
            throw new MailetException("listserv_id not specified for JDBCListserv");
        }
        if (getInitParameter("listserv_table") == null) {
            throw new MailetException("listserv_table not specified for JDBCListserv");
        }
        if (getInitParameter("members_table") == null) {
            throw new MailetException("members_table not specified for JDBCListserv");
        }

        String datasourceName = getInitParameter("data_source");
        listservID = getInitParameter("listserv_id");
        listservTable = getInitParameter("listserv_table");
        membersTable = getInitParameter("members_table");

        if (getInitParameter("cache_settings") != null) {
            try {
                cacheSettings = new Boolean(getInitParameter("cache_settings")).booleanValue();
            } catch (Exception e) {
                //ignore error
            }
        }

        Connection conn = null;

        try {
            ServiceManager componentManager = (ServiceManager)getMailetContext().getAttribute(Constants.AVALON_COMPONENT_MANAGER);
            // Get the DataSourceSelector service
            DataSourceSelector datasources = (DataSourceSelector)componentManager.lookup(DataSourceSelector.ROLE);
            // Get the data-source required.
            datasource = (DataSourceComponent)datasources.select(datasourceName);

            conn = datasource.getConnection();

            // Check if the required listserv table exists. If not, complain.
            DatabaseMetaData dbMetaData = conn.getMetaData();
            // Need to ask in the case that identifiers are stored, ask the DatabaseMetaInfo.
            // Try UPPER, lower, and MixedCase, to see if the table is there.
            if (!(theJDBCUtil.tableExists(dbMetaData, listservTable)))  {
                StringBuffer exceptionBuffer =
                    new StringBuffer(128)
                            .append("Could not find table '")
                            .append(listservTable)
                            .append("' in datasource '")
                            .append(datasourceName)
                            .append("'");
                throw new MailetException(exceptionBuffer.toString());
            }

            // Check if the required members table exists. If not, complain.
            // Need to ask in the case that identifiers are stored, ask the DatabaseMetaInfo.
            // Try UPPER, lower, and MixedCase, to see if the table is there.
            if (!( theJDBCUtil.tableExists(dbMetaData, membersTable)))  {
                StringBuffer exceptionBuffer =
                    new StringBuffer(128)
                            .append("Could not find table '")
                            .append(membersTable)
                            .append("' in datasource '")
                            .append(datasourceName)
                            .append("'");
                throw new MailetException(exceptionBuffer.toString());
            }

            StringBuffer queryBuffer =
                new StringBuffer(256)
                        .append("SELECT members_only, attachments_allowed, reply_to_list, subject_prefix, list_address FROM ")
                        .append(listservTable)
                        .append(" WHERE listserv_id = ?");
            listservQuery = queryBuffer.toString();
            queryBuffer =
                new StringBuffer(128)
                        .append("SELECT member FROM ")
                        .append(membersTable)
                        .append(" WHERE listserv_id = ?");
            membersQuery = queryBuffer.toString();

            //Always load settings at least once... if we aren't caching, we will load at each getMembers() call
            loadSettings();
        } catch (MailetException me) {
            throw me;
        } catch (Exception e) {
            throw new MessagingException("Error initializing JDBCListserv", e);
        } finally {
            theJDBCUtil.closeJDBCConnection(conn);
        }
    }

    /**
     * Returns a Collection of MailAddress objects of members to receive this email
     */
    public Collection getMembers() throws MessagingException {
        if (!cacheSettings) {
            loadSettings();
        }

        return members;
    }

    /**
     * Returns whether this list should restrict to senders only
     */
    public boolean isMembersOnly() throws MessagingException {
        return membersOnly;
    }

    /**
     * Returns whether this listserv allow attachments
     */
    public boolean isAttachmentsAllowed() throws MessagingException {
        return attachmentsAllowed;
    }

    /**
     * Returns whether listserv should add reply-to header
     *
     * @return whether listserv should add a reply-to header
     */
    public boolean isReplyToList() throws MessagingException {
        return replyToList;
    }

    /**
     * The email address that this listserv processes on.  If returns null, will use the
     * recipient of the message, which hopefully will be the correct email address assuming
     * the matcher was properly specified.
     */
    public MailAddress getListservAddress() throws MessagingException {
        return listservAddress;
    }

    /**
     * An optional subject prefix which will be surrounded by [].
     */
    public String getSubjectPrefix() throws MessagingException {
        return subjectPrefix;
    }

    /**
     * Loads the configuration settings for this mailet from the database.
     *
     * @throws MessagingException if a problem occurs while accessing the database or
     *                            the required parameters are not present
     */
    protected void loadSettings() throws MessagingException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            //Load members
            conn = datasource.getConnection();
            try {
                stmt = conn.prepareStatement(membersQuery);
                stmt.setString(1, listservID);
                rs = stmt.executeQuery();
                Collection tmpMembers = new Vector();
                while (rs.next()) {
                    String address = rs.getString(1);
                    try {
                        MailAddress mailAddress = new MailAddress(address);
                        tmpMembers.add(mailAddress);
                    } catch (ParseException pe) {
                        //don't stop... just log and continue
                        StringBuffer exceptionBuffer =
                            new StringBuffer(64)
                                    .append("error parsing address '")
                                    .append(address)
                                    .append("' in listserv '")
                                    .append(listservID)
                                    .append("'");
                        log(exceptionBuffer.toString());
                    }
                }
                members = tmpMembers;
            } finally {
                ResultSet localRS = rs;
                // Clear reference to result set
                rs = null;
                theJDBCUtil.closeJDBCResultSet(localRS);
                Statement localStmt = stmt;
                // Clear reference to statement
                stmt = null;
                theJDBCUtil.closeJDBCStatement(localStmt);
            }

            stmt = conn.prepareStatement(listservQuery);
            stmt.setString(1, listservID);
            rs = stmt.executeQuery();
            if (!rs.next()) {
                StringBuffer exceptionBuffer =
                    new StringBuffer(64)
                            .append("Could not find listserv record for '")
                            .append(listservID)
                            .append("'");
                throw new MailetException(exceptionBuffer.toString());
            }
            membersOnly = rs.getBoolean("members_only");
            attachmentsAllowed = rs.getBoolean("attachments_allowed");
            replyToList = rs.getBoolean("reply_to_list");
            subjectPrefix = rs.getString("subject_prefix");
            String address = rs.getString("list_address");
            if (address == null) {
                listservAddress = null;
            } else {
                try {
                    listservAddress = new MailAddress(address);
                } catch (ParseException pe) {
                    //log and ignore
                    StringBuffer logBuffer =
                        new StringBuffer(128)
                                .append("invalid listserv address '")
                                .append(listservAddress)
                                .append("' for listserv '")
                                .append(listservID)
                                .append("'");
                    log(logBuffer.toString());
                    listservAddress = null;
                }
            }
        } catch (SQLException sqle) {
            throw new MailetException("Problem loading settings", sqle);
        } finally {
            theJDBCUtil.closeJDBCResultSet(rs);
            theJDBCUtil.closeJDBCStatement(stmt);
            theJDBCUtil.closeJDBCConnection(conn);
        }
    }

    /**
     * Return a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "JDBC listserv mailet";
    }
}
