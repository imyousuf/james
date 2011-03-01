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
package org.apache.james.dnsservice.library;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;

import org.apache.james.dnsservice.api.DNSService;
import org.apache.mailet.HostAddress;
import org.slf4j.Logger;

/**
 * 
 *
 */
public class MXHostAddressIterator implements Iterator<HostAddress>{

    
    private Iterator<HostAddress> addresses = null;
    private Iterator<String> hosts;
    private DNSService dns;
    private boolean useSingleIP;
    private Logger logger;
    private int defaultPort;

    public MXHostAddressIterator(Iterator<String> hosts, DNSService dns, boolean useSingleIP, Logger logger) {
        this(hosts, 25, dns, useSingleIP, logger);
    }
    

    public MXHostAddressIterator(Iterator<String> hosts, int defaultPort, DNSService dns, boolean useSingleIP, Logger logger) {
        this.hosts = hosts;
        this.dns = dns;
        this.useSingleIP = useSingleIP;
        this.logger = logger;
        this.defaultPort = defaultPort;
    }
    
    /*
     * (non-Javadoc)
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        /* Make sure that when next() is called, that we can
         * provide a HostAddress.  This means that we need to
         * have an inner iterator, and verify that it has
         * addresses.  We could, for example, run into a
         * situation where the next mxHost didn't have any valid
         * addresses.
         */
        if ((addresses == null || !addresses.hasNext()) && hosts.hasNext()) do {
            String nextHostname = (String)hosts.next();
            final String hostname;
            final String port;
            

            int idx = nextHostname.indexOf(':');
            if ( idx > 0) {
                port = nextHostname.substring(idx+1);
                hostname = nextHostname.substring(0,idx);
            } else {
                hostname = nextHostname;
                port = defaultPort + "";
            }
            
            InetAddress[] addrs = null;
            try {
                if (useSingleIP) {
                    addrs = new InetAddress[] {dns.getByName(hostname)};
                } else {
                    addrs = dns.getAllByName(hostname);
                }
            } catch (UnknownHostException uhe) {
                // this should never happen, since we just got
                // this host from mxHosts, which should have
                // already done this check.
                StringBuffer logBuffer = new StringBuffer(128)
                                         .append("Couldn't resolve IP address for discovered host ")
                                         .append(hostname)
                                         .append(".");
                logger.error(logBuffer.toString());
            }
            final InetAddress[] ipAddresses = addrs;

            addresses = new Iterator<HostAddress>() {
                int i = 0;

                public boolean hasNext() {
                    return ipAddresses != null && i < ipAddresses.length;
                }

                public HostAddress next() {
                    return new org.apache.mailet.HostAddress(hostname, "smtp://" + ipAddresses[i++].getHostAddress() +":" + port);
                }

                public void remove() {
                    throw new UnsupportedOperationException ("remove not supported by this iterator");
                }
            };
        } while (!addresses.hasNext() && hosts.hasNext());

        return addresses != null && addresses.hasNext();
    }

    /*
     * (non-Javadoc)
     * @see java.util.Iterator#next()
     */
    public HostAddress next() {
        return addresses != null ? addresses.next() : null;
    }

    /**
     * Not supported. 
     */
    public void remove() {
        throw new UnsupportedOperationException ("remove not supported by this iterator");
    }

}
