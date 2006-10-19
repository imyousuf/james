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

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.services.DNSServer;
import org.apache.james.services.DomainList;

public abstract class AbstractDomainList extends AbstractLogEnabled implements Serviceable, DomainList {
    DNSServer dns;

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(org.apache.avalon.framework.service.ServiceManager)
     */
    public void service(ServiceManager arg0) throws ServiceException {
        dns = (DNSServer) arg0.lookup(DNSServer.ROLE);
    }
    

    /**
     * @see org.apache.james.services.DomainList#getDomains()
     */
    public List getDomains() {  
        List domains = getInternalDomainList();
        if (getLogger().isInfoEnabled()) {
            for (Iterator i = domains.iterator(); i.hasNext(); ) {
                getLogger().info("Handling mail for: " + i.next());
            }
        }  
        return domains;
    }
    
    /**
     * Return a List which holds all ipAddress of the domains in the given List
     * 
     * @param domains List of domains
     * @return domainIP List of ipaddress for domains
     */
    protected List getDomainsIP(List domains) {
        List domainIP = new ArrayList();
        if (domains.size() > 0 ) {
            for (int i = 0; i < domains.size(); i++) {
                try {
                    InetAddress[]  addrs = InetAddress.getAllByName(domains.get(i).toString());
                    for (int j = 0; j < addrs.length ; j++) {
                        domainIP.add(addrs[j].getHostAddress());
                    }
                } catch (UnknownHostException e) {
                    getLogger().error("Cannot get IP address(es) for " + domains.get(i));
                }
            }
        }
        return domainIP;    
    }
    
    /**
     * Return dnsServer
     * 
     * @return dns
     */
    protected DNSServer getDNSServer() {
        return dns;
    }
    
    /**
     * Return domainList
     * 
     * @return List
     */
    protected abstract List getInternalDomainList();
}
