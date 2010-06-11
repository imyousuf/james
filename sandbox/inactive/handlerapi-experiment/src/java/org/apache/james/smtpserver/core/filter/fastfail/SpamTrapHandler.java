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
import java.util.HashMap;
import java.util.Map;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.smtpserver.hook.HookResult;
import org.apache.james.smtpserver.hook.HookReturnCode;
import org.apache.james.smtpserver.hook.RcptHook;
import org.apache.mailet.MailAddress;

/**
 * This handler can be used for providing a spam trap. IPAddresses which send emails to the configured
 * recipients will get blacklisted for the configured time.
 */
public class SpamTrapHandler extends AbstractLogEnabled implements RcptHook,Configurable{

    // Map which hold blockedIps and blockTime in memory
    private Map blockedIps = new HashMap();
    
    private Collection spamTrapRecips = new ArrayList();
    
    // Default blocktime 12 hours
    private long blockTime = 4320000; 
    
    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(org.apache.avalon.framework.configuration.Configuration)
     */
    public void configure(Configuration arg0) throws ConfigurationException {
        Configuration[] rcptsConf = arg0.getChildren("spamTrapRecip");
    
        if (rcptsConf.length > 0 ) {
            for (int i= 0; i < rcptsConf.length; i++) {
                String rcpt = rcptsConf[i].getValue().toLowerCase();
                
                getLogger().debug("Add spamTrapRecip " + rcpt);
           
                spamTrapRecips.add(rcpt);
            }
        } else {
            throw new ConfigurationException("Please configure a spamTrapRecip.");
        }
    
        Configuration blockTimeConf = arg0.getChild("blockTime",false);
    
        if (blockTimeConf != null) {
            blockTime = blockTimeConf.getValueAsLong(blockTime);
        }
    }
    
    public void setSpamTrapRecipients(Collection spamTrapRecips) {
        this.spamTrapRecips = spamTrapRecips;
    }
    
    public void setBlockTime(long blockTime) {
        this.blockTime = blockTime;
    }
    
    /**
     * @see org.apache.james.smtpserver.hook.RcptHook#doRcpt(org.apache.james.smtpserver.SMTPSession, org.apache.mailet.MailAddress, org.apache.mailet.MailAddress)
     */
    public HookResult doRcpt(SMTPSession session, MailAddress sender, MailAddress rcpt) {
        if (isBlocked(session.getRemoteIPAddress())) {
            return new HookResult(HookReturnCode.DENY);
        } else {
         
            if (spamTrapRecips.contains(rcpt.toString().toLowerCase())){
        
                addIp(session.getRemoteIPAddress());
            
                return new HookResult(HookReturnCode.DENY);
            }
        }
        return new HookResult(HookReturnCode.DECLINED);
    }
    
    
    /**
     * Check if ipAddress is in the blockList.
     * 
     * @param ip ipAddress to check
     * @return true or false
     */
    private boolean isBlocked(String ip) {
        Object rawTime = blockedIps.get(ip);
    
        if (rawTime != null) {
            long blockTime = ((Long) rawTime).longValue();
           
            if (blockTime > System.currentTimeMillis()) {
                getLogger().debug("BlockList contain Ip " + ip);
                return true;
            } else {
                getLogger().debug("Remove ip " + ip + " from blockList");
               
                synchronized(blockedIps) {
                    blockedIps.remove(ip);
                }
            }
        }
        return false;
    }
    
    /**
     * Add ipaddress to blockList
     * 
     * @param ip IpAddress to add
     */
    private void addIp(String ip) {
        long bTime = System.currentTimeMillis() + blockTime;
        
        getLogger().debug("Add ip " + ip + " for " + bTime + " to blockList");
    
        synchronized(blockedIps) {
            blockedIps.put(ip, new Long(bTime));
        }
    
    }
}
