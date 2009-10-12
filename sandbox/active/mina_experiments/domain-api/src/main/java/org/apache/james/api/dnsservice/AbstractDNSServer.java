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



package org.apache.james.api.dnsservice;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Iterator;

/**
 * Abstract class to simplify the mocks
 */
public class AbstractDNSServer implements DNSService {

    /**
     * @see org.apache.james.api.dnsservice.DNSService#findMXRecords(String)
     */
    public Collection findMXRecords(String hostname) {
        throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.api.dnsservice.DNSService#findTXTRecords(String)
     */
    public Collection findTXTRecords(String hostname) {
        throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.api.dnsservice.DNSService#getAllByName(String)
     */
    public InetAddress[] getAllByName(String host) throws UnknownHostException {
        throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.api.dnsservice.DNSService#getByName(String)
     */
    public InetAddress getByName(String host) throws UnknownHostException {
        throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.api.dnsservice.DNSService#getSMTPHostAddresses(String)
     */
    public Iterator getSMTPHostAddresses(String domainName) {
        throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.api.dnsservice.DNSService#getHostName(java.net.InetAddress)
     */
    public String getHostName(InetAddress addr) {
        throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.api.dnsservice.DNSService#getLocalHost()
     */
    public InetAddress getLocalHost() throws UnknownHostException {
        throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

}
