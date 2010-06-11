/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.dnsserver;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.xbill.DNS.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Provides DNS client functionality to components running
 * inside James
 *
 * @version 1.0.0, 18/06/2000
 * @author  Serge Knystautas <sergek@lokitech.com>
 */
public class DNSServer
    extends AbstractLogEnabled
    implements Configurable, Initializable,
    org.apache.james.services.DNSServer {

    /**
     * A resolver instance used to retrieve DNS records.  This
     * is a reference to a third party library object.
     */
    private Resolver resolver;

    /**
     * A TTL cache of results received from the DNS server.  This
     * is a reference to a third party library object.
     */
    private Cache cache;

    /**
     * Whether the DNS response is required to be authoritative
     */
    private byte dnsCredibility;

    /**
     * The DNS servers to be used by this component
     */
    private Collection dnsServers = new Vector();

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure( final Configuration configuration )
        throws ConfigurationException {

        // Get the DNS servers that this service will use for lookups
        final Configuration serversConfiguration = configuration.getChild( "servers" );
        final Configuration[] serverConfigurations =
            serversConfiguration.getChildren( "server" );

        for ( int i = 0; i < serverConfigurations.length; i++ ) {
            dnsServers.add( serverConfigurations[ i ].getValue() );
        }

        final boolean authoritative =
            configuration.getChild( "authoritative" ).getValueAsBoolean( false );
        dnsCredibility = authoritative ? Credibility.AUTH_ANSWER : Credibility.NONAUTH_ANSWER;
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize()
        throws Exception {

        getLogger().info("DNSServer init...");

        // If no DNS servers were configured, default to local host
        if (dnsServers.isEmpty()) {
            try {
                dnsServers.add( InetAddress.getLocalHost().getHostName() );
            } catch ( UnknownHostException ue ) {
                dnsServers.add( "127.0.0.1" );
            }
        }

        if (getLogger().isInfoEnabled()) {
            for (Iterator i = dnsServers.iterator(); i.hasNext(); ) {
                getLogger().info("DNS Server is: " + i.next());
            }
        }

        //Create the extended resolver...
        final String serversArray[] = (String[])dnsServers.toArray(new String[0]);
        try {
            resolver = new ExtendedResolver( serversArray );
        } catch (UnknownHostException uhe) {
            getLogger().fatalError("DNS service could not be initialized.  The DNS servers specified are not recognized hosts.", uhe);
            throw uhe;
        }

        cache = new Cache (DClass.IN);

        getLogger().info("DNSServer ...init end");
    }

    /**
     * <p>Return a prioritized list of MX records
     * obtained from the server.</p>
     *
     * <p>TODO: This should actually return a List, not
     * a Collection.</p>
     *
     * @param the domain name to look up
     *
     * @return a list of MX records corresponding to
     *         this mail domain name
     */
    public Collection findMXRecords(String hostname) {
        Record answers[] = lookup(hostname, Type.MX);

        // TODO: Determine why this collection is synchronized
        Collection servers = new Vector();
        try {
            if (answers == null) {
                return servers;
            }

            MXRecord mxAnswers[] = new MXRecord[answers.length];
            for (int i = 0; i < answers.length; i++) {
                mxAnswers[i] = (MXRecord)answers[i];
            }

            // TODO: Convert this to a static class instance
            //       No need to pay the object creation cost
            //       on each call
            Comparator prioritySort = new Comparator () {
                    public int compare (Object a, Object b) {
                        MXRecord ma = (MXRecord)a;
                        MXRecord mb = (MXRecord)b;
                        return ma.getPriority () - mb.getPriority ();
                    }
                };

            Arrays.sort(mxAnswers, prioritySort);

            for (int i = 0; i < mxAnswers.length; i++) {
                servers.add(mxAnswers[i].getTarget ().toString ());
            }
            return servers;
        } finally {
            //If we found no results, we'll add the original domain name if
            //it's a valid DNS entry
            if (servers.size () == 0) {
                try {
                    InetAddress.getByName(hostname);
                    servers.add(hostname);
                } catch (UnknownHostException uhe) {
                    // The original domain name is not a valid host,
                    // so we can't add it to the server list.  In this
                    // case we return an empty list of servers
                }
            }
        }
    }

    /**
     * Looks up DNS records of the specified type for the specified name.
     *
     * This method is a public wrapper for the private implementation 
     * method
     *
     * @param name the name of the host to be looked up
     * @param type the type of record desired
     */
    public Record[] lookup(String name, short type) {
        return rawDNSLookup(name,false,type);
    }

    /**
     * Looks up DNS records of the specified type for the specified name
     *
     * @param name the name of the host to be looked up
     * @param querysent whether the query has already been sent to the DNS servers
     * @param type the type of record desired
     */
    private Record[] rawDNSLookup(String namestr, boolean querysent, short type) {
        Name name = new Name(namestr);
        short dclass = DClass.IN;

        Record [] answers;
        int answerCount = 0, n = 0;
        Enumeration e;

        SetResponse cached = cache.lookupRecords(name, type, dnsCredibility);
        if (cached.isSuccessful()) {
            RRset [] rrsets = cached.answers();
            answerCount = 0;
            for (int i = 0; i < rrsets.length; i++) {
                answerCount += rrsets[i].size();
            }

            answers = new Record[answerCount];

            for (int i = 0; i < rrsets.length; i++) {
                e = rrsets[i].rrs();
                while (e.hasMoreElements()) {
                    Record r = (Record)e.nextElement();
                    answers[n++] = r;
                }
            }
        }
        else if (cached.isNXDOMAIN() || cached.isNXRRSET()) {
            return null;
        }
        else if (querysent) {
            return null;
        }
        else {
            Record question = Record.newRecord(name, type, dclass);
            org.xbill.DNS.Message query = org.xbill.DNS.Message.newQuery(question);
            org.xbill.DNS.Message response;

            try {
                response = resolver.send(query);
            }
            catch (Exception ex) {
                return null;
            }

            short rcode = response.getHeader().getRcode();
            if (rcode == Rcode.NOERROR || rcode == Rcode.NXDOMAIN) {
                cache.addMessage(response);
            }

            if (rcode != Rcode.NOERROR) {
                return null;
            }

            return rawDNSLookup(namestr, true, type);
        }

        return answers;
    }
}
