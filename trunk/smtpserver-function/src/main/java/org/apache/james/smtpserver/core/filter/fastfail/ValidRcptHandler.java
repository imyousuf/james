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

import javax.annotation.Resource;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.api.vut.ErrorMappingException;
import org.apache.james.api.vut.VirtualUserTable;
import org.apache.james.api.vut.VirtualUserTableStore;
import org.apache.james.dsn.DSNStatus;
import org.apache.james.smtpserver.Configurable;
import org.apache.james.smtpserver.SMTPResponse;
import org.apache.james.smtpserver.SMTPRetCode;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.smtpserver.hook.HookResult;
import org.apache.james.smtpserver.hook.HookReturnCode;
import org.apache.james.smtpserver.hook.RcptHook;
import org.apache.mailet.MailAddress;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;

/**
 * Handler which reject invalid recipients
 */
public class ValidRcptHandler extends AbstractLogEnabled implements RcptHook, Configurable, Serviceable {
    
    private UsersRepository users;
    
    /**
     * Gets the users repository.
     * @return the users
     */
    public final UsersRepository getUsers() {
        return users;
    }

    /**
     * Sets the users repository.
     * @param users the users to set
     */
    @Resource(name="localusersrepository")
    public final void setUsers(UsersRepository users) {
        this.users = users;
    }
    
    private Collection<String> recipients = new ArrayList<String>();
    private Collection<String> domains = new ArrayList<String>();
    private Collection<Pattern> regex = new ArrayList<Pattern>();
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
    @SuppressWarnings("unchecked")
	public void configure(Configuration config) throws ConfigurationException {
        setValidRecipients(config.getList("validRecipients"));
        setValidDomains(config.getList("validDomains"));
        try {
            setValidRegex(config.getList("validRegexPattern"));
        } catch(MalformedPatternException mpe) {
            throw new ConfigurationException("Malformed pattern: ", mpe);
        }
        setVirtualUserTableSupport(config.getBoolean("enableVirtualUserTable",true));    
    	setTableName(config.getString("table",null));   
    }
    
    /**
     * Set the valid recipients. 
     * 
     * @param recip The valid recipients. Commaseperated list
     */
    public void setValidRecipients(Collection<String> recips) {
    	Iterator<String> recipsIt = recips.iterator();
    	while (recipsIt.hasNext()) {
            String recipient = recipsIt.next();
            
            getLogger().debug("Add recipient to valid recipients: " + recipient);
            recipients.add(recipient);
    	}
    }

    /**
     * Set the domains for which every rcpt will be accepted. 
     * 
     * @param dom The valid domains. Commaseperated list
     */
    public void setValidDomains(Collection<String> doms) {
    	Iterator<String> domsIt = doms.iterator();
    	
        while (domsIt.hasNext()) {
            String domain = domsIt.next().toLowerCase();
            getLogger().debug("Add domain to valid domains: " + domain);
            domains.add(domain);
        }  
    }
    
    /**
     * 
     * @param reg 
     * @throws MalformedPatternException
     */
    public void setValidRegex(Collection<String> regs) throws MalformedPatternException {
        Perl5Compiler compiler = new Perl5Compiler();
                
        Iterator<String> regsIt = regs.iterator();
        while (regsIt.hasNext()) {
            String patternString = regsIt.next();

            getLogger().debug("Add regex to valid regex: " + patternString);
            
            Pattern pattern = compiler.compile(patternString, Perl5Compiler.READ_ONLY_MASK);
            regex.add(pattern);

        }  
    }
    
    public void setVirtualUserTableSupport(boolean vut) {
        this.vut = vut;
    }

    public void setTableName(String tableName) {
    	this.tableName = tableName;
    }
    
    /**
     * @see org.apache.james.smtpserver.hook.RcptHook#doRcpt(org.apache.james.smtpserver.SMTPSession, org.apache.mailet.MailAddress, org.apache.mailet.MailAddress)
     */
    public HookResult doRcpt(SMTPSession session, MailAddress sender, MailAddress rcpt) {
        
        if (!session.isRelayingAllowed()) {
            boolean invalidUser = true;

            if (users.contains(rcpt.getLocalPart()) == true || recipients.contains(rcpt.toString().toLowerCase()) || domains.contains(rcpt.getDomain().toLowerCase())) {
                invalidUser = false;
            }

            // check if an valid virtual mapping exists
            if (invalidUser == true  && vut == true) {
                try {
                    Collection<String> targetString = table.getMappings(rcpt.getLocalPart(), rcpt.getDomain());
            
                    if (targetString != null && targetString.isEmpty() == false) {
                        invalidUser = false;
                    }
                } catch (ErrorMappingException e) {
                    String responseString = e.getMessage();
                    getLogger().info("Rejected message. Reject Message: " + responseString);
                    SMTPResponse resp = new SMTPResponse(responseString);
                    return new HookResult(HookReturnCode.DENY,resp.getRetCode(),resp.getLines().get(0));
                }
            }
            
            if (invalidUser == true && !regex.isEmpty()) {
                Iterator<Pattern> reg = regex.iterator();
                Perl5Matcher matcher  = new Perl5Matcher();
                
                while (reg.hasNext()) {
                    if (matcher.matches(rcpt.toString(), reg.next())) {
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
        } else {
            getLogger().debug("Sender allowed");
        }
        return new HookResult(HookReturnCode.DECLINED);
    }
}
