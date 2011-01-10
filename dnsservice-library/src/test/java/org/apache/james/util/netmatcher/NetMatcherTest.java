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
package org.apache.james.util.netmatcher;

import java.net.UnknownHostException;

import junit.framework.TestCase;

import org.apache.james.dnsservice.api.mock.DNSFixture;
import org.apache.james.util.netmatcher.NetMatcher;

/**
 * Test the NetMatcher class with various IPv4 and IPv6 parameters.
 *
 */
public class NetMatcherTest extends TestCase {

    /**
     * 
     */
    private static NetMatcher netMatcher;
    
    /**
     * 
     */
    public void testIpV4NetworksUniqueness() {
        netMatcher = new NetMatcher(DNSFixture.LOCALHOST_IP_V4_ADDRESSES_DUPLICATE, DNSFixture.DNS_SERVER_IPV4_MOCK);
        assertEquals("[172.16.0.0/255.255.0.0, 192.168.1.0/255.255.255.0]", netMatcher.toString());
    }
    
    /**
     * 
     */
    public void testIpV6NetworksUniqueness() {
        netMatcher = new NetMatcher(DNSFixture.LOCALHOST_IP_V6_ADDRESSES_DUPLICATE, DNSFixture.DNS_SERVER_IPV6_MOCK);
        assertEquals("[0:0:0:0:0:0:0:1/32768, 2781:db8:1234:0:0:0:0:0/48]", netMatcher.toString());
    }
    
    /**
     * @throws UnknownHostException 
     */
    public void testIpV4Matcher() throws UnknownHostException {
        
        netMatcher = new NetMatcher(DNSFixture.LOCALHOST_IP_V4_ADDRESSES, DNSFixture.DNS_SERVER_IPV4_MOCK);
        
        assertEquals(true, netMatcher.matchInetNetwork("127.0.0.1"));
        assertEquals(true, netMatcher.matchInetNetwork("localhost"));
        assertEquals(true, netMatcher.matchInetNetwork("172.16.15.254"));
        assertEquals(true, netMatcher.matchInetNetwork("192.168.1.254"));
        assertEquals(false, netMatcher.matchInetNetwork("192.169.1.254"));
    
    }

    /**
     * @throws UnknownHostException 
     */
    public void testIpV4MatcherWithIpV6() throws UnknownHostException {
        
        netMatcher = new NetMatcher(DNSFixture.LOCALHOST_IP_V4_ADDRESSES, DNSFixture.DNS_SERVER_IPV4_MOCK);
        
        assertEquals(false, netMatcher.matchInetNetwork("0:0:0:0:0:0:0:1%0"));
        assertEquals(false, netMatcher.matchInetNetwork("00:00:00:00:00:00:00:1"));
        assertEquals(false, netMatcher.matchInetNetwork("00:00:00:00:00:00:00:2"));
        assertEquals(false, netMatcher.matchInetNetwork("2781:0db8:1234:8612:45ee:ffff:fffe:0001"));
        assertEquals(false, netMatcher.matchInetNetwork("2781:0db8:1235:8612:45ee:ffff:fffe:0001"));
    
    }

    /**
     * @throws UnknownHostException 
     */
    public void testIpV6Matcher() throws UnknownHostException {
        
        netMatcher = new NetMatcher(DNSFixture.LOCALHOST_IP_V6_ADDRESSES, DNSFixture.DNS_SERVER_IPV6_MOCK);
        
        assertEquals(true, netMatcher.matchInetNetwork("0:0:0:0:0:0:0:1%0"));
        assertEquals(true, netMatcher.matchInetNetwork("00:00:00:00:00:00:00:1"));
        assertEquals(false, netMatcher.matchInetNetwork("00:00:00:00:00:00:00:2"));
        assertEquals(true, netMatcher.matchInetNetwork("2781:0db8:1234:8612:45ee:ffff:fffe:0001"));
        assertEquals(false, netMatcher.matchInetNetwork("2781:0db8:1235:8612:45ee:ffff:fffe:0001"));
    
    }

    /**
     * @throws UnknownHostException 
     */
    public void testIpV6MatcherWithIpV4() throws UnknownHostException {
        
        netMatcher = new NetMatcher(DNSFixture.LOCALHOST_IP_V6_ADDRESSES, DNSFixture.DNS_SERVER_IPV6_MOCK);
        
        assertEquals(false, netMatcher.matchInetNetwork("127.0.0.1"));
        assertEquals(false, netMatcher.matchInetNetwork("localhost"));
        assertEquals(false, netMatcher.matchInetNetwork("172.16.15.254"));
        assertEquals(false, netMatcher.matchInetNetwork("192.168.1.254"));
        assertEquals(false, netMatcher.matchInetNetwork("192.169.1.254"));
        
    }

}
