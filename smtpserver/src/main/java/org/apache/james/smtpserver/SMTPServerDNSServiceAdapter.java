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
package org.apache.james.smtpserver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.james.lifecycle.api.LogEnabled;
import org.apache.james.protocols.smtp.DNSService;
import org.apache.james.protocols.smtp.TemporaryResolutionException;
import org.apache.james.util.MXHostAddressIterator;
import org.apache.mailet.HostAddress;

public class SMTPServerDNSServiceAdapter implements DNSService, LogEnabled{

    private org.apache.james.dnsservice.api.DNSService dns;
    private Log log;
    
    public SMTPServerDNSServiceAdapter(org.apache.james.dnsservice.api.DNSService dns) {    
        this.dns = dns;
    }
    
    /**
     * @see org.apache.james.protocols.smtp.DNSService#findMXRecords(java.lang.String)
     */
    public Collection<String> findMXRecords(String hostname) throws TemporaryResolutionException {
        try {
            return dns.findMXRecords(hostname);
        } catch (org.apache.james.dnsservice.api.TemporaryResolutionException e) {
            throw new TemporaryResolutionException(e.getMessage());
        }
    }

    /**
     * @see org.apache.james.protocols.smtp.DNSService#findTXTRecords(java.lang.String)
     */
    public Collection<String> findTXTRecords(String hostname) {
        return dns.findTXTRecords(hostname);
    }

    /**
     * @see org.apache.james.protocols.smtp.DNSService#getAllByName(java.lang.String)
     */
    public InetAddress[] getAllByName(String host) throws UnknownHostException {
        return dns.getAllByName(host);
    }

    /**
     * @see org.apache.james.protocols.smtp.DNSService#getByName(java.lang.String)
     */
    public InetAddress getByName(String host) throws UnknownHostException {
        return dns.getByName(host);
    }

    /**
     * @see org.apache.james.protocols.smtp.DNSService#getHostName(java.net.InetAddress)
     */
    public String getHostName(InetAddress addr) {
        return dns.getHostName(addr);
    }

    /**
     * @see org.apache.james.protocols.smtp.DNSService#getLocalHost()
     */
    public InetAddress getLocalHost() throws UnknownHostException {
        return dns.getLocalHost();
    }

    /**
     * @see org.apache.james.protocols.smtp.DNSService#getSMTPHostAddresses(java.lang.String)
     */
    public Iterator<HostAddress> getSMTPHostAddresses(String domainName) throws TemporaryResolutionException {
        return new MXHostAddressIterator(findMXRecords(domainName).iterator(), dns, false, log);
    }

    /*
     * 
     */
    public void setLog(Log log) {
        this.log = log;
    }

}
