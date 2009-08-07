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

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.dsn.DSNStatus;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.smtpserver.core.PostDataListener;
import org.apache.james.smtpserver.urirbl.URIScanner;
import org.apache.mailet.Mail;

/**
 * Extract domains from message and check against URIRBLServer. For more informations see http://www.surbl.org
 */
public class URIRBLHandler extends AbstractLogEnabled implements PostDataListener,
    Serviceable {

    private DNSService dnsServer;

    private Collection uriRbl;

    private boolean getDetail = false;

    private boolean checkAuthNetworks = false;
    
    private final static String LISTED_DOMAIN ="LISTED_DOMAIN";
    
    private final static String URBLSERVER = "URBL_SERVER";
    
    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager)
     */
    public void service(ServiceManager serviceMan) throws ServiceException {
        setDnsServer((DNSService) serviceMan.lookup(DNSService.ROLE));
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration arg0) throws ConfigurationException {
        boolean invalidConfig = false;
    
        Configuration serverConfiguration = arg0.getChild("uriRblServers", false);
        if ( serverConfiguration != null ) {
            ArrayList serverCollection = new ArrayList();
            Configuration[] children = serverConfiguration.getChildren("server");
            if ( children != null ) {
                for ( int i = 0 ; i < children.length ; i++ ) {
                    String rblServerName = children[i].getValue();
                    serverCollection.add(rblServerName);
                    if (getLogger().isInfoEnabled()) {
                        getLogger().info("Adding uriRBL server: " + rblServerName);
                    }
                }
                if (serverCollection != null && serverCollection.size() > 0) {
                    setUriRblServer(serverCollection);
                } else {
                    invalidConfig = true;
                }
            }
        } else {
            invalidConfig = true;
        }
        
        if (invalidConfig == true) {
            throw new ConfigurationException("Please provide at least one server");
        }
    
        Configuration configuration = arg0.getChild("getDetail",false);
        if(configuration != null) {
           getDetail = configuration.getValueAsBoolean();
        }
        
        Configuration configRelay = arg0.getChild("checkAuthNetworks", false);
        if (configRelay != null) {
            setCheckAuthNetworks(configRelay.getValueAsBoolean(false));
        }

    }
   
    /**
     * Set the UriRBL Servers
     * 
     * @param uriRbl The Collection holding the servers
     */
    public void setUriRblServer(Collection uriRbl) {
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
     * Set the DNSService
     * 
     * @param dnsServer
     *            The DNSService
     */
    public void setDnsServer(DNSService dnsServer) {
        this.dnsServer = dnsServer;
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
     * Recursively scans all MimeParts of an email for domain strings. Domain
     * strings that are found are added to the supplied HashSet.
     *
     * @param part MimePart to scan
     * @return domains The HashSet that contains the domains which were extracted
     */
    private HashSet scanMailForDomains(MimePart part) throws MessagingException, IOException {
        HashSet domains = new HashSet();
        getLogger().debug("mime type is: \"" + part.getContentType() + "\"");
       
        if (part.isMimeType("text/plain") || part.isMimeType("text/html")) {
            getLogger().debug("scanning: \"" + part.getContent().toString() + "\"");
            HashSet newDom = URIScanner.scanContentForDomains(domains, part.getContent().toString());
           
            // Check if new domains are found and add the domains 
            if (newDom != null && newDom.size() > 0) {
                domains.addAll(newDom);
            }
        } else if (part.isMimeType("multipart/*")) {
            MimeMultipart multipart = (MimeMultipart) part.getContent();
            int count = multipart.getCount();
            getLogger().debug("multipart count is: " + count);
          
            for (int index = 0; index < count; index++) {
                getLogger().debug("recursing index: " + index);
                MimeBodyPart mimeBodyPart = (MimeBodyPart) multipart.getBodyPart(index);
                HashSet newDomains = scanMailForDomains(mimeBodyPart);
                
                // Check if new domains are found and add the domains 
                if(newDomains != null && newDomains.size() > 0) {
                    domains.addAll(newDomains);
                }
            }
        }
        return domains;
    }



    /**
     * (non-Javadoc)
     * @see org.apache.james.smtpserver.core.PostDataListener#onData(org.apache.james.smtpserver.SMTPSession, org.apache.mailet.Mail)
     */
	public String onData(SMTPSession session, Mail mail) {
		MimeMessage message;

		// Not scan the message if relaying allowed
		if (session.isRelayingAllowed() && !checkAuthNetworks) {
			return null;
		}

		try {
			message = mail.getMessage();

			HashSet domains = scanMailForDomains(message);

			Iterator fDomains = domains.iterator();

			while (fDomains.hasNext()) {
				Iterator uRbl = uriRbl.iterator();
				String target = fDomains.next().toString();

				while (uRbl.hasNext()) {
					try {
						String uRblServer = uRbl.next().toString();
						String address = target + "." + uRblServer;

						if (getLogger().isDebugEnabled()) {
							getLogger().debug("Lookup " + address);
						}

						dnsServer.getByName(address);

						String detail = null;

						// we should try to retrieve details
						if (getDetail) {
							Collection txt = dnsServer.findTXTRecords(target
									+ "." + uRblServer);

							// Check if we found a txt record
							if (!txt.isEmpty()) {
								// Set the detail
								detail = txt.iterator().next().toString();

							}
						}

						if (detail != null) {

							return "554 "
									+ DSNStatus.getStatus(DSNStatus.PERMANENT,
											DSNStatus.SECURITY_OTHER)
									+ "Rejected: message contains domain "
									+ target + " listed by " + uRblServer
									+ " . Details: " + detail;
						} else {
							return "554 "
									+ DSNStatus.getStatus(DSNStatus.PERMANENT,
											DSNStatus.SECURITY_OTHER)
									+ " Rejected: message contains domain "
									+ target + " listed by " + uRblServer;
						}

					} catch (UnknownHostException uhe) {
						// domain not found. keep processing
					}
				}
			}
		} catch (MessagingException e) {
			getLogger().error(e.getMessage());
		} catch (IOException e) {
			getLogger().error(e.getMessage());
		}
		return null;
	}

}
