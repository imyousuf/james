/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.transport.mailets;

import java.io.*;
import java.sql.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.mailet.*;

import org.apache.avalon.cornerstone.services.datasource.DataSourceSelector;
import org.apache.avalon.excalibur.datasource.DataSourceComponent;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.phoenix.BlockContext;

import org.apache.james.Constants;
import org.apache.james.util.SqlResources;

/**
 * Rewrites recipient addresses based on a database table.  The connection
 * is configured by passing the URL to a conn definition.  You need to set
 * the table name to check (or view) along with the source and target columns
 * to use.  For example,
 * <mailet match="All" class="JDBCAlias">
 *   <mappings>db://maildb/Aliases</mappings>
 * </mailet>
 *
 * @author  Serge Knystautas <sergek@lokitech.com>
 */
public class JDBCAlias extends GenericMailet implements Contextualizable {

    protected DataSourceComponent datasource;
    protected Context context;

    // Contains all of the sql strings for this component.
    protected SqlResources sqlQueries;

    public void contextualize(final Context context) throws ContextException {
        this.context = context;
    }

    public void init() throws MessagingException {
        String mappingsURL = getInitParameter("mappings");
        String sqlFileName = getInitParameter("sqlFile");

        String datasourceName = mappingsURL.substring(5);
        int pos = datasourceName.indexOf("/");
        String tableName = datasourceName.substring(pos + 1);
        datasourceName = datasourceName.substring(0, pos);


        Connection conn = null;
        try {
            ComponentManager componentManager = (ComponentManager)getMailetContext().getAttribute(Constants.AVALON_COMPONENT_MANAGER);
            // Get the DataSourceSelector block
            DataSourceSelector datasources = (DataSourceSelector)componentManager.lookup(DataSourceSelector.ROLE);
            // Get the data-source required.
            datasource = (DataSourceComponent)datasources.select(datasourceName);

            // Initialise the sql strings.
            String fileName = sqlFileName.substring("file://".length());
            fileName = ((BlockContext)context).getBaseDirectory() + File.separator + fileName;
            File sqlFile = (new File(fileName)).getCanonicalFile();

            String resourceName = "org.apache.james.mailrepository.JDBCAlias";

            log("Reading SQL resources from file: " +
                              sqlFile.getAbsolutePath() + ", section " +
                              this.getClass().getName() + ".");

            // Build the statement parameters
            Map sqlParameters = new HashMap();
            if (tableName != null) {
                sqlParameters.put("table", tableName);
            }

            sqlQueries = new SqlResources();
            sqlQueries.init(sqlFile, this.getClass().getName(),
                            conn, sqlParameters);

            // Check if the required table exists. If not, create it.
            DatabaseMetaData dbMetaData = conn.getMetaData();
            // Need to ask in the case that identifiers are stored, ask the DatabaseMetaInfo.
            // Try UPPER, lower, and MixedCase, to see if the table is there.
            if (! ( tableExists(dbMetaData, tableName) ||
                    tableExists(dbMetaData, tableName.toUpperCase()) ||
                    tableExists(dbMetaData, tableName.toLowerCase()) ))  {
                // Users table doesn't exist - create it.
                PreparedStatement createStatement =
                    conn.prepareStatement(sqlQueries.getSqlString("createTable", true));
                createStatement.execute();
                createStatement.close();

                log("JdbcMailRepository: Created table \'" +
                                 tableName + "\'.");
            }
        } catch (MessagingException me) {
            throw me;
        } catch (Exception e) {
            throw new MessagingException("An exception occurred while configuring JDBCAlias.", e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException sqle) {
                    //ignore
                }
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
            conn = getConnection();

            for (Iterator i = recipients.iterator(); i.hasNext(); ) {
                try {
                    MailAddress source = (MailAddress)i.next();
                    mappingStmt = conn.prepareStatement(sqlQueries.getSqlString("select", true));

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
                    mappingStmt.close();
                }
            }
        } catch (SQLException sqle) {
            throw new MessagingException("Error accessing database", sqle);
        } finally {
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
     * Opens a database connection.
     */
    private Connection getConnection() throws MessagingException {
        try {
            return datasource.getConnection();
        } catch (SQLException sqle) {
            throw new MessagingException (
                "An exception occurred getting a database connection.", sqle);
        }
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
