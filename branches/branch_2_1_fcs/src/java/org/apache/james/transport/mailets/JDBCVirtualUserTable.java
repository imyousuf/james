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

package org.apache.james.transport.mailets;

import org.apache.avalon.cornerstone.services.datasource.DataSourceSelector;
import org.apache.avalon.excalibur.datasource.DataSourceComponent;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.james.Constants;
import org.apache.james.util.JDBCUtil;
import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetException;

import javax.mail.MessagingException;
import javax.mail.internet.ParseException;
import java.sql.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Vector;

/**
 * Implements a Virtual User Table for JAMES.  Derived from the
 * JDBCAlias mailet, but whereas that mailet uses a simple map from a
 * source address to a destination address, this handles simple
 * wildcard selection, verifies that a catchall address is for a domain
 * in the Virtual User Table, and handles forwarding.
 *
 * With JDBCAlias, if the destination address were remote it would be
 * subject to relay testing, even though it should be treated as a local
 * address.  JDBCVirtualUserTable incorporates JDBCAlias processing for
 * local destinations, and Forward processing for remote destinations.
 *
 * To prevent from breaking existing JDBCAlias applications, and to
 * allow for evolution of this mailet, it is released as a new mailet,
 * rather than as an update to JDBCAlias.  However, anyone using
 * JDBCAlias should be able to upgrade to JDBCVirtualUserTable.
 *
 * As with JDBCAlias, JDBCVirtualUserTable does not provide any
 * administation tools.  You'll have to create the VirtualUserTable
 * yourself.  The standard configuration is as follows:
 *
 * CREATE TABLE VirtualUserTable
 * (
 *  user varchar(64) NOT NULL default '',
 *  domain varchar(255) NOT NULL default '',
 *  target_address varchar(255) NOT NULL default '',
 *  PRIMARY KEY (user,domain)
 * );
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
 *
 * @author  Noel J. Begman <noel@devtech.com>
 */
public class JDBCVirtualUserTable extends GenericMailet
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
            ComponentManager componentManager = (ComponentManager)getMailetContext().getAttribute(Constants.AVALON_COMPONENT_MANAGER);
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
            query = getInitParameter("sqlquery");
            if (query == null) {
                query = "select VirtualUserTable.target_address from VirtualUserTable, VirtualUserTable as VUTDomains where (VirtualUserTable.user like ? or VirtualUserTable.user like '\\%') and (VirtualUserTable.domain like ? or (VirtualUserTable.domain like '\\%' and VUTDomains.domain like ?)) order by concat(VirtualUserTable.user,'@',VirtualUserTable.domain) desc limit 1";
            }
        } catch (MailetException me) {
            throw me;
        } catch (Exception e) {
            throw new MessagingException("Error initializing JDBCVirtualUserTable", e);
        } finally {
            theJDBCUtil.closeJDBCConnection(conn);
        }
    }

    /**
     * Checks the recipient list of the email for user mappings.  Maps recipients as
     * appropriate, modifying the recipient list of the mail and sends mail to any new
     * non-local recipients.
     *
     * @param mail the mail to process
     */
    public void service(Mail mail)
            throws MessagingException {
        Connection conn = null;
        PreparedStatement mappingStmt = null;

        Collection recipients = mail.getRecipients();
        Collection recipientsToRemove = new Vector();
        Collection recipientsToAddLocal = new Vector();
        Collection recipientsToAddForward = new Vector();

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
                        try {
                            String targetString = mappingRS.getString(1);
                            MailAddress target = (targetString.indexOf('@') < 0) ? new MailAddress(targetString, "localhost")
                                                                                 : new MailAddress(targetString);

                            //Mark this source address as an address to remove from the recipient list
                            recipientsToRemove.add(source);

                            //Need to separate local and remote recipients.
                            if (getMailetContext().isLocalServer(target.getHost())) {
                                recipientsToAddLocal.add(target);
                            } else {
                                recipientsToAddForward.add(target);
                            }
                        } catch (ParseException pe) {
                            //Don't map this address... there's an invalid address mapping here
                            StringBuffer exceptionBuffer =
                                new StringBuffer(128)
                                .append("There is an invalid map from ")
                                .append(source)
                                .append(" to ")
                                .append(mappingRS.getString(1));
                            log(exceptionBuffer.toString());
                            continue;
                        }
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

        // Remove mapped recipients
        recipients.removeAll(recipientsToRemove);

        // Add mapped recipients that are local
        recipients.addAll(recipientsToAddLocal);

        // Forward to mapped recipients that are remote
        if (recipientsToAddForward.size() != 0) {
            getMailetContext().sendMail(mail.getSender(), recipientsToAddForward, mail.getMessage());
        }

        // If there are no recipients left, Ghost the message
        if (recipients.size() == 0) {
            mail.setState(Mail.GHOST);
        }
    }

    public String getMailetInfo() {
        return "JDBC Virtual User Table mailet";
    }
}

