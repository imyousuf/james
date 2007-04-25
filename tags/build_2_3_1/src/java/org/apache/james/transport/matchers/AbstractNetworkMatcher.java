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

package org.apache.james.transport.matchers;

import org.apache.james.util.NetMatcher;
import javax.mail.MessagingException;
import java.util.StringTokenizer;
import java.util.Collection;

/**
  * AbstractNetworkMatcher makes writing IP Address matchers easier.
  *
  * AbstractNetworkMatcher provides a means for checking to see whether
  * a particular IP address or domain is within a set of subnets
  * These subnets may be expressed in one of several formats:
  * 
  *     Format                          Example
  *     explicit address                127.0.0.1
  *     address with a wildcard         127.0.0.*
  *     domain name                     myHost.com
  *     domain name + prefix-length     myHost.com/24
  *     domain name + mask              myHost.com/255.255.255.0
  *     IP address + prefix-length      127.0.0.0/8
  *     IP + mask                       127.0.0.0/255.0.0.0
  *
  * For more information, see also: RFC 1518 and RFC 1519.
  * 
  * @version $ID$
  */
public abstract class AbstractNetworkMatcher extends org.apache.mailet.GenericMatcher {

    /**
     * This is a Network Matcher that should be configured to contain
     * authorized networks
     */
    private NetMatcher authorizedNetworks = null;

    public void init() throws MessagingException {
        Collection nets = allowedNetworks();
        if (nets != null) {
            authorizedNetworks = new NetMatcher() {
                protected void log(String s) {
                    AbstractNetworkMatcher.this.log(s);
                }
            };
            authorizedNetworks.initInetNetworks(allowedNetworks());
            log("Authorized addresses: " + authorizedNetworks.toString());
        }
    }

    protected Collection allowedNetworks() {
        Collection networks = null;
        if (getCondition() != null) {
            StringTokenizer st = new StringTokenizer(getCondition(), ", ", false);
            networks = new java.util.ArrayList();
            while (st.hasMoreTokens()) networks.add(st.nextToken());
        }
        return networks;
    }

    protected boolean matchNetwork(java.net.InetAddress addr) {
        return authorizedNetworks == null ? false : authorizedNetworks.matchInetNetwork(addr);
    }

    protected boolean matchNetwork(String addr) {
        return authorizedNetworks == null ? false : authorizedNetworks.matchInetNetwork(addr);
    }
}
