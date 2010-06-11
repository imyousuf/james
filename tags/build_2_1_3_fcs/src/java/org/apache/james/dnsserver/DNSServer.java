/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
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
