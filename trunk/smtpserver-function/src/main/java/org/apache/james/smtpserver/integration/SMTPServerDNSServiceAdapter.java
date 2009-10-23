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
package org.apache.james.smtpserver.integration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Iterator;

import javax.annotation.Resource;

import org.apache.james.smtpserver.protocol.DNSService;
import org.apache.james.smtpserver.protocol.TemporaryResolutionException;
import org.apache.mailet.HostAddress;

public class SMTPServerDNSServiceAdapter implements DNSService{

    private org.apache.james.api.dnsservice.DNSService dns;

    
    public SMTPServerDNSServiceAdapter() {    
    
    }
    
    @Resource(name="org.apache.james.api.dnsservice.DNSService")
    public void setDNSService(org.apache.james.api.dnsservice.DNSService dns) {
        this.dns = dns;
    }
    
    /**
     * @see org.apache.james.smtpserver.protocol.DNSService#findMXRecords(java.lang.String)
     */
    public Collection<String> findMXRecords(String hostname) throws TemporaryResolutionException {
        try {
            return dns.findMXRecords(hostname);
        } catch (org.apache.james.api.dnsservice.TemporaryResolutionException e) {
            throw new TemporaryResolutionException(e.getMessage());
        }
    }

    /**
     * @see org.apache.james.smtpserver.protocol.DNSService#findTXTRecords(java.lang.String)
     */
    public Collection<String> findTXTRecords(String hostname) {
        return dns.findTXTRecords(hostname);
    }

    /**
     * @see org.apache.james.smtpserver.protocol.DNSService#getAllByName(java.lang.String)
     */
    public InetAddress[] getAllByName(String host) throws UnknownHostException {
        return dns.getAllByName(host);
    }

    /**
     * @see org.apache.james.smtpserver.protocol.DNSService#getByName(java.lang.String)
     */
    public InetAddress getByName(String host) throws UnknownHostException {
        return dns.getByName(host);
    }

    /**
     * @see org.apache.james.smtpserver.protocol.DNSService#getHostName(java.net.InetAddress)
     */
    public String getHostName(InetAddress addr) {
        return dns.getHostName(addr);
    }

    /**
     * @see org.apache.james.smtpserver.protocol.DNSService#getLocalHost()
     */
    public InetAddress getLocalHost() throws UnknownHostException {
        return dns.getLocalHost();
    }

    /**
     * @see org.apache.james.smtpserver.protocol.DNSService#getSMTPHostAddresses(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public Iterator<HostAddress> getSMTPHostAddresses(String domainName) throws TemporaryResolutionException {
        try {
            return dns.getSMTPHostAddresses(domainName);
        } catch (org.apache.james.api.dnsservice.TemporaryResolutionException e) {
            throw new TemporaryResolutionException(e.getMessage());
        }
    }

}
