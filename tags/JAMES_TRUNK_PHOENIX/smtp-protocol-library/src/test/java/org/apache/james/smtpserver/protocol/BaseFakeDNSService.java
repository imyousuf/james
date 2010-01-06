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

public class BaseFakeDNSService implements DNSService{

    public Collection<String> findMXRecords(String hostname) throws TemporaryResolutionException {
        throw new UnsupportedOperationException("Unimplemented in mock");

    }

    public Collection<String> findTXTRecords(String hostname) {
        throw new UnsupportedOperationException("Unimplemented in mock");

    }

    public InetAddress[] getAllByName(String host) throws UnknownHostException {
        throw new UnsupportedOperationException("Unimplemented in mock");

    }

    public InetAddress getByName(String host) throws UnknownHostException {
        throw new UnsupportedOperationException("Unimplemented in mock");

    }

    public String getHostName(InetAddress addr) {
        throw new UnsupportedOperationException("Unimplemented in mock");

    }

    public InetAddress getLocalHost() throws UnknownHostException {
        throw new UnsupportedOperationException("Unimplemented in mock");

    }

    public Iterator<HostAddress> getSMTPHostAddresses(String domainName) throws TemporaryResolutionException {
        throw new UnsupportedOperationException("Unimplemented in mock");

    }

}
