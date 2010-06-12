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

package org.apache.james.api.dnsservice.util;

import java.net.InetAddress;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.james.api.dnsservice.DNSService;

/**
 * Class which can be used to check if an ipAddress match a network
 */
public class NetMatcher
{
    private DNSService dnsServer;
    
    private ArrayList<InetNetwork> networks;


    /**
     * Init the class with the given networks 
     *
     * @param nets a Collection which holds all networks
     */
    public void initInetNetworks(final Collection<String> nets)
    {
        networks = new ArrayList<InetNetwork>();
        
        InetNetwork in = new InetNetwork(dnsServer);
        
        for (Iterator<String> iter = nets.iterator(); iter.hasNext(); ) try
        {
            InetNetwork net = in.getFromString(iter.next());
            if (!networks.contains(net)) networks.add(net);
        }
        catch (java.net.UnknownHostException uhe)
        {
            log("Cannot resolve address: " + uhe.getMessage());
        }
        networks.trimToSize();
    }

    /**
     * Init the class with the given networks 
     *
     * @param nets a String[] which holds all networks
     */
    public void initInetNetworks(final String[] nets)
    {
        
        networks = new ArrayList<InetNetwork>();
        
        InetNetwork in = new InetNetwork(dnsServer);
        
        for (int i = 0; i < nets.length; i++) try
        {
            InetNetwork net = in.getFromString(nets[i]);
            if (!networks.contains(net)) networks.add(net);
        }
        catch (java.net.UnknownHostException uhe)
        {
            log("Cannot resolve address: " + uhe.getMessage());
        }
        networks.trimToSize();
    }

    /**
     * Return true if passed host match a network which was used to init the Netmatcher
     * 
     * @param hostIP the ipAddress or hostname to check
     * @return true if match the network
     */
    public boolean matchInetNetwork(final String hostIP)
    {
        InetAddress ip = null;

        try
        {
            ip = dnsServer.getByName(hostIP);
        }
        catch (java.net.UnknownHostException uhe)
        {
            log("Cannot resolve address for " + hostIP + ": " + uhe.getMessage());
        }

        boolean sameNet = false;

        if (ip != null) for (Iterator<InetNetwork> iter = networks.iterator(); (!sameNet) && iter.hasNext(); )
        {
            InetNetwork network = iter.next();
            sameNet = network.contains(ip);
        }
        return sameNet;
    }

    /**
     * @see #matchInetNetwork(String)
     */
    public boolean matchInetNetwork(final InetAddress ip)
    {
        boolean sameNet = false;

        for (Iterator<InetNetwork> iter = networks.iterator(); (!sameNet) && iter.hasNext(); )
        {
            InetNetwork network = iter.next();
            sameNet = network.contains(ip);
        }
        return sameNet;
    }

    /**
     * Create a new instance of Netmatcher
     * 
     * @param nets a String[] which holds all networks
     * @param dnsServer the DNSService which will be used in this class
     */
    public NetMatcher(final String[] nets,DNSService dnsServer)
    {
        this.dnsServer = dnsServer;
        initInetNetworks(nets);
    }

    /**
     * Create a new instance of Netmatcher
     * 
     * @param nets a Collection which holds all networks
     * @param dnsServer the DNSService which will be used in this class
     */ 
    public NetMatcher(final Collection<String> nets,DNSService dnsServer)
    {
        this.dnsServer = dnsServer;
        initInetNetworks(nets);
    }

    /**
     * @see InetNetwork#toString()
     */
    public String toString() {
        return networks.toString();
    }

    /**
     * Can be overwritten for loggin
     * 
     * @param s the String to log
     */
    protected void log(String s) { }
}
