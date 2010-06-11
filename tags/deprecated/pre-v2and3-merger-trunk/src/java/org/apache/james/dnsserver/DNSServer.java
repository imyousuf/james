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

package org.apache.james.dnsserver;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.xbill.DNS.Cache;
import org.xbill.DNS.Credibility;
import org.xbill.DNS.DClass;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.FindServer;
import org.xbill.DNS.Message;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.RRset;
import org.xbill.DNS.SetResponse;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Provides DNS client functionality to services running
 * inside James
 */
public class DNSServer
    extends AbstractLogEnabled
    implements Configurable, Initializable,
    org.apache.james.services.DNSServer, DNSServerMBean {

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
     * The DNS servers to be used by this service
     */
    private List dnsServers = new ArrayList();

    /**
     * The MX Comparator used in the MX sort.
     */
    private Comparator mxComparator = new MXRecordComparator();

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure( final Configuration configuration )
        throws ConfigurationException {

        final boolean autodiscover =
            configuration.getChild( "autodiscover" ).getValueAsBoolean( true );

        if (autodiscover) {
            getLogger().info("Autodiscovery is enabled - trying to discover your system's DNS Servers");
            String[] serversArray = FindServer.servers();
            if (serversArray != null) {
                for ( int i = 0; i < serversArray.length; i++ ) {
                    dnsServers.add(serversArray[ i ]);
                    getLogger().info("Adding autodiscovered server " + serversArray[i]);
                }
            }
        }

        // Get the DNS servers that this service will use for lookups
        final Configuration serversConfiguration = configuration.getChild( "servers" );
        final Configuration[] serverConfigurations =
            serversConfiguration.getChildren( "server" );

        for ( int i = 0; i < serverConfigurations.length; i++ ) {
            dnsServers.add( serverConfigurations[ i ].getValue() );
        }

        if (dnsServers.isEmpty()) {
            getLogger().info("No DNS servers have been specified or found by autodiscovery - adding 127.0.0.1");
            dnsServers.add("127.0.0.1");
        }

        final boolean authoritative =
            configuration.getChild( "authoritative" ).getValueAsBoolean( false );
        // TODO: Check to see if the credibility field is being used correctly.  From the
        //       docs I don't think so
        dnsCredibility = authoritative ? Credibility.AUTH_ANSWER : Credibility.NONAUTH_ANSWER;
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize()
        throws Exception {

        getLogger().debug("DNSServer init...");

        // If no DNS servers were configured, default to local host
        if (dnsServers.isEmpty()) {
            try {
                dnsServers.add( InetAddress.getLocalHost().getHostName() );
            } catch ( UnknownHostException ue ) {
                dnsServers.add( "127.0.0.1" );
            }
        }

        //Create the extended resolver...
        final String[] serversArray = (String[])dnsServers.toArray(new String[0]);

        if (getLogger().isInfoEnabled()) {
            for(int c = 0; c < serversArray.length; c++) {
                getLogger().info("DNS Server is: " + serversArray[c]);
            }
        }

        try {
            resolver = new ExtendedResolver( serversArray );
        } catch (UnknownHostException uhe) {
            getLogger().fatalError("DNS service could not be initialized.  The DNS servers specified are not recognized hosts.", uhe);
            throw uhe;
        }

        cache = new Cache (DClass.IN);

        getLogger().debug("DNSServer ...init end");
    }

    /**
     * <p>Return the list of DNS servers in use by this service</p>
     *
     * @return an array of DNS server names
     */
    public String[] getDNSServers() {
        return (String[])dnsServers.toArray(new String[0]);
    }
    
    /**
     * <p>Return a prioritized unmodifiable list of MX records
     * obtained from the server.</p>
     *
     * @param hostname domain name to look up
     *
     * @return a unmodifiable list of MX records corresponding to
     *         this mail domain name
     */
    public Collection findMXRecords(String hostname) {
        Record answers[] = lookup(hostname, Type.MX);
        List servers = new ArrayList();
        try {
            if (answers == null) {
                return servers;
            }

            MXRecord mxAnswers[] = new MXRecord[answers.length];
            for (int i = 0; i < answers.length; i++) {
                mxAnswers[i] = (MXRecord)answers[i];
            }

            Arrays.sort(mxAnswers, mxComparator);

            for (int i = 0; i < mxAnswers.length; i++) {
                servers.add(mxAnswers[i].getTarget ().toString ());
                getLogger().debug(new StringBuffer("Found MX record ").append(mxAnswers[i].getTarget ().toString ()).toString());
            }
            return Collections.unmodifiableCollection(servers);
        } finally {
            //If we found no results, we'll add the original domain name if
            //it's a valid DNS entry
            if (servers.size () == 0) {
                StringBuffer logBuffer =
                    new StringBuffer(128)
                            .append("Couldn't resolve MX records for domain ")
                            .append(hostname)
                            .append(".");
                getLogger().info(logBuffer.toString());
                try {
                    InetAddress.getByName(hostname);
                    servers.add(hostname);
                } catch (UnknownHostException uhe) {
                    // The original domain name is not a valid host,
                    // so we can't add it to the server list.  In this
                    // case we return an empty list of servers
                    logBuffer = new StringBuffer(128)
                                .append("Couldn't resolve IP address for host ")
                                .append(hostname)
                                .append(".");
                    getLogger().error(logBuffer.toString());
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
    public Record[] lookup(String name, int type) {
        return rawDNSLookup(name,false,type);
    }

    /**
     * Looks up DNS records of the specified type for the specified name
     *
     * @param namestr the name of the host to be looked up
     * @param querysent whether the query has already been sent to the DNS servers
     * @param type the type of record desired
     */
    private Record[] rawDNSLookup(String namestr, boolean querysent, int type) {
        Name name = null;
        try {
            name = Name.fromString(namestr, Name.root);
        } catch (TextParseException tpe) {
            // TODO: Figure out how to handle this correctly.
            getLogger().error("Couldn't parse name " + namestr, tpe);
            return null;
        }
        int dclass = DClass.IN;

        SetResponse cached = cache.lookupRecords(name, type, dnsCredibility);
        if (cached.isSuccessful()) {
            getLogger().debug(new StringBuffer(256)
                             .append("Retrieving MX record for ")
                             .append(name).append(" from cache")
                             .toString());

            return processSetResponse(cached);
        }
        else if (cached.isNXDOMAIN() || cached.isNXRRSET()) {
            return null;
        }
        else if (querysent) {
            return null;
        }
        else {
            getLogger().debug(new StringBuffer(256)
                             .append("Looking up MX record for ")
                             .append(name)
                             .toString());
            Record question = Record.newRecord(name, type, dclass);
            Message query = Message.newQuery(question);
            Message response = null;

            try {
                response = resolver.send(query);
            }
            catch (Exception ex) {
                getLogger().warn("Query error!", ex);
                return null;
            }

            int rcode = response.getHeader().getRcode();
            if (rcode == Rcode.NOERROR || rcode == Rcode.NXDOMAIN) {
                cached = cache.addMessage(response);
                if (cached != null && cached.isSuccessful()) {
                    return processSetResponse(cached);
                }
            }

            if (rcode != Rcode.NOERROR) {
                return null;
            }

            return rawDNSLookup(namestr, true, type);
        }
    }
    
    private Record[] processSetResponse(SetResponse sr) {
        Record [] answers;
        int answerCount = 0, n = 0;

        RRset [] rrsets = sr.answers();
        answerCount = 0;
        for (int i = 0; i < rrsets.length; i++) {
            answerCount += rrsets[i].size();
        }

        answers = new Record[answerCount];

        for (int i = 0; i < rrsets.length; i++) {
            Iterator iter = rrsets[i].rrs();
            while (iter.hasNext()) {
                Record r = (Record)iter.next();
                answers[n++] = r;
            }
        }
        return answers;
    }

    /* RFC 2821 section 5 requires that we sort the MX records by their
     * preference, and introduce a randomization.  This Comparator does
     * comparisons as normal unless the values are equal, in which case
     * it "tosses a coin", randomly speaking.
     *
     * This way MX record w/preference 0 appears before MX record
     * w/preference 1, but a bunch of MX records with the same preference
     * would appear in different orders each time.
     *
     * Reminder for maintainers: the return value on a Comparator can
     * be counter-intuitive for those who aren't used to the old C
     * strcmp function:
     *
     * < 0 ==> a < b
     * = 0 ==> a = b
     * > 0 ==> a > b
     */
    private static class MXRecordComparator implements Comparator {
        private final static Random random = new Random();
        public int compare (Object a, Object b) {
            int pa = ((MXRecord)a).getPriority();
            int pb = ((MXRecord)b).getPriority();
            return (pa == pb) ? (512 - random.nextInt(1024)) : pa - pb;
        }
    }
}
