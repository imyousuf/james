/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.dnsserver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.xbill.DNS.Credibility;
import org.xbill.DNS.DClass;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.FindServer;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.Type;
import org.xbill.DNS.dns;

/**
 * Provides DNS client functionality to components running
 * inside James
 *
 * @version 1.0.0, 18/06/2000
 */
public class DNSServer
    extends AbstractLogEnabled
    implements Configurable, Initializable,
    org.apache.james.services.DNSServer {

    private static class PriorityCompare implements Comparator {
        public int compare (Object a, Object b) {
            MXRecord ma = (MXRecord)a;
            MXRecord mb = (MXRecord)b;
            return ma.getPriority () - mb.getPriority ();
        }
    }

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

 
        // TODO@ Maybe add some more config items to control DNS lookup retries and
        // timeouts

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

 
        /**
         * A resolver instance used to retrieve DNS records.  This
         * is a reference to a third party library object.
         */
        Resolver resolver = null;

        /**
        * Holds the list of dns servers used to create the new ExtendedResolver instance
        */
        String[] serversArray;

        getLogger().info("DNSServer init...");

        // If no DNS servers were configured, try and get the underlying systems dns servers
        // otherwise use 127.0.0.1
        // Currently, this works if either the appropriate properties are set, the OS has a unix-like /etc/resolv.conf,
        // or the system is Windows based with ipconfig or winipcfg.
        if (dnsServers.isEmpty()) {
            getLogger().info("Trying to locate your systems DNS Servers");
            serversArray = FindServer.servers();
            if (serversArray == null) {
                getLogger().info("No DNS servers found, defaulting to 127.0.0.1");
                serversArray = new String[]{"127.0.0.1"};
            }
        } else {
            //Create the extended resolver using config data
            serversArray = (String[])dnsServers.toArray(new String[0]);
        }

        try {
            resolver = new ExtendedResolver( serversArray );
        } catch (UnknownHostException uhe) {
            getLogger().fatalError("DNS service could not be initialized.  The DNS servers specified are not recognized hosts.", uhe);
            throw uhe;
        }

        if (getLogger().isInfoEnabled()) {
            for(int i = 0; i < serversArray.length; i++) {
                getLogger().info("DNS Server is: " + serversArray[i]);
            }
        }

        // set the resolver for the static class dns
        dns.setResolver(resolver);

        getLogger().info("DNSServer ...init end");
    }

    /**
     * <p>Return a prioritized list of MX records
     * obtained from the server.</p>
     *
     * @param the domain name to look up
     *
     * @return a list of MX records corresponding to
     *         this mail domain name
     */
    public Collection findMXRecords(String hostname) {
        Record answers[] = dns.getRecords(hostname, Type.MX,  DClass.IN,  dnsCredibility);

        List servers = new ArrayList();
        try {
            if (answers == null) {
                return servers;
            }

            MXRecord mxAnswers[] = new MXRecord[answers.length];
            for (int i = 0; i < answers.length; i++) {
                mxAnswers[i] = (MXRecord)answers[i];
            }

            Comparator prioritySort = new PriorityCompare ();
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
}
