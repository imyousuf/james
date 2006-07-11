/***********************************************************************
 * Copyright (c) 1999-2006 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.smtpserver;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.james.services.DNSServer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;

/**
  * Connect handler for DNSRBL processing
  */
public class DNSRBLHandler
    extends AbstractLogEnabled
    implements ConnectHandler, Configurable, Serviceable {
    /**
     * The lists of rbl servers to be checked to limit spam
     */
    private String[] whitelist;
    private String[] blacklist;
    
    private DNSServer dnsServer = null;
    
    private boolean getDetail = false;

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration handlerConfiguration) throws ConfigurationException {

        Configuration rblserverConfiguration = handlerConfiguration.getChild("rblservers", false);
        if ( rblserverConfiguration != null ) {
            ArrayList rblserverCollection = new ArrayList();
            Configuration[] children = rblserverConfiguration.getChildren("whitelist");
            if ( children != null ) {
                for ( int i = 0 ; i < children.length ; i++ ) {
                    String rblServerName = children[i].getValue();
                    rblserverCollection.add(rblServerName);
                    if (getLogger().isInfoEnabled()) {
                        getLogger().info("Adding RBL server to whitelist: " + rblServerName);
                    }
                }
                if (rblserverCollection != null && rblserverCollection.size() > 0) {
                    setWhitelist((String[]) rblserverCollection.toArray(new String[rblserverCollection.size()]));
                    rblserverCollection.clear();
                }
            }
            children = rblserverConfiguration.getChildren("blacklist");
            if ( children != null ) {
                for ( int i = 0 ; i < children.length ; i++ ) {
                    String rblServerName = children[i].getValue();
                    rblserverCollection.add(rblServerName);
                    if (getLogger().isInfoEnabled()) {
                        getLogger().info("Adding RBL server to blacklist: " + rblServerName);
                    }
                }
                if (rblserverCollection != null && rblserverCollection.size() > 0) {
                    setBlacklist((String[]) rblserverCollection.toArray(new String[rblserverCollection.size()]));
                    rblserverCollection.clear();
                }
            }
        }
        
        Configuration configuration = handlerConfiguration.getChild("getDetail",false);
        if(configuration != null) {
           getDetail = configuration.getValueAsBoolean();
        }

    }

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager)
     */
    public void service(ServiceManager serviceMan) throws ServiceException {
        setDNSServer((DNSServer) serviceMan.lookup(DNSServer.ROLE));
    }
    
    /*
     * check if the remote Ip address is block listed
     *
     * @see org.apache.james.smtpserver.ConnectHandler#onConnect(SMTPSession)
    **/
    public void onConnect(SMTPSession session) {
        boolean blocklisted = checkDNSRBL(session, session.getRemoteIPAddress());
        session.setBlockListed(blocklisted);
    }
    
    /**
     * Set the whitelist array
     * 
     * @param whitelist The array which contains the whitelist
     */
    public void setWhitelist(String[] whitelist) {
        this.whitelist = whitelist;
    }
    
    /**
     * Set the blacklist array
     * 
     * @param blacklist The array which contains the blacklist
     */
    public void setBlacklist(String[] blacklist) {
        this.blacklist = blacklist;
    }
    
    /**
     * Set the DNSServer
     * 
     * @param dnsServer The DNSServer
     */
    public void setDNSServer(DNSServer dnsServer) {
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
     * @see org.apache.james.smtpserver.SMTPHandlerConfigurationData#checkDNSRBL(Socket)
     */
    /*
     * This checks DNSRBL whitelists and blacklists.  If the remote IP is whitelisted
     * it will be permitted to send e-mail, otherwise if the remote IP is blacklisted,
     * the sender will only be permitted to send e-mail to postmaster (RFC 2821) or
     * abuse (RFC 2142), unless authenticated.
     */

    public boolean checkDNSRBL(SMTPSession session, String ipAddress) {
        
        /*
         * don't check against rbllists if the client is allowed to relay..
         * This whould make no sense.
         */
        if (session.isRelayingAllowed()) {
            getLogger().info("Ipaddress " + session.getRemoteIPAddress() + " is allowed to relay. Don't check it");
            return false;
        }
        
        if (whitelist != null || blacklist != null) {
            StringBuffer sb = new StringBuffer();
            StringTokenizer st = new StringTokenizer(ipAddress, " .", false);
            while (st.hasMoreTokens()) {
                sb.insert(0, st.nextToken() + ".");
            }
            String reversedOctets = sb.toString();

            if (whitelist != null) {
                String[] rblList = whitelist;
                for (int i = 0 ; i < rblList.length ; i++) try {
                    dnsServer.getByName(reversedOctets + rblList[i]);
                    if (getLogger().isInfoEnabled()) {
                        getLogger().info("Connection from " + ipAddress + " whitelisted by " + rblList[i]);
                    }
                    return false;
                } catch (java.net.UnknownHostException uhe) {
                    if (getLogger().isInfoEnabled()) {
                        getLogger().info("IpAddress " + session.getRemoteIPAddress() + " not listed on " + rblList[i]);
                    }
                }
            }

            if (blacklist != null) {
                String[] rblList = blacklist;
                for (int i = 0 ; i < rblList.length ; i++) try {
                    dnsServer.getByName(reversedOctets + rblList[i]);
                    if (getLogger().isInfoEnabled()) {
                        getLogger().info("Connection from " + ipAddress + " restricted by " + rblList[i] + " to SMTP AUTH/postmaster/abuse.");
                    }
                    
                    // we should try to retrieve details
                    if (getDetail) {
                        Collection txt = dnsServer.findTXTRecords(reversedOctets + rblList[i]);
                        
                        // Check if we found a txt record
                        if (!txt.isEmpty()) {
                            // Set the detail
                            session.setBlockListedDetail(txt.iterator().next().toString());
                        }
                    }
                    return true;
                } catch (java.net.UnknownHostException uhe) {
                    // if it is unknown, it isn't blocked
                    if (getLogger().isInfoEnabled()) {
                        getLogger().info("unknown host exception thrown:" + rblList[i]);
                    }
                }
            }
        }
        return false;
    }

}
