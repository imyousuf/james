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
import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetException;

import javax.mail.MessagingException;
import javax.mail.internet.ParseException;
import java.sql.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

/**
 * Rewrites recipient addresses based on a database table.  The connection
 * is configured by passing the URL to a conn definition.  You need to set
 * the table name to check (or view) along with the source and target columns
 * to use.  For example,
 * &lt;mailet match="All" class="JDBCAlias"&gt;
 *   &lt;mappings&gt;db://maildb/Aliases&lt;/mappings&gt;
 *   &lt;source_column&gt;source_email_address&lt;/source_column&gt;
 *   &lt;target_column&gt;target_email_address&lt;/target_column&gt;
 * &lt;/mailet&gt;
 *
 * @author  Serge Knystautas <sergek@lokitech.com>
 */
public class JDBCAlias extends GenericMailet {

    protected DataSourceComponent datasource;
    protected String query = null;

    public void init() throws MessagingException {
        String mappingsURL = getInitParameter("mappings");

        String datasourceName = mappingsURL.substring(5);
        int pos = datasourceName.indexOf("/");
        String tableName = datasourceName.substring(pos + 1);
        datasourceName = datasourceName.substring(0, pos);

        Connection conn = null;
        if (getInitParameter("source_column") == null) {
            throw new MailetException("source_column not specified for JDBCAlias");
        }
        if (getInitParameter("target_column") == null) {
            throw new MailetException("target_column not specified for JDBCAlias");
        }
        try {
            ComponentManager componentManager = (ComponentManager)getMailetContext().getAttribute(Constants.AVALON_COMPONENT_MANAGER);
            // Get the DataSourceSelector block
            DataSourceSelector datasources = (DataSourceSelector)componentManager.lookup(DataSourceSelector.ROLE);
            // Get the data-source required.
            datasource = (DataSourceComponent)datasources.select(datasourceName);

            conn = datasource.getConnection();

            // Check if the required table exists. If not, complain.
            DatabaseMetaData dbMetaData = conn.getMetaData();
            // Need to ask in the case that identifiers are stored, ask the DatabaseMetaInfo.
            // Try UPPER, lower, and MixedCase, to see if the table is there.
            if (! ( tableExists(dbMetaData, tableName) ||
                    tableExists(dbMetaData, tableName.toUpperCase()) ||
                    tableExists(dbMetaData, tableName.toLowerCase()) ))  {
                throw new MailetException("Could not find table '" + tableName + "' in datasource '" + datasourceName + "'");
            }

            //Build the query
            query = "SELECT " + getInitParameter("target_column")
                    + " FROM " + tableName + " WHERE "
                    + getInitParameter("source_column") + " = ?";
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

    public void service(Mail mail) throws MessagingException {
        //Then loop through each address in the recipient list and try to map it according to the alias table

        Connection conn = null;
        PreparedStatement mappingStmt = null;
        ResultSet mappingRS = null;

        Collection recipients = mail.getRecipients();
        Collection recipientsToRemove = new Vector();
        Collection recipientsToAdd = new Vector();
        try {
            conn = datasource.getConnection();
            mappingStmt = conn.prepareStatement(query);


            for (Iterator i = recipients.iterator(); i.hasNext(); ) {
                try {
                    MailAddress source = (MailAddress)i.next();
                    mappingStmt.setString(1, source.toString());
                    mappingRS = mappingStmt.executeQuery();
                    if (!mappingRS.next()) {
                        //This address was not found
                        continue;
                    }
                    try {
                        String targetString = mappingRS.getString(1);
                        MailAddress target = new MailAddress(targetString);

                        //Mark this source address as an address to remove from the recipient list
                        recipientsToRemove.add(source);
                        recipientsToAdd.add(target);
                    } catch (ParseException pe) {
                        //Don't alias this address... there's an invalid address mapping here
                        log("There is an invalid alias from " + source + " to " + mappingRS.getString(1));
                        continue;
                    }
                } finally {
                    mappingRS.close();
                }
            }
        } catch (SQLException sqle) {
            throw new MessagingException("Error accessing database", sqle);
        } finally {
            try {
                mappingStmt.close();
            } catch (Exception e) {
                //ignore
            }
            try {
                conn.close();
            } catch (Exception e) {
                //ignore
            }
        }

        recipients.removeAll(recipientsToRemove);
        recipients.addAll(recipientsToAdd);
    }

    public String getMailetInfo() {
        return "JDBC aliasing mailet";
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
