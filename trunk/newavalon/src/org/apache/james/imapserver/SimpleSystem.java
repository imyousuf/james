/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.imapserver;


import java.util.*;
import org.apache.avalon.*;

/**
 * A simple, single-server, implementation of IMAPSystem.
 *
 * References: rfc 2060, rfc 2193, rfc 2221
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1 on 14 Dec 2000
 */

public class SimpleSystem implements IMAPSystem, Component, Initializable {

    private static final String namespaceToken = "#";
    private static final String hierarchySeperator = ".";
    private static final String namespace
	= "((\"#mail.\" \".\")) ((\"#users.\" \".\")) ((\"#shared.\" \".\"))";

    private static String singleServer;
    private Set servers = new HashSet();
    private Context context;
    private Configuration conf;
    private ComponentManager compMgr;


    /**
     * Constructor
     *
     * @param host String this machine.
     */
    public SimpleSystem() {
    }

    public void configure(Configuration conf) throws ConfigurationException {
        this.conf = conf;
    }

    public void contextualize(Context context) {
        this.context = context;
    }

    public void compose(ComponentManager comp) {
        compMgr = comp;
    }

    public void init() {
	// Derive namespace and namespaceToken from conf
	singleServer = (String) context.get("HostName");
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
     * @returns String holding an IMAP URL for the user's home server
     * @throws AuthenticationException if this System does not recognise
     * the user.
     */
    public String getHomeServer(String username)
	throws AuthenticationException {
	return singleServer;
    }


    /**
     * Returns the character used as a mail hierarchy seperator in a given
     * namespace. A namespace must use the same seperator at all levels of
     * hierarchy.
     * <p>Recommendations (from rfc 2683) are period (US)/ full stop (Brit),
     * forward slash or backslash.
     *
     * @param namespace String identifying a namespace
     * @returns char, usually '.', '/', or '\'
     */
    public String getHierarchySeperator(String namespace) {
	return hierarchySeperator;
    }


    /**
     * Provides the set of namesapces a given user can access. Implementations
     * should but are not required to reveal all namespaces that a user can
     * access. Different namespaces may be handled by different
     * <code>IMAPHosts</code>
     *
     * @param username String identifying a user of this System
     * @returns String whose contents should be a space seperated triple 
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

    public void destroy() {
    }


}




