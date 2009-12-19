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



package org.apache.james.smtpserver.protocol;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.mailet.HostAddress;


/**
 * Provides abstraction for DNS resolutions. The interface is Mail specific.
 * It may be a good idea to make the interface more generic or expose 
 * commonly needed DNS methods.
 *
 */
public interface DNSService {

    /**
     * The component role used by components implementing this service
     */
     String ROLE = "org.apache.james.smtpserver.protocol.DNSService";

    /**
     * <p>Return a prioritized unmodifiable list of host handling mail
     * for the domain.</p>
     * 
     * <p>First lookup MX hosts, then MX hosts of the CNAME adress, and
     * if no server is found return the IP of the hostname</p>
     *
     * @param hostname domain name to look up
     *
     * @return a unmodifiable list of handling servers corresponding to
     *         this mail domain name
     * @throws TemporaryResolutionException get thrown on temporary problems 
     */
    Collection<String> findMXRecords(String hostname) throws TemporaryResolutionException;

    /**
     * Get a collection of DNS TXT Records
     * 
     * @param hostname The hostname to check
     * @return collection of strings representing TXT record values
     */
    Collection<String> findTXTRecords(String hostname);


    /**
     * Returns an Iterator over org.apache.mailet.HostAddress, a
     * specialized subclass of javax.mail.URLName, which provides
     * location information for servers that are specified as mail
     * handlers for the given hostname.  This is done using MX records,
     * and the HostAddress instances are returned sorted by MX priority.
     * If no host is found for domainName, the Iterator returned will be
     * empty and the first call to hasNext() will return false.  The
     * Iterator is a nested iterator: the outer iteration is over the
     * results of the MX record lookup, and the inner iteration is over
     * potentially multiple A records for each MX record.  DNS lookups
     * are deferred until actually needed.
     *
     * @since v2.2.0a16-unstable
     * @param domainName - the domain for which to find mail servers
     * @return an Iterator over HostAddress instances, sorted by priority
     * @throws TemporaryResolutionException get thrown on temporary problems
     */
    Iterator<HostAddress> getSMTPHostAddresses(String domainName) throws TemporaryResolutionException;
    
    /**
     * @see java.net.InetAddress#getAllByName(String)
     */
    public InetAddress[] getAllByName(String host) throws UnknownHostException;
 
    /**
     * @see java.net.InetAddress#getByName(String)
     */
    public InetAddress getByName(String host) throws UnknownHostException;

    /**
     * @see org.xbill.DNS.Address#getHostName(InetAddress)
     */
    public String getHostName(InetAddress addr);
    
    /**
     */
    public InetAddress getLocalHost() throws UnknownHostException;
}
