/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.transport.mailets;

import org.apache.avalon.cornerstone.services.datasource.DataSourceSelector;
import org.apache.avalon.excalibur.datasource.DataSourceComponent;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.james.Constants;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetException;

import javax.mail.MessagingException;
import javax.mail.internet.ParseException;
import java.sql.*;
import java.util.Collection;
import java.util.Vector;

/**
 * Rewrites recipient addresses based on a database table.  The connection
 * is configured by passing the URL to a conn definition.  You need to set
 * the table name to check (or view) along with the source and target columns
 * to use.  For example,
 * &lt;mailet match="All" class="JDBCAlias"&gt;
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
 * @author  Serge Knystautas <sergek@lokitech.com>
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
            ComponentManager componentManager = (ComponentManager)getMailetContext().getAttribute(Constants.AVALON_COMPONENT_MANAGER);
            // Get the DataSourceSelector block
            DataSourceSelector datasources = (DataSourceSelector)componentManager.lookup(DataSourceSelector.ROLE);
            // Get the data-source required.
            datasource = (DataSourceComponent)datasources.select(datasourceName);

            conn = datasource.getConnection();

            // Check if the required listserv table exists. If not, complain.
            DatabaseMetaData dbMetaData = conn.getMetaData();
            // Need to ask in the case that identifiers are stored, ask the DatabaseMetaInfo.
            // Try UPPER, lower, and MixedCase, to see if the table is there.
            if (! ( tableExists(dbMetaData, listservTable) ||
                    tableExists(dbMetaData, listservTable.toUpperCase()) ||
                    tableExists(dbMetaData, listservTable.toLowerCase()) ))  {
                throw new MailetException("Could not find table '" + listservTable + "' in datasource '" + datasourceName + "'");
            }

            // Check if the required members table exists. If not, complain.
            // Need to ask in the case that identifiers are stored, ask the DatabaseMetaInfo.
            // Try UPPER, lower, and MixedCase, to see if the table is there.
            if (! ( tableExists(dbMetaData, membersTable) ||
                    tableExists(dbMetaData, membersTable.toUpperCase()) ||
                    tableExists(dbMetaData, membersTable.toLowerCase()) ))  {
                throw new MailetException("Could not find table '" + membersTable + "' in datasource '" + datasourceName + "'");
            }

            listservQuery = "SELECT members_only, attachments_allowed, reply_to_list, subject_prefix, list_address FROM "
                    + listservTable + " WHERE listserv_id = ?";
            membersQuery = "SELECT member FROM " + membersTable + " WHERE listserv_id = ?";

            //Always load settings at least once... if we aren't caching, we will load at each getMembers() call
            loadSettings();
        } catch (MailetException me) {
            throw me;
        } catch (Exception e) {
            throw new MessagingException("Error initializing JDBCAlias", e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException sqle) {
                //ignore
            }
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

    protected void loadSettings() throws MessagingException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            //Load members
            conn = datasource.getConnection();
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
                    log("error parsing address '" + address + "' in listserv '" + listservID + "'");
                }
            }
            members = tmpMembers;
            rs.close();
            stmt.close();

            stmt = conn.prepareStatement(listservQuery);
            stmt.setString(1, listservID);
            rs = stmt.executeQuery();
            if (!rs.next()) {
                throw new MailetException("Could not find listserv record for '" + listservID + "'");
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
                    log("invalid listserv address '" + listservAddress + "' for listserv '" + listservID + "'");
                    listservAddress = null;
                }
            }
            rs.close();
            stmt.close();
        } catch (SQLException sqle) {
            throw new MailetException("Problem loading settings", sqle);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException sqle) {
                //ignore
            }
        }
    }

    public String getMailetInfo() {
        return "JDBC listserv mailet";
    }

    /**
     * Checks database metadata to see if a table exists.
     */
    private boolean tableExists(DatabaseMetaData dbMetaData, String tableName)
            throws SQLException {
        ResultSet rsTables = dbMetaData.getTables(null, null, tableName, null);
        boolean found = rsTables.next();
        rsTables.close();
        return found;
    }
}
