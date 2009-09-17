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

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.dsn.DSNStatus;
import org.apache.james.smtpserver.Configurable;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.smtpserver.hook.HookResult;
import org.apache.james.smtpserver.hook.HookReturnCode;
import org.apache.james.smtpserver.hook.MessageHook;
import org.apache.james.socket.LogEnabled;
import org.apache.mailet.Mail;

/**
 * Extract domains from message and check against URIRBLServer. For more informations see http://www.surbl.org
 */
public class URIRBLHandler implements LogEnabled, MessageHook, Configurable {

    /** This log is the fall back shared by all instances */
    private static final Log FALLBACK_LOG = LogFactory.getLog(URIRBLHandler.class);
    
    /** Non context specific log should only be used when no context specific log is available */
    private Log serviceLog = FALLBACK_LOG;
    
    private final static String LISTED_DOMAIN ="LISTED_DOMAIN";
    
    private final static String URBLSERVER = "URBL_SERVER";
    
    private DNSService dnsService;

    private Collection<String> uriRbl;

    private boolean getDetail = false;

    private boolean checkAuthNetworks = false;

    /**
     * Sets the service log.
     * Where available, a context sensitive log should be used.
     * @param Log not null
     */
    public void setLog(Log log) {
        this.serviceLog = log;
    }
    
    /**
     * Gets the DNS service.
     * @return the dnsService
     */
    public final DNSService getDNSService() {
        return dnsService;
    }

    /**
     * Sets the DNS service.
     * @param dnsService the dnsService to set
     */
    @Resource(name="dnsserver")
    public final void setDNSService(DNSService dnsService) {
        this.dnsService = dnsService;
    }
    

    
    /**
     * @see org.apache.james.smtpserver.Configurable#configure(org.apache.commons.configuration.Configuration)
     */
    public void configure(Configuration config) throws ConfigurationException {
        String[] servers = config.getStringArray("uriRblServers/server");
        Collection<String> serverCollection = new ArrayList<String>();
        for ( int i = 0 ; i < servers.length ; i++ ) {
            String rblServerName = servers[i];
            serverCollection.add(rblServerName);
            if (serviceLog.isInfoEnabled()) {
                serviceLog.info("Adding uriRBL server: " + rblServerName);
            }
        }
        if (serverCollection != null && serverCollection.size() > 0) {
            setUriRblServer(serverCollection);
        } else {
            throw new ConfigurationException("Please provide at least one server");
        }
            
        setGetDetail(config.getBoolean("getDetail",false));
        setCheckAuthNetworks(config.getBoolean("checkAuthNetworks", false));
        
        
    }
   
    /**
     * Set the UriRBL Servers
     * 
     * @param uriRbl The Collection holding the servers
     */
    public void setUriRblServer(Collection<String> uriRbl) {
        this.uriRbl = uriRbl;
    }
    
    /**
     * Set to true if AuthNetworks should be included in the EHLO check
     * 
     * @param checkAuthNetworks
     *            Set to true to enable
     */
    public void setCheckAuthNetworks(boolean checkAuthNetworks) {
        this.checkAuthNetworks = checkAuthNetworks;
    }

    /**
     * Set for try to get a TXT record for the blocked record. 
     * 
     * @param getDetail Set to ture for enable
     */
    public void setGetDetail(boolean getDetail) {
        this.getDetail = getDetail;
    }
    
    /**
     * @see org.apache.james.smtpserver.hook.MessageHook#onMessage(org.apache.james.smtpserver.SMTPSession, org.apache.mailet.Mail)
     */
    public HookResult onMessage(SMTPSession session, Mail mail) {
        if (check(session, mail)) {
            String uRblServer = (String) session.getState().get(URBLSERVER);
            String target = (String) session.getState().get(LISTED_DOMAIN);
            String detail = null;

            // we should try to retrieve details
            if (getDetail) {
                Collection txt = dnsService.findTXTRecords(target+ "." + uRblServer);

                // Check if we found a txt record
                if (!txt.isEmpty()) {
                    // Set the detail
                    detail = txt.iterator().next().toString();

                }
            }

            if (detail != null) {
                return new HookResult(HookReturnCode.DENY, DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.SECURITY_OTHER)
                    + "Rejected: message contains domain " + target + " listed by " + uRblServer +" . Details: " 
                    + detail);
            } else {
                return new HookResult(HookReturnCode.DENY, DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.SECURITY_OTHER)
                    + " Rejected: message contains domain " + target + " listed by " + uRblServer);
            }  

        } else {
            return new HookResult(HookReturnCode.DECLINED);
        }
    }

    /**
     * Recursively scans all MimeParts of an email for domain strings. Domain
     * strings that are found are added to the supplied HashSet.
     *
     * @param part MimePart to scan
     * @param session not null
     * @return domains The HashSet that contains the domains which were extracted
     */
    private HashSet<String> scanMailForDomains(MimePart part, SMTPSession session) throws MessagingException, IOException {
        HashSet<String> domains = new HashSet<String>();
        session.getLogger().debug("mime type is: \"" + part.getContentType() + "\"");
       
        if (part.isMimeType("text/plain") || part.isMimeType("text/html")) {
            session.getLogger().debug("scanning: \"" + part.getContent().toString() + "\"");
            HashSet<String> newDom = URIScanner.scanContentForDomains(domains, part.getContent().toString());
           
            // Check if new domains are found and add the domains 
            if (newDom != null && newDom.size() > 0) {
                domains.addAll(newDom);
            }
        } else if (part.isMimeType("multipart/*")) {
            MimeMultipart multipart = (MimeMultipart) part.getContent();
            int count = multipart.getCount();
            session.getLogger().debug("multipart count is: " + count);
          
            for (int index = 0; index < count; index++) {
                session.getLogger().debug("recursing index: " + index);
                MimeBodyPart mimeBodyPart = (MimeBodyPart) multipart.getBodyPart(index);
                HashSet<String> newDomains = scanMailForDomains(mimeBodyPart, session);
                
                // Check if new domains are found and add the domains 
                if(newDomains != null && newDomains.size() > 0) {
                    domains.addAll(newDomains);
                }
            }
        }
        return domains;
    }

    /**
     * Check method
     */
    protected boolean check(SMTPSession session, Mail mail) {
        MimeMessage message;
        
        // Not scan the message if relaying allowed
        if (session.isRelayingAllowed() && !checkAuthNetworks) {
            return false;
        }
        
        try {
            message = mail.getMessage();

            HashSet<String> domains = scanMailForDomains(message, session);

            Iterator<String> fDomains = domains.iterator();

            while (fDomains.hasNext()) {
                Iterator<String> uRbl = uriRbl.iterator();
                String target = fDomains.next().toString();
                
                while (uRbl.hasNext()) {
                    try {
                        String uRblServer = uRbl.next().toString();
                        String address = target + "." + uRblServer;
                        
                        if (session.getLogger().isDebugEnabled()) {
                            session.getLogger().debug("Lookup " + address);
                        }
                        
                        dnsService.getByName(address);
            
                        // store server name for later use
                        session.getState().put(URBLSERVER, uRblServer);
                        session.getState().put(LISTED_DOMAIN,target);

                        return true;

                    } catch (UnknownHostException uhe) {
                        // domain not found. keep processing
                    }
                }
            }
        } catch (MessagingException e) {
            session.getLogger().error(e.getMessage());
        } catch (IOException e) {
            session.getLogger().error(e.getMessage());
        }
        return false;
    }
}
