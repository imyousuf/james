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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.services.VirtualUserTable;
import org.apache.james.services.VirtualUserTableStore;
import org.apache.james.smtpserver.SMTPResponse;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.smtpserver.hook.HookResult;
import org.apache.james.smtpserver.hook.HookReturnCode;
import org.apache.james.smtpserver.hook.RcptHook;
import org.apache.james.util.mail.SMTPRetCode;
import org.apache.james.util.mail.dsn.DSNStatus;
import org.apache.james.vut.ErrorMappingException;
import org.apache.mailet.MailAddress;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;

/**
 * Handler which reject invalid recipients
 */
public class ValidRcptHandler extends AbstractLogEnabled implements RcptHook, Configurable, Serviceable {
    
    private Collection recipients = new ArrayList();
    private Collection domains = new ArrayList();
    private Collection regex = new ArrayList();
    private boolean vut = true;
    private VirtualUserTable table;
    private String tableName = null;
    
    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(org.apache.avalon.framework.service.ServiceManager)
     */
    public void service(ServiceManager arg0) throws ServiceException {
        if (tableName == null || tableName.equals("")) {
            table = (VirtualUserTable) arg0.lookup(VirtualUserTable.ROLE); 
        } else {
            table = ((VirtualUserTableStore) arg0.lookup(VirtualUserTableStore.ROLE)).getTable(tableName);
        }
    }
    
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
        Configuration vutConfig = arg0.getChild("enableVirtualUserTable");
        
        if (vutConfig != null) {
            vut = vutConfig.getValueAsBoolean(true);    
        }
        Configuration tableConfig = arg0.getChild("table");
        
        if (tableConfig != null) {
            tableName = tableConfig.getValue(null);   
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
     * Set the domains for which every rcpt will be accepted. 
     * 
     * @param dom The valid domains. Commaseperated list
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
    
    public void setVirtualUserTableSupport(boolean vut) {
        this.vut = vut;
    }

    /**
     * @see org.apache.james.smtpserver.hook.RcptHook#doRcpt(org.apache.james.smtpserver.SMTPSession, org.apache.mailet.MailAddress, org.apache.mailet.MailAddress)
     */
    public HookResult doRcpt(SMTPSession session, MailAddress sender, MailAddress rcpt) {
        
    if (!session.isRelayingAllowed() && session.getUser() == null) {
            boolean invalidUser = true;

            if (session.getConfigurationData().getUsersRepository().contains(rcpt.getUser()) == true || recipients.contains(rcpt.toString().toLowerCase()) || domains.contains(rcpt.getHost().toLowerCase())) {
                invalidUser = false;
            }

            // check if an valid virtual mapping exists
            if (invalidUser == true  && vut == true) {
                try {
                    Collection targetString = table.getMappings(rcpt.getUser(), rcpt.getHost());
            
                    if (targetString != null && targetString.isEmpty() == false) {
                        invalidUser = false;
                    }
                } catch (ErrorMappingException e) {
                    String responseString = e.getMessage();
                    getLogger().info("Rejected message. Reject Message: " + responseString);
                    SMTPResponse resp = new SMTPResponse(responseString);
                    return new HookResult(HookReturnCode.DENY,resp.getRetCode(),(String) resp.getLines().get(0));
                }
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
        
            if (invalidUser == true) {
                //user not exist
                getLogger().info("Rejected message. Unknown user: " + rcpt.toString());
                return new HookResult(HookReturnCode.DENY,SMTPRetCode.TRANSACTION_FAILED, DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.ADDRESS_MAILBOX) + " Unknown user: " + rcpt.toString());
            }
            return new HookResult(HookReturnCode.DECLINED);
        } else {
            getLogger().debug("Sender allowed");
            return new HookResult(HookReturnCode.DECLINED);
        }
    }
}
