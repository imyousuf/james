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
package org.apache.james.mailetcontainer.camel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import org.apache.james.dnsservice.api.TemporaryResolutionException;
import org.apache.james.dnsservice.library.MXHostAddressIterator;
import org.apache.james.mailetcontainer.lib.JamesMailetContext;
import org.apache.mailet.HostAddress;

public class CamelMailetContext extends JamesMailetContext {


    /**
     * Performs DNS lookups as needed to find servers which should or might
     * support SMTP. Returns an Iterator over HostAddress, a specialized
     * subclass of javax.mail.URLName, which provides location information for
     * servers that are specified as mail handlers for the given hostname. This
     * is done using MX records, and the HostAddress instances are returned
     * sorted by MX priority. If no host is found for domainName, the Iterator
     * returned will be empty and the first call to hasNext() will return false.
     * 
     * @see org.apache.james.dnsservice.api.DNSService#getSMTPHostAddresses(String)
     * @since Mailet API v2.2.0a16-unstable
     * @param domainName
     *            - the domain for which to find mail servers
     * @return an Iterator over HostAddress instances, sorted by priority
     */
    public Iterator<HostAddress> getSMTPHostAddresses(String domainName) {
        try {
            return new MXHostAddressIterator(dns.findMXRecords(domainName).iterator(), dns, false, log);
        } catch (TemporaryResolutionException e) {
            // TODO: We only do this to not break backward compatiblity. Should
            // fixed later
            return Collections.unmodifiableCollection(new ArrayList<HostAddress>(0)).iterator();
        }
    }

}
