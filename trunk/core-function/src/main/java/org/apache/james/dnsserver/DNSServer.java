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



package org.apache.james.dnsserver;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.api.dnsservice.TemporaryResolutionException;
import org.apache.mailet.HostAddress;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Cache;
import org.xbill.DNS.Credibility;
import org.xbill.DNS.DClass;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.PTRRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.ResolverConfig;
import org.xbill.DNS.ReverseMap;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Provides DNS client functionality to services running
 * inside James
 */
public class DNSServer
    extends AbstractLogEnabled
    implements Configurable, Initializable, org.apache.james.api.dnsservice.DNSService, DNSServerMBean {

    /**
     * A resolver instance used to retrieve DNS records.  This
     * is a reference to a third party library object.
     */
    protected Resolver resolver;

    /**
     * A TTL cache of results received from the DNS server.  This
     * is a reference to a third party library object.
     */
    protected Cache cache;

    /**
     * Maximum number of RR to cache.
     */

    private int maxCacheSize = 50000;

    /**
     * Whether the DNS response is required to be authoritative
     */
    private int dnsCredibility;
    
    /**
     * The DNS servers to be used by this service
     */
    private List<String> dnsServers = new ArrayList<String>();
    
    /**
     * The search paths to be used
     */
    private Name[] searchPaths = null;

    /**
     * The MX Comparator used in the MX sort.
     */
    private Comparator<MXRecord> mxComparator = new MXRecordComparator();

    /**
     * If true than the DNS server will return only a single IP per each MX record
     * when looking up SMTPServers
     */
    private boolean singleIPPerMX;
    
    /**
     * If true register this service as the default resolver/cache for DNSJava static
     * calls
     */
    private boolean setAsDNSJavaDefault;
    
    private String localHostName;
    
    private String localCanonicalHostName;
    
    private String localAddress;
    

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure( final Configuration configuration )
        throws ConfigurationException {

        final boolean autodiscover =
            configuration.getChild( "autodiscover" ).getValueAsBoolean( true );

        List<Name> sPaths = new ArrayList<Name>();
        if (autodiscover) {
            getLogger().info("Autodiscovery is enabled - trying to discover your system's DNS Servers");
            String[] serversArray = ResolverConfig.getCurrentConfig().servers();
            if (serversArray != null) {
                for ( int i = 0; i < serversArray.length; i++ ) {
                    dnsServers.add(serversArray[ i ]);
                    getLogger().info("Adding autodiscovered server " + serversArray[i]);
                }
            }
            Name[] systemSearchPath = ResolverConfig.getCurrentConfig().searchPath();
            if (systemSearchPath != null && systemSearchPath.length > 0) {
                sPaths.addAll(Arrays.asList(systemSearchPath));
            }
            if (getLogger().isInfoEnabled()) {
                for (Iterator<Name> i = sPaths.iterator(); i.hasNext();) {
                    Name searchPath = i.next();
                    getLogger().info("Adding autodiscovered search path " + searchPath.toString());
                }
            }
        }

        singleIPPerMX = configuration.getChild( "singleIPperMX" ).getValueAsBoolean( false ); 

        setAsDNSJavaDefault = configuration.getChild( "setAsDNSJavaDefault" ).getValueAsBoolean( true );
        
        // Get the DNS servers that this service will use for lookups
        final Configuration serversConfiguration = configuration.getChild( "servers" );
        final Configuration[] serverConfigurations =
            serversConfiguration.getChildren( "server" );

        for ( int i = 0; i < serverConfigurations.length; i++ ) {
            dnsServers.add( serverConfigurations[ i ].getValue() );
        }

        // Get the DNS servers that this service will use for lookups
        final Configuration searchPathsConfiguration = configuration.getChild( "searchpaths" );
        final Configuration[] searchPathsConfigurations =
            searchPathsConfiguration.getChildren( "searchpath" );

        for ( int i = 0; i < searchPathsConfigurations.length; i++ ) {
            try {
                sPaths.add( Name.fromString(searchPathsConfigurations[ i ].getValue()) );
            } catch (TextParseException e) {
                throw new ConfigurationException("Unable to parse searchpath host: "+searchPathsConfigurations[ i ].getValue(),e);
            }
        }
        
        searchPaths = (Name[]) sPaths.toArray(new Name[0]);

        if (dnsServers.isEmpty()) {
            getLogger().info("No DNS servers have been specified or found by autodiscovery - adding 127.0.0.1");
            dnsServers.add("127.0.0.1");
        }

        final boolean authoritative =
           configuration.getChild( "authoritative" ).getValueAsBoolean( false );
        // TODO: Check to see if the credibility field is being used correctly.  From the
        //      docs I don't think so
        dnsCredibility = authoritative ? Credibility.AUTH_ANSWER : Credibility.NONAUTH_ANSWER;

        maxCacheSize = (int) configuration.getChild( "maxcachesize" ).getValueAsLong( maxCacheSize );
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize()
        throws Exception {

        getLogger().debug("DNSService init...");

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
        cache.setMaxEntries(maxCacheSize);
        
        if (setAsDNSJavaDefault) {
            Lookup.setDefaultResolver(resolver);
            Lookup.setDefaultCache(cache, DClass.IN);
            Lookup.setDefaultSearchPath(searchPaths);
            getLogger().info("Registered cache, resolver and search paths as DNSJava defaults");
        }
        
        // Cache the local hostname and local address. This is needed because 
        // the following issues:
        // JAMES-787
        // JAMES-302
        InetAddress addr = getLocalHost();
        localCanonicalHostName = addr.getCanonicalHostName();
        localHostName = addr.getHostName();
        localAddress = addr.getHostAddress();
        
        getLogger().debug("DNSService ...init end");
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
     * <p>Return the list of DNS servers in use by this service</p>
     *
     * @return an array of DNS server names
     */
    public Name[] getSearchPaths() {
        return searchPaths;
    }

    
    /**
     * <p>Return a prioritized unmodifiable list of MX records
     * obtained from the server.</p>
     *
     * @param hostname domain name to look up
     *
     * @return a list of MX records corresponding to this mail domain
     * @throws TemporaryResolutionException get thrown on temporary problems
     */
    private List<String> findMXRecordsRaw(String hostname) throws TemporaryResolutionException {
        Record answers[] = lookup(hostname, Type.MX, "MX");
        List<String> servers = new ArrayList<String>();
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
        return servers;
    }
    
    /**
     * @see org.apache.james.api.dnsservice.DNSService#findMXRecords(String)
     */
    public Collection<String> findMXRecords(String hostname) throws TemporaryResolutionException {
        List<String> servers = new ArrayList<String>();
        try {
            servers = findMXRecordsRaw(hostname);
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
                    getByName(hostname);
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
     * @param namestr the name of the host to be looked up
     * @param type the type of record desired
     * @param typeDesc the description of the record type, for debugging purpose
     */
    protected Record[] lookup(String namestr, int type, String typeDesc) throws TemporaryResolutionException {
        // Name name = null;
        try {
            // name = Name.fromString(namestr, Name.root);
            Lookup l = new Lookup(namestr, type);
            
            l.setCache(cache);
            l.setResolver(resolver);
            l.setCredibility(dnsCredibility);
            l.setSearchPath(searchPaths);
            Record[] r = l.run();
            
            try {
                if (l.getResult() == Lookup.TRY_AGAIN) {
                    throw new TemporaryResolutionException(
                            "DNSService is temporary not reachable");
                } else {
                    return r;
                }
            } catch (IllegalStateException ise) {
                // This is okay, because it mimics the original behaviour
                // TODO find out if it's a bug in DNSJava 
                getLogger().debug("Error determining result ", ise);
                throw new TemporaryResolutionException(
                        "DNSService is temporary not reachable");
            }
            
            // return rawDNSLookup(name, false, type, typeDesc);
        } catch (TextParseException tpe) {
            // TODO: Figure out how to handle this correctly.
            getLogger().error("Couldn't parse name " + namestr, tpe);
            return null;
        }
    }
    
    protected Record[] lookupNoException(String namestr, int type, String typeDesc) {
        try {
            return lookup(namestr, type, typeDesc);
        } catch (TemporaryResolutionException e) {
            return null;
        }
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
    private static class MXRecordComparator implements Comparator<MXRecord> {
        private final static Random random = new Random();
        public int compare (MXRecord a, MXRecord b) {
            int pa = a.getPriority();
            int pb = b.getPriority();
            return (pa == pb) ? (512 - random.nextInt(1024)) : pa - pb;
        }
    }

    /**
     * @see org.apache.james.api.dnsservice.DNSService#getSMTPHostAddresses(String)
     */
    public Iterator<HostAddress> getSMTPHostAddresses(final String domainName) throws TemporaryResolutionException {
        return new Iterator<HostAddress>() {
            private Iterator<String> mxHosts = findMXRecords(domainName).iterator();
            private Iterator<HostAddress> addresses = null;

            public boolean hasNext() {
                /* Make sure that when next() is called, that we can
                 * provide a HostAddress.  This means that we need to
                 * have an inner iterator, and verify that it has
                 * addresses.  We could, for example, run into a
                 * situation where the next mxHost didn't have any valid
                 * addresses.
                 */
                if ((addresses == null || !addresses.hasNext()) && mxHosts.hasNext()) do {
                    final String nextHostname = (String)mxHosts.next();
                    InetAddress[] addrs = null;
                    try {
                        if (singleIPPerMX) {
                            addrs = new InetAddress[] {getByName(nextHostname)};
                        } else {
                            addrs = getAllByName(nextHostname);
                        }
                    } catch (UnknownHostException uhe) {
                        // this should never happen, since we just got
                        // this host from mxHosts, which should have
                        // already done this check.
                        StringBuffer logBuffer = new StringBuffer(128)
                                                 .append("Couldn't resolve IP address for discovered host ")
                                                 .append(nextHostname)
                                                 .append(".");
                        getLogger().error(logBuffer.toString());
                    }
                    final InetAddress[] ipAddresses = addrs;

                    addresses = new Iterator<HostAddress>() {
                        int i = 0;

                        public boolean hasNext() {
                            return ipAddresses != null && i < ipAddresses.length;
                        }

                        public HostAddress next() {
                            return new org.apache.mailet.HostAddress(nextHostname, "smtp://" + ipAddresses[i++].getHostAddress());
                        }

                        public void remove() {
                            throw new UnsupportedOperationException ("remove not supported by this iterator");
                        }
                    };
                } while (!addresses.hasNext() && mxHosts.hasNext());

                return addresses != null && addresses.hasNext();
            }

            public HostAddress next() {
                return addresses != null ? addresses.next() : null;
            }

            public void remove() {
                throw new UnsupportedOperationException ("remove not supported by this iterator");
            }
        };
    }

    /* java.net.InetAddress.get[All]ByName(String) allows an IP literal
     * to be passed, and will recognize it even with a trailing '.'.
     * However, org.xbill.DNS.Address does not recognize an IP literal
     * with a trailing '.' character.  The problem is that when we
     * lookup an MX record for some domains, we may find an IP address,
     * which will have had the trailing '.' appended by the time we get
     * it back from dnsjava.  An MX record is not allowed to have an IP
     * address as the right-hand-side, but there are still plenty of
     * such records on the Internet.  Since java.net.InetAddress can
     * handle them, for the time being we've decided to support them.
     *
     * These methods are NOT intended for use outside of James, and are
     * NOT declared by the org.apache.james.services.DNSServer.  This is
     * currently a stopgap measure to be revisited for the next release.
     */

    private static String allowIPLiteral(String host) {
        if ((host.charAt(host.length() - 1) == '.')) {
            String possible_ip_literal = host.substring(0, host.length() - 1);
            if (org.xbill.DNS.Address.isDottedQuad(possible_ip_literal)) {
                host = possible_ip_literal;
            }
        }
        return host;
    }

    /**
     * @see org.apache.james.api.dnsservice.DNSService#getByName(String)
     */
    public InetAddress getByName(String host) throws UnknownHostException {
        String name = allowIPLiteral(host);
         
        try {
            // Check if its local
            if (name.equalsIgnoreCase(localHostName) || name.equalsIgnoreCase(localCanonicalHostName) ||name.equals(localAddress)) {
                return getLocalHost();
            }
            
            return org.xbill.DNS.Address.getByAddress(name);
        } catch (UnknownHostException e) {
            Record[] records = lookupNoException(name, Type.A, "A");

            if (records != null && records.length >= 1) {
                ARecord a = (ARecord) records[0];
                return InetAddress.getByAddress(name, a.getAddress().getAddress());
            } else throw e;
        }
    }

    /**
     * @see org.apache.james.api.dnsservice.DNSService#getAllByName(String)
     */
    public InetAddress[] getAllByName(String host) throws UnknownHostException {
        String name = allowIPLiteral(host);
        try {
            // Check if its local
            if (name.equalsIgnoreCase(localHostName) || name.equalsIgnoreCase(localCanonicalHostName) ||name.equals(localAddress)) {
                return new InetAddress[] {getLocalHost()};
            }
            
            InetAddress addr = org.xbill.DNS.Address.getByAddress(name);
            return new InetAddress[] {addr};
        } catch (UnknownHostException e) {
            Record[] records = lookupNoException(name, Type.A, "A");
            
            if (records != null && records.length >= 1) {
                InetAddress [] addrs = new InetAddress[records.length];
                for (int i = 0; i < records.length; i++) {
                    ARecord a = (ARecord) records[i];
                    addrs[i] = InetAddress.getByAddress(name, a.getAddress().getAddress());
                }
                return addrs;
            } else throw e;
        }
    }
    
    /**
     * @see org.apache.james.api.dnsservice.DNSService#findTXTRecords(String)
     */
    public Collection<String> findTXTRecords(String hostname){
        List<String> txtR = new ArrayList<String>();
        Record[] records = lookupNoException(hostname, Type.TXT, "TXT");
    
        if (records != null) {
           for (int i = 0; i < records.length; i++) {
               TXTRecord txt = (TXTRecord) records[i];
               txtR.add(txt.rdataToString());
           }
        
        }
        return txtR;
    }

    /**
     * @see org.apache.james.api.dnsservice.DNSService#getHostName(java.net.InetAddress)
     */
    public String getHostName(InetAddress addr){
        String result = null;
        Name name = ReverseMap.fromAddress(addr);
        Record[] records = lookupNoException(name.toString(), Type.PTR, "PTR");

        if (records == null) {
            result = addr.getHostAddress();
        } else {
            PTRRecord ptr = (PTRRecord) records[0];
            result = ptr.getTarget().toString();
        }
        return result;
    }

    /**
     * @see org.apache.james.api.dnsservice.DNSService#getLocalHost()
     */
    public InetAddress getLocalHost() throws UnknownHostException {
        return InetAddress.getLocalHost();
    }

}
