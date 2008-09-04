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



package org.apache.james.domain;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.api.domainlist.ManageableDomainList;

/**
 * All implementations of the DomainList interface should extends this abstract class
 */
public abstract class AbstractDomainList extends AbstractLogEnabled implements Serviceable, ManageableDomainList {
    private DNSService dns;
    private boolean autoDetect = true;
    private boolean autoDetectIP = true;

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(org.apache.avalon.framework.service.ServiceManager)
     */
    public void service(ServiceManager arg0) throws ServiceException {
        dns = (DNSService) arg0.lookup(DNSService.ROLE);
    }
    

    /**
     * @see org.apache.james.api.domainlist.DomainList#getDomains()
     */
    public List getDomains() {  
        List domains = getDomainListInternal();
        if (domains != null) {
            
            String hostName = null;
            try {
                hostName = getDNSServer().getHostName(getDNSServer().getLocalHost());
            } catch  (UnknownHostException ue) {
                hostName = "localhost";
            }
            
            getLogger().info("Local host is: " + hostName);
            
            if (autoDetect == true && (!hostName.equals("localhost"))) {
                domains.add(hostName.toLowerCase(Locale.US));
            }

            
            if (autoDetectIP == true) {
                domains.addAll(getDomainsIP(domains,dns,getLogger()));
            }
       
            if (getLogger().isInfoEnabled()) {
                for (Iterator i = domains.iterator(); i.hasNext(); ) {
                    getLogger().debug("Handling mail for: " + i.next());
                }
            }  
            return domains;
        } else {
            return null;
        }
    }
    
    
    /**
     * Return a List which holds all ipAddress of the domains in the given List
     * 
     * @param domains List of domains
     * @return domainIP List of ipaddress for domains
     */
    private static List getDomainsIP(List domains,DNSService dns,Logger log) {
        List domainIP = new ArrayList();
        if (domains.size() > 0 ) {
            for (int i = 0; i < domains.size(); i++) {
                List domList = getDomainIP(domains.get(i).toString(),dns,log);
                
                for(int i2 = 0; i2 < domList.size();i2++) {
                    if(domainIP.contains(domList.get(i2)) == false) {
                        domainIP.add(domList.get(i2));
                    }
                }
            }
        }
        return domainIP;    
    }
    
    /**
     * @see #getDomainsIP(List, DNSService, Logger)
     */
    private static List getDomainIP(String domain, DNSService dns, Logger log) {
        List domainIP = new ArrayList();
        try {
            InetAddress[]  addrs = dns.getAllByName(domain);
            for (int j = 0; j < addrs.length ; j++) {
                String ip = addrs[j].getHostAddress();
                if (domainIP.contains(ip) == false) {
                    domainIP.add(ip);
                }
            }
        } catch (UnknownHostException e) {
            log.error("Cannot get IP address(es) for " + domain);
        }
        return domainIP;
    }
    
    /**
     * @see org.apache.james.api.domainlist.ManageableDomainList#addDomain(java.lang.String)
     */
    public synchronized boolean addDomain(String domain) {
        getLogger().info("Add domain " + domain + " to DomainList");
    
        //TODO: Should we care about autoDetectIP ?
        return addDomainInternal(domain);
    }
    
    /**
     * @see org.apache.james.api.domainlist.ManageableDomainList#removeDomain(java.lang.String)
     */
    public synchronized boolean removeDomain(String domain) {
        getLogger().info("Remove domain " + domain + " from DomainList");
    
    
        //TODO: Should we care about autoDetectIP ?
        return removeDomainInternal(domain);
    }
    
    /**
     * @see org.apache.james.api.domainlist.DomainList#setAutoDetect(boolean)
     */
    public synchronized void setAutoDetect(boolean autoDetect) {
        getLogger().info("Set autodetect to: " + autoDetect);
        this.autoDetect = autoDetect;
    }
    
    /**
     * @see org.apache.james.api.domainlist.DomainList#setAutoDetectIP(boolean)
     */
    public synchronized void setAutoDetectIP(boolean autoDetectIP) {
        getLogger().info("Set autodetectIP to: " + autoDetectIP);
        this.autoDetectIP = autoDetectIP;
    }
    
    /**
     * Return dnsServer
     * 
     * @return dns
     */
    protected DNSService getDNSServer() {
        return dns;
    }
    
    /**
     * Return domainList
     * 
     * @return List
     */
    protected abstract List getDomainListInternal();
    
    /**
     * Add domain
     * 
     * @param domain domain to add
     * @return true if successfully
     */
    protected abstract boolean addDomainInternal(String domain);
    
    /**
     * Remove domain
     * 
     * @param domain domain to remove
     * @return true if successfully
     */
    protected abstract boolean removeDomainInternal(String domain);
}
