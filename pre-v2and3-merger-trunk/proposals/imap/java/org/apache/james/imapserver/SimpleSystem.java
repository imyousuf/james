/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.imapserver;

import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.james.imapserver.AuthenticationException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A simple, single-server, implementation of IMAPSystem.
 *
 * References: rfc 2060, rfc 2193, rfc 2221
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1 on 14 Dec 2000
 */
public class SimpleSystem
    implements IMAPSystem, Component, Initializable {

    private static final String namespaceToken = "#";
    private static final String hierarchySeparator = ".";
    private static final String namespace
        = "((\"#mail.\" \".\")) ((\"#users.\" \".\")) ((\"#shared.\" \".\"))";

    private static String singleServer;
    private Set servers = new HashSet();

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {
        String hostName = null;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch  (UnknownHostException ue) {
            hostName = "localhost";
        }
        singleServer = hostName;
        servers.add(singleServer);
    }

    /**
     * Returns the token indicating a namespace.  Implementation dependent but
     * by convention, '#'.
     * Example: #news.org.apache vs #mail.org.apache
     */
    public String getNamespaceToken() {
        return namespaceToken;
    }

    /**
     * Returns the home server (server with user's INBOX) for specified user.
     * Enables Login Referrals per RFC2221. (Ie user attempts to login to a
     * server which is not their Home Server.)  The returned string must comply
     * with RFC2192, IMAP URL Scheme.
     *
     * @param username String representation of a user
     * @return String holding an IMAP URL for the user's home server
     * @throws AuthenticationException if this System does not recognise
     * the user.
     */
    public String getHomeServer(String username)
        throws AuthenticationException {
        return singleServer;
    }

    /**
     * Returns the character used as a mail hierarchy separator in a given
     * namespace. A namespace must use the same separator at all levels of
     * hierarchy.
     * <p>Recommendations (from rfc 2683) are period (US)/ full stop (Brit),
     * forward slash or backslash.
     *
     * @param namespace String identifying a namespace
     * @return char, usually '.', '/', or '\'
     */
    public String getHierarchySeperator(String namespace) {
        return hierarchySeparator;
    }

    /**
     * Provides the set of namesapces a given user can access. Implementations
     * should but are not required to reveal all namespaces that a user can
     * access. Different namespaces may be handled by different
     * <code>IMAPHosts</code>
     *
     * @param username String identifying a user of this System
     * @return String whose contents should be a space seperated triple
     * <personal namespaces(s)> space <other users' namespace(s)> space
     * <shared namespace(s)>, per RFC2342
     */
    public String getNamespaces(String username) {
        return namespace;
    }

    /**
     * Returns an iterator over the collection of servers on which this user
     * has access. The collection should be unmodifiable.
     * Enable Mailbox Referrals - RFC 2193.
     *
     * @param username String identifying a user
     * @return iterator over a collection of strings
     */
    public Iterator getAccessibleServers(String username) {
        return Collections.unmodifiableSet(servers).iterator();
    }
}
