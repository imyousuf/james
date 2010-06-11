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

package org.apache.james.services;

import java.util.Collection;
import java.util.Iterator;

/**
 * Provides abstraction for DNS resolutions. The interface is Mail specific.
 * It may be a good idea to make the interface more generic or expose 
 * commonly needed DNS methods.
 *
 */
public interface DNSServer {

    /**
     * The component role used by components implementing this service
     */
    String ROLE = "org.apache.james.services.DNSServer";

    /**
     * <p>Get a priority-sorted collection of DNS MX records for a given hostname</p>
     *
     * <p>TODO: Change this to a list, as not all collections are sortable</p>
     *
     * @param hostname the hostname to check
     * @return collection of strings representing MX record values. 
     */
    Collection findMXRecords(String hostname);


    /**
     * Performs DNS lookups as needed to find servers which should or might
     * support SMTP.  Returns one SMTPHostAddresses for each such host
     * discovered by DNS.  If no host is found for domainName, the Iterator
     * returned will be empty and the first call to hasNext() will return
     * false.
     * @param domainName the String domain for which SMTP host addresses are
     * sought.
     * @return an Enumeration in which the Objects returned by next()
     * are instances of SMTPHostAddresses.
     */
    Iterator getSMTPHostAddresses(String domainName);
    
}
