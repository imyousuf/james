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




package org.apache.james.smtpserver.core.filter.fastfail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.avalon.cornerstone.services.datasources.DataSourceSelector;
import org.apache.avalon.excalibur.datasource.DataSourceComponent;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.smtpserver.CommandHandler;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.util.JDBCUtil;
import org.apache.james.util.mail.dsn.DSNStatus;
import org.apache.mailet.MailAddress;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;

/**
 * Handler which reject invalid recipients
 */
public class ValidRcptHandler extends AbstractLogEnabled implements CommandHandler, Configurable,Serviceable,Initializable{
    
    private Collection recipients = new ArrayList();
    private Collection domains = new ArrayList();
    private Collection regex = new ArrayList();
    private String repositoryPath;
    private DataSourceComponent dataSource;
    private DataSourceSelector selector;
    private boolean useVirtualUserTable = false;
    private boolean useSql = false;
    private String query = "select VirtualUserTable.target_address from VirtualUserTable, VirtualUserTable as VUTDomains where (VirtualUserTable.user like ? or VirtualUserTable.user like '\\%') and (VirtualUserTable.domain like ? or (VirtualUserTable.domain like '\\%' and VUTDomains.domain like ?)) order by concat(VirtualUserTable.user,'@',VirtualUserTable.domain) desc limit 1";
    
    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration arg0) throws ConfigurationException {
        Configuration recipientsConfig = arg0.getChild("validRecipients");
        if (recipientsConfig != null) {
            setValidRecipients(recipientsConfig.getValue());
        }
        
        Configuration domainConfig = arg0.getChild("validDomains");        
        if (domainConfig != null) {
            setValidDomains(domainConfig.getValue());
        }
        
        Configuration regexConfig = arg0.getChild("validRegexPattern");        
        if (regexConfig != null) {
            try {
                setValidRegex(regexConfig.getValue());
            } catch(MalformedPatternException mpe) {
                throw new ConfigurationException("Malformed pattern: ", mpe);
            }
        }
    
        Configuration virtualUserTableConfig = arg0.getChild("useVirtualUserTable", false);
        if (virtualUserTableConfig != null) {
            useVirtualUserTable = virtualUserTableConfig.getValueAsBoolean(false);
        }
          
    
        Configuration configRepositoryPath = arg0.getChild("repositoryPath", false);
        if (configRepositoryPath != null) {
            repositoryPath = configRepositoryPath.getValue();
            useSql = true;
            
            Configuration configQuery = arg0.getChild("query", false);
            if (configQuery != null) {
                this.query = configQuery.getValue();
            } 
        }  
    }
    
    /**
     * Set the valid recipients. 
     * 
     * @param recip The valid recipients. Commaseperated list
     */
    public void setValidRecipients(String recip) {
        StringTokenizer st = new StringTokenizer(recip, ", ", false);
        
        while (st.hasMoreTokens()) {
            String recipient = st.nextToken().toLowerCase();
            
            getLogger().debug("Add recipient to valid recipients: " + recipient);
            recipients.add(recipient);
        }
    }

    /**
     * Set the valid domains. 
     * 
     * @param recip The valid domains. Commaseperated list
     */
    public void setValidDomains(String dom) {
        StringTokenizer st = new StringTokenizer(dom, ", ", false);
        
        while (st.hasMoreTokens()) {
            String domain = st.nextToken().toLowerCase();
            getLogger().debug("Add domain to valid domains: " + domain);
            domains.add(domain);
        }  
    }
    
    /**
     * 
     * @param reg 
     * @throws MalformedPatternException
     */
    public void setValidRegex(String reg) throws MalformedPatternException {
        Perl5Compiler compiler = new Perl5Compiler();
        
        StringTokenizer st = new StringTokenizer(reg, ", ", false);
        
        while (st.hasMoreTokens()) {
            String patternString = st.nextToken().trim();

            getLogger().debug("Add regex to valid regex: " + patternString);
            
            Pattern pattern = compiler.compile(patternString, Perl5Compiler.READ_ONLY_MASK);
            regex.add(pattern);

        }  
    }
    
    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager)
     */
    public void service(ServiceManager arg0) throws ServiceException {
        selector = (DataSourceSelector) arg0.lookup(DataSourceSelector.ROLE);
    }

    /**
     * @see org.apache.james.smtpserver.CommandHandler#getImplCommands()
     */
    public Collection getImplCommands() {
        Collection c = new ArrayList();
        c.add("RCPT");
            
        return c;
    }

    /**
     * @see org.apache.james.smtpserver.CommandHandler#onCommand(SMTPSession)
     */
    public void onCommand(SMTPSession session) {
        if (!session.isRelayingAllowed() && !(session.isAuthRequired() && session.getUser() != null)) {
            checkValidRcpt(session);
        } else {
            getLogger().debug("Sender allowed to relay");
        }
    }
    
    
    
    /**
     * Check if the recipient should be accepted
     * 
     * @param session The SMTPSession
     */
    private void checkValidRcpt(SMTPSession session) {
        MailAddress rcpt = (MailAddress) session.getState().get(SMTPSession.CURRENT_RECIPIENT);
        boolean invalidUser = true;

        if (session.getConfigurationData().getUsersRepository().contains(rcpt.getUser()) == true || recipients.contains(rcpt.toString().toLowerCase()) || domains.contains(rcpt.getHost().toLowerCase())) {
            invalidUser = false;
        }
        
        if (invalidUser == true && !regex.isEmpty()) {
            Iterator reg = regex.iterator();
            Perl5Matcher matcher  = new Perl5Matcher();
            
            while (reg.hasNext()) {
                if (matcher.matches(rcpt.toString(), (Pattern) reg.next())) {
                    // regex match
                    invalidUser = false;
                    break;
                }
            }
        }
    
        if (useSql == true && invalidUser == true) {
            invalidUser = userExist(rcpt,useVirtualUserTable);
        }
    
        if (invalidUser == true) {
            //user not exist
            String responseString = "554 " + DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.ADDRESS_MAILBOX) + " Unknown user: " + rcpt.toString();
        
            getLogger().info("Rejected message. Unknown user: " + rcpt.toString());
        
            session.writeResponse(responseString);
            session.setStopHandlerProcessing(true);
        }
    }
    
    /**
     * Return true if the MailAddress exists in the table
     * 
     * @param rcpt The MailAddress to check
     * @return true if the MailAddress exists in the table. If not false
     */
    private boolean userExist(MailAddress rcpt, boolean useVirtualUserTable) {
        Connection conn = null;
        PreparedStatement mappingStmt = null;
        try {
            conn = dataSource.getConnection();
            mappingStmt = conn.prepareStatement(query);
                ResultSet mappingRS = null;
                try {
                    if (useVirtualUserTable == true) {
                        mappingStmt.setString(1, rcpt.getUser());
                        mappingStmt.setString(2, rcpt.getHost());
                        mappingStmt.setString(3, rcpt.getHost());
                    } else {
                        mappingStmt.setString(1, rcpt.toString());
                    }
                    mappingRS = mappingStmt.executeQuery();
                    
                    if (mappingRS.next()) {
                       return true;
                    }
                } finally {
                    theJDBCUtil.closeJDBCResultSet(mappingRS);
                }
            
        } catch (SQLException sqle) {
            getLogger().error("Error accessing database", sqle);
            
            // safety first
            return true;
        } finally {
            theJDBCUtil.closeJDBCStatement(mappingStmt);
            theJDBCUtil.closeJDBCConnection(conn);
        }
        return false;
    
    }
    
    /**
     * The JDBCUtil helper class
     */
    private final JDBCUtil theJDBCUtil = new JDBCUtil() {
        protected void delegatedLog(String logString) {
            getLogger().debug("ValidRcpt: " + logString);
        }
    };
 
    /**
     * Return the DataSourceSelector 
     * 
     * @param datasources The DataSourceSelector
     * @param repositoryPath The repositoryPath
     * @return true or false 
     * 
     * @throws ServiceException
     */
    private DataSourceComponent getDataSource(DataSourceSelector datasources,String repositoryPath) throws ServiceException {

        int stindex = repositoryPath.indexOf("://") + 3;
        String datasourceName = repositoryPath.substring(stindex);

        return (DataSourceComponent) datasources.select(datasourceName);
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {
        if (useSql == true) {
            // init the datasource 
            dataSource = getDataSource(selector,repositoryPath);
            getLogger().debug("SQL is used");
        }
    }
}
