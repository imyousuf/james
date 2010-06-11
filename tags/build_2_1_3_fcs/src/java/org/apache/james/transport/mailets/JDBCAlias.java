/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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

    // The JDBCUtil helper class
    private final JDBCUtil theJDBCUtil =
            new JDBCUtil() {
                protected void delegatedLog(String logString) {
                    log("JDBCAlias: " + logString);
                }
            };

    /**
     * Initialize the mailet
     */
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
            // Get the DataSourceSelector service
            DataSourceSelector datasources = (DataSourceSelector)componentManager.lookup(DataSourceSelector.ROLE);
            // Get the data-source required.
            datasource = (DataSourceComponent)datasources.select(datasourceName);

            conn = datasource.getConnection();

            // Check if the required table exists. If not, complain.
            DatabaseMetaData dbMetaData = conn.getMetaData();
            // Need to ask in the case that identifiers are stored, ask the DatabaseMetaInfo.
            // Try UPPER, lower, and MixedCase, to see if the table is there.
            if (!(theJDBCUtil.tableExists(dbMetaData, tableName)))  {
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
            StringBuffer queryBuffer =
                new StringBuffer(128)
                        .append("SELECT ")
                        .append(getInitParameter("target_column"))
                        .append(" FROM ")
                        .append(tableName)
                        .append(" WHERE ")
                        .append(getInitParameter("source_column"))
                        .append(" = ?");
            query = queryBuffer.toString();
        } catch (MailetException me) {
            throw me;
        } catch (Exception e) {
            throw new MessagingException("Error initializing JDBCAlias", e);
        } finally {
            theJDBCUtil.closeJDBCConnection(conn);
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
                        StringBuffer exceptionBuffer =
                            new StringBuffer(128)
                                    .append("There is an invalid alias from ")
                                    .append(source)
                                    .append(" to ")
                                    .append(mappingRS.getString(1));
                        log(exceptionBuffer.toString());
                        continue;
                    }
                } finally {
                    ResultSet localRS = mappingRS;
                    // Clear reference to result set
                    mappingRS = null;
                    theJDBCUtil.closeJDBCResultSet(localRS);
                }
            }
        } catch (SQLException sqle) {
            throw new MessagingException("Error accessing database", sqle);
        } finally {
            theJDBCUtil.closeJDBCStatement(mappingStmt);
            theJDBCUtil.closeJDBCConnection(conn);
        }

        recipients.removeAll(recipientsToRemove);
        recipients.addAll(recipientsToAdd);
    }

    /**
     * Return a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "JDBC aliasing mailet";
    }

}
