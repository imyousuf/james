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



package org.apache.james.smtpserver.protocol.core.fastfail;

import java.util.Collection;
import java.util.StringTokenizer;

import org.apache.james.api.protocol.ConnectHandler;
import org.apache.james.dsn.DSNStatus;
import org.apache.james.smtpserver.protocol.DNSService;
import org.apache.james.smtpserver.protocol.SMTPSession;
import org.apache.james.smtpserver.protocol.hook.HookResult;
import org.apache.james.smtpserver.protocol.hook.HookReturnCode;
import org.apache.james.smtpserver.protocol.hook.RcptHook;
import org.apache.mailet.MailAddress;

/**
  * Connect handler for DNSRBL processing
  */
public class DNSRBLHandler implements  ConnectHandler<SMTPSession>, RcptHook{

    
    /**
     * The lists of rbl servers to be checked to limit spam
     */
    private String[] whitelist;
    private String[] blacklist;
    
    private DNSService dnsService = null;
    
    private boolean getDetail = false;
    
    private String blocklistedDetail = null;
    
    public static final String RBL_BLOCKLISTED_MAIL_ATTRIBUTE_NAME = "org.apache.james.smtpserver.rbl.blocklisted";
    
    public static final String RBL_DETAIL_MAIL_ATTRIBUTE_NAME = "org.apache.james.smtpserver.rbl.detail";


    /**
     * Sets the DNS service.
     * @param dnsService the dnsService to set
     */
    public final void setDNSService(DNSService dnsService) {
        this.dnsService = dnsService;
    }

   
    
    /**
     * check if the remote Ip address is block listed
     *
    **/
    public void onConnect(SMTPSession session) {
        checkDNSRBL(session, session.getRemoteIPAddress());
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
     * Set for try to get a TXT record for the blocked record. 
     * 
     * @param getDetail Set to ture for enable
     */
    public void setGetDetail(boolean getDetail) {
        this.getDetail = getDetail;
    }

    /**
     *
     * This checks DNSRBL whitelists and blacklists.  If the remote IP is whitelisted
     * it will be permitted to send e-mail, otherwise if the remote IP is blacklisted,
     * the sender will only be permitted to send e-mail to postmaster (RFC 2821) or
     * abuse (RFC 2142), unless authenticated.
     */

    public void checkDNSRBL(SMTPSession session, String ipAddress) {
        
        /*
         * don't check against rbllists if the client is allowed to relay..
         * This whould make no sense.
         */
        if (session.isRelayingAllowed()) {
            session.getLogger().info("Ipaddress " + session.getRemoteIPAddress() + " is allowed to relay. Don't check it");
            return;
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
                    dnsService.getByName(reversedOctets + rblList[i]);
                    if (session.getLogger().isInfoEnabled()) {
                        session.getLogger().info("Connection from " + ipAddress + " whitelisted by " + rblList[i]);
                    }
                    
                    return;
                } catch (java.net.UnknownHostException uhe) {
                    if (session.getLogger().isDebugEnabled()) {
                        session.getLogger().debug("IpAddress " + session.getRemoteIPAddress() + " not listed on " + rblList[i]);
                    }
                }
            }

            if (blacklist != null) {
                String[] rblList = blacklist;
                for (int i = 0 ; i < rblList.length ; i++) try {
                    dnsService.getByName(reversedOctets + rblList[i]);
                    if (session.getLogger().isInfoEnabled()) {
                        session.getLogger().info("Connection from " + ipAddress + " restricted by " + rblList[i] + " to SMTP AUTH/postmaster/abuse.");
                    }
                    
                    // we should try to retrieve details
                    if (getDetail) {
                        Collection<String> txt = dnsService.findTXTRecords(reversedOctets + rblList[i]);
                        
                        // Check if we found a txt record
                        if (!txt.isEmpty()) {
                            // Set the detail
                            String blocklistedDetail = txt.iterator().next().toString();
                            
                            session.getConnectionState().put(RBL_DETAIL_MAIL_ATTRIBUTE_NAME, blocklistedDetail);
                        }
                    }
                    
                    session.getConnectionState().put(RBL_BLOCKLISTED_MAIL_ATTRIBUTE_NAME, "true");
                    return;
                } catch (java.net.UnknownHostException uhe) {
                    // if it is unknown, it isn't blocked
                    if (session.getLogger().isDebugEnabled()) {
                        session.getLogger().debug("unknown host exception thrown:" + rblList[i]);
                    }
                }
            }
        }
    }

    /**
     * @see org.apache.james.smtpserver.protocol.hook.RcptHook#doRcpt(org.apache.james.smtpserver.protocol.SMTPSession, org.apache.mailet.MailAddress, org.apache.mailet.MailAddress)
     */
    public HookResult doRcpt(SMTPSession session, MailAddress sender, MailAddress rcpt) {
        
        if (!session.isRelayingAllowed()) {
            String blocklisted = (String) session.getConnectionState().get(RBL_BLOCKLISTED_MAIL_ATTRIBUTE_NAME);
    
            if (blocklisted != null) { // was found in the RBL
                if (blocklistedDetail == null) {
                    return new HookResult(HookReturnCode.DENY,DSNStatus.getStatus(DSNStatus.PERMANENT,
                            DSNStatus.SECURITY_AUTH)  + " Rejected: unauthenticated e-mail from " + session.getRemoteIPAddress() 
                            + " is restricted.  Contact the postmaster for details.");
                } else {
                    return new HookResult(HookReturnCode.DENY,DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.SECURITY_AUTH) + " " + blocklistedDetail);
                }
               
            }
        }
        return new HookResult(HookReturnCode.DECLINED);
    }
}
