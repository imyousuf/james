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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Implements a Virtual User Table for JAMES.  Derived from the
 * JDBCAlias mailet, but whereas that mailet uses a simple map from a
 * source address to a destination address, this handles simple
 * wildcard selection, verifies that a catchall address is for a domain
 * in the Virtual User Table, and handles forwarding.
 *
 * JDBCVirtualUserTable does not provide any administation tools.
 * You'll have to create the VirtualUserTable yourself.  The standard
 * configuration is as follows:
 *
 * CREATE TABLE VirtualUserTable
 * (
 *  user varchar(64) NOT NULL default '',
 *  domain varchar(255) NOT NULL default '',
 *  target_address varchar(255) NOT NULL default '',
 *  PRIMARY KEY (user,domain)
 * );
 *
 * The user column specifies the username of the virtual recipient, the domain
 * column the domain of the virtual recipient, and the target_address column
 * the email address of the real recipient. The target_address column can contain
 * just the username in the case of a local user, and multiple recipients can be
 * specified in a list separated by commas, semi-colons or colons.
 *
 * The standard query used with VirtualUserTable is:
 *
 * select VirtualUserTable.target_address from VirtualUserTable, VirtualUserTable as VUTDomains
 * where (VirtualUserTable.user like ? or VirtualUserTable.user like "\%")
 * and (VirtualUserTable.domain like ?
 * or (VirtualUserTable.domain like "\%" and VUTDomains.domain like ?))
 * order by concat(VirtualUserTable.user,'@',VirtualUserTable.domain) desc limit 1
 *
 * For a given [user, domain, domain] used with the query, this will
 * match as follows (in precedence order):
 *
 * 1. user@domain    - explicit mapping for user@domain
 * 2. user@%         - catchall mapping for user anywhere
 * 3. %@domain       - catchall mapping for anyone at domain
 * 4. null           - no valid mapping
 *
 * You need to set the connection.  At the moment, there is a limit to
 * what you can change regarding the SQL Query, because there isn't a
 * means to specify where in the query to replace parameters. [TODO]
 *
 * &lt;mailet match="All" class="JDBCVirtualUserTable"&gt;
 *   &lt;table&gt;db://maildb/VirtualUserTable&lt;/table&gt;
 *   &lt;sqlquery&gt;sqlquery&lt;/sqlquery&gt;
 * &lt;/mailet&gt;
 */
public class JDBCVirtualUserTable extends AbstractVirtualUserTable
{
    protected DataSourceComponent datasource;

    /**
     * The query used by the mailet to get the alias mapping
     */
    protected String query = null;

    /**
     * The JDBCUtil helper class
     */
    private final JDBCUtil theJDBCUtil = new JDBCUtil() {
        protected void delegatedLog(String logString) {
            log("JDBCVirtualUserTable: " + logString);
        }
    };

    /**
     * Initialize the mailet
     */
    public void init() throws MessagingException {
        if (getInitParameter("table") == null) {
            throw new MailetException("Table location not specified for JDBCVirtualUserTable");
        }

        String tableURL = getInitParameter("table");

        String datasourceName = tableURL.substring(5);
        int pos = datasourceName.indexOf("/");
        String tableName = datasourceName.substring(pos + 1);
        datasourceName = datasourceName.substring(0, pos);
        Connection conn = null;

        try {
            ServiceManager componentManager = (ServiceManager)getMailetContext().getAttribute(Constants.AVALON_COMPONENT_MANAGER);
            // Get the DataSourceSelector service
            DataSourceSelector datasources = (DataSourceSelector)componentManager.lookup(DataSourceSelector.ROLE);
            // Get the data-source required.
            datasource = (DataSourceComponent)datasources.select(datasourceName);

            conn = datasource.getConnection();

            // Check if the required table exists. If not, complain.
            DatabaseMetaData dbMetaData = conn.getMetaData();
            // Need to ask in the case that identifiers are stored, ask the DatabaseMetaInfo.
            // Try UPPER, lower, and MixedCase, to see if the table is there.
            if (!(theJDBCUtil.tableExists(dbMetaData, tableName))) {
                StringBuffer exceptionBuffer =
                                              new StringBuffer(128)
                                              .append("Could not find table '")
                                              .append(tableName)
                                              .append("' in datasource '")
                                              .append(datasourceName)
                                              .append("'");
                throw new MailetException(exceptionBuffer.toString());
            }

            //Build the query
            query = getInitParameter("sqlquery","select VirtualUserTable.target_address from VirtualUserTable, VirtualUserTable as VUTDomains where (VirtualUserTable.user like ? or VirtualUserTable.user like '\\%') and (VirtualUserTable.domain like ? or (VirtualUserTable.domain like '\\%' and VUTDomains.domain like ?)) order by concat(VirtualUserTable.user,'@',VirtualUserTable.domain) desc limit 1");
        } catch (MailetException me) {
            throw me;
        } catch (Exception e) {
            throw new MessagingException("Error initializing JDBCVirtualUserTable", e);
        } finally {
            theJDBCUtil.closeJDBCConnection(conn);
        }
    }

    /**
     * Map any virtual recipients to real recipients using the configured
     * JDBC connection, table and query.
     * 
     * @param recipientsMap the mapping of virtual to real recipients
     */
    protected void mapRecipients(Map recipientsMap) throws MessagingException {
        Connection conn = null;
        PreparedStatement mappingStmt = null;

        Collection recipients = recipientsMap.keySet();

        try {
            conn = datasource.getConnection();
            mappingStmt = conn.prepareStatement(query);

            for (Iterator i = recipients.iterator(); i.hasNext(); ) {
                ResultSet mappingRS = null;
                try {
                    MailAddress source = (MailAddress)i.next();
                    mappingStmt.setString(1, source.getUser());
                    mappingStmt.setString(2, source.getHost());
                    mappingStmt.setString(3, source.getHost());
                    mappingRS = mappingStmt.executeQuery();
                    if (mappingRS.next()) {
                        String targetString = mappingRS.getString(1);
                        recipientsMap.put(source, targetString);
                    }
                } finally {
                    theJDBCUtil.closeJDBCResultSet(mappingRS);
                }
            }
        } catch (SQLException sqle) {
            throw new MessagingException("Error accessing database", sqle);
        } finally {
            theJDBCUtil.closeJDBCStatement(mappingStmt);
            theJDBCUtil.closeJDBCConnection(conn);
        }
    }

    public String getMailetInfo() {
        return "JDBC Virtual User Table mailet";
    }
}
