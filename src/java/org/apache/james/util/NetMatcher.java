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

package org.apache.james.util;

import java.net.InetAddress;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;

public class NetMatcher
{
    ArrayList networks;

    public void initInetNetworks(final Collection nets)
    {
        networks = new ArrayList();
        for (Iterator iter = networks.iterator(); iter.hasNext(); ) try
        {
            networks.add(InetNetwork.getFromString((String) iter.next()));
        }
        catch (java.net.UnknownHostException uhe)
        {
            log("Cannot resolve address: " + uhe.getMessage());
        }
        networks.trimToSize();
    }

    public void initInetNetworks(final String[] nets)
    {
        networks = new ArrayList();
        for (int i = 0; i < nets.length; i++) try
        {
            networks.add(InetNetwork.getFromString(nets[i]));
        }
        catch (java.net.UnknownHostException uhe)
        {
            log("Cannot resolve address: " + uhe.getMessage());
        }
        networks.trimToSize();
    }

    public boolean matchInetNetwork(final String hostIP, final boolean logging)
    {
        InetAddress ip = null;

        try
        {
            ip = InetAddress.getByName(hostIP);
        }
        catch (java.net.UnknownHostException uhe)
        {
            log("Cannot resolve address: " + uhe.getMessage());
        }

        boolean sameNet = false;

        for (Iterator iter = networks.iterator(); (!sameNet || logging) && iter.hasNext(); )
        {
            InetNetwork network = (InetNetwork) iter.next();
            sameNet = network.contains(ip);
            if (logging) log(hostIP + " is" + (sameNet ? "    " : " not") + " contained by " + "Network " + network);
        }
        return sameNet;
    }

    public boolean matchInetNetwork(final InetAddress ip, final boolean logging)
    {
        boolean sameNet = false;

        for (Iterator iter = networks.iterator(); (!sameNet || logging) && iter.hasNext(); )
        {
            InetNetwork network = (InetNetwork) iter.next();
            sameNet = network.contains(ip);
            if (logging) log(ip + " is" + (sameNet ? "    " : " not") + " contained by " + "Network " + network);
        }
        return sameNet;
    }

    public NetMatcher()
    {
        initInetNetworks(valnets);
    }

    public NetMatcher(final String[] nets)
    {
        initInetNetworks(valnets);
    }

    /* ---------------------- TEST CODE BELOW  ----------------------*/

    public static void main(String[] args)
    {
        if (args.length == 0) args = new String[] {
            "127.0.0.1",
            "192.168.1.7",
            "192.168.1.10",
            "mail.devtech.com",
            "devtech.dyndns.org",
            "cvs.apache.org",
            "www.apache.org",
            "nagoya.apache.org",
            "moof.apache.org",
            "www.php.net"
        };

        System.out.println("Initializing InetNetwork");
        System.out.println("---------------------");
        System.out.println();

        NetMatcher matcher = new NetMatcher() {
            protected void log(String s)
            {
                System.err.println(s);
            }
        };
        matcher.initInetNetworks(valnets);
        matcher.validate(args);
        matcher.initInetNetworks(benchnets);
        matcher.benchmark(args);
    }

    protected void log(String s) { }

    private static final String[] valnets = new String[] { 
        "127.0.0.1",
        "192.168.0.0/16",
        "208.*",
        "208.185.*",
        "208.185.179.*",
        "208.185.179.012",
        "208.185.179.012/0",
        "208.000.000.000/8",
        "208.185.000.000/16",
        "208.185.179.000/24",
        "208.185.179.012/32",
        "208.185.179.012/27",
        "208.185.179.012/8",
        "208.185.179.012/16",
        "208.185.179.012/24",
        "208.185.179.012/32",
        "208.185.179.012/27",
        "apache.org",
        "apache.org/255.255.255.224",
        "apache.org/0",
        "apache.org/8",
        "apache.org/16",
        "apache.org/24",
        "apache.org/32",
        "apache.org/27",
        "208.185.179.12/255.255.255.224",
        "127.0.0.1/8"
    };

    private static final String[] benchnets = new String[] { 
        "127.0.0.1/8",
        "192.168.0.0/16"
    };

    public void validate(final String[] args)
    {
        System.out.println("Validating");
        for (int i = 0; i < args.length; i++)
        {
            try
            {
                matchInetNetwork(args[i], true);
                System.out.println("---------------------");
            }
            catch (Exception e)
            {
                System.err.println("Cannot validate " + args[i]);
                e.printStackTrace();
            }
        }
        System.out.println();
    }

    static final long loops = 1000000;

    public void benchmark(final String[] args)
    {
        System.out.println("Benchmarking");
        long benchStart = System.currentTimeMillis();
        for (int i = 0; i < args.length; i++)
        {
            try
            {
                long startTime, stopTime;
                boolean match;

                InetAddress IP = InetAddress.getByName(args[i]);
                match = matchInetNetwork(IP, false);
                startTime = System.currentTimeMillis();
                for (long loop = 0; loop < loops; loop++) matchInetNetwork(IP, false);
                stopTime = System.currentTimeMillis();
                System.out.println("Took: " + (stopTime - startTime)*1.0/loops + "ms to " + (match ? "match " : "reject ") + IP);

                String hostIP = IP.getHostAddress();
                match = matchInetNetwork(hostIP, false);
                startTime = System.currentTimeMillis();
                for (long loop = 0; loop < loops; loop++) matchInetNetwork(hostIP, false);
                stopTime = System.currentTimeMillis();
                System.out.println("Took: " + (stopTime - startTime)*1.0/loops + "ms to " + (match ? "match " : "reject ") + hostIP);

                hostIP = args[i];
                match = matchInetNetwork(hostIP, false);
                startTime = System.currentTimeMillis();
                for (long loop = 0; loop < loops; loop++) matchInetNetwork(hostIP, false);
                stopTime = System.currentTimeMillis();
                System.out.println("Took: " + (stopTime - startTime)*1.0/loops + "ms to " + (match ? "match " : "reject ") + hostIP);
            }
            catch (Exception e)
            {
                System.err.println("Cannot validate " + args[i]);
                e.printStackTrace();
            }
        }
        System.out.println("Benchmark took " + (System.currentTimeMillis() - benchStart)*1.0/1000 + " seconds.");
    }
}

class InetNetwork
{
    /*
     * Implements network masking, and is compatible with RFC 1518 and
     * RFC 1519, which describe CIDR: Classless Inter-Domain Routing.
     */

    private InetAddress network;
    private InetAddress netmask;

    public InetNetwork(InetAddress ip, InetAddress netmask)
    {
        network = maskIP(ip, netmask);
        this.netmask = netmask;
    }

    public boolean contains(final String name) throws java.net.UnknownHostException
    {
        return network.equals(maskIP(InetAddress.getByName(name), netmask));
    }

    public boolean contains(final InetAddress ip)
    {
        return network.equals(maskIP(ip, netmask));
    }

    public String toString()
    {
        return network.getHostAddress() + "/" + netmask.getHostAddress();
    }

    public static InetNetwork getFromString(String netspec) throws java.net.UnknownHostException
    {
//      System.out.println("arg: " + netspec);
        if (netspec.endsWith("*")) netspec = normalizeFromAsterisk(netspec);
        else
        {
            int iSlash = netspec.indexOf('/');
            if (iSlash == -1) netspec += "/255.255.255.255";
            else if (netspec.indexOf('.', iSlash) == -1) netspec = normalizeFromCIDR(netspec);
        }
//      System.out.println("net: " + netspec);

        return new InetNetwork(InetAddress.getByName(netspec.substring(0, netspec.indexOf('/'))),
                               InetAddress.getByName(netspec.substring(netspec.indexOf('/') + 1)));
    }

    public static InetAddress maskIP(final byte[] ip, final byte[] mask)
    {
        try
        {
            return getByAddress(new byte[]
            {
                (byte) (mask[0] & ip[0]),
                (byte) (mask[1] & ip[1]),
                (byte) (mask[2] & ip[2]),
                (byte) (mask[3] & ip[3])
            });
        }
        catch(Exception _) {}
        {
            return null;
        }
    }

    public static InetAddress maskIP(final InetAddress ip, final InetAddress mask)
    {
        return maskIP(ip.getAddress(), mask.getAddress());
    }

    /*
     * This converts from an uncommon "wildcard" CIDR format
     * to "address + mask" format:
     * 
     *   *               =>  000.000.000.0/000.000.000.0
     *   xxx.*           =>  xxx.000.000.0/255.000.000.0
     *   xxx.xxx.*       =>  xxx.xxx.000.0/255.255.000.0
     *   xxx.xxx.xxx.*   =>  xxx.xxx.xxx.0/255.255.255.0
     */
    static private String normalizeFromAsterisk(final String netspec)
    {
        String[] masks = {  "0.0.0.0/0.0.0.0", "0.0.0/255.0.0.0", "0.0/255.255.0.0", "0/255.255.255.0" };
        char[] srcb = netspec.toCharArray();                
        int octets = 0;
        for (int i = 1; i < netspec.length(); i++) {
            if (srcb[i] == '.') octets++;
        }
        return (octets == 0) ? masks[0] : netspec.substring(0, netspec.length() -1 ).concat(masks[octets]);
    }

    /*
     * RFC 1518, 1519 - Classless Inter-Domain Routing (CIDR)
     * This converts from "prefix + prefix-length" format to
     * "address + mask" format, e.g. from xxx.xxx.xxx.xxx/yy
     * to xxx.xxx.xxx.xxx/yyy.yyy.yyy.yyy.
     */
    static private String normalizeFromCIDR(final String netspec)
    {
        final int bits = 32 - Integer.parseInt(netspec.substring(netspec.indexOf('/')+1));
        final int mask = (bits == 32) ? 0 : 0xFFFFFFFF - ((1 << bits)-1);

        return netspec.substring(0, netspec.indexOf('/') + 1) +
                Integer.toString(mask >> 24 & 0xFF, 10) + "." +
                Integer.toString(mask >> 16 & 0xFF, 10) + "." +
                Integer.toString(mask >>  8 & 0xFF, 10) + "." +
                Integer.toString(mask >>  0 & 0xFF, 10);
    }

    private static java.lang.reflect.Method getByAddress = null;

    static {
        try {
            Class inetAddressClass = Class.forName("java.net.InetAddress");
            Class[] parameterTypes = { byte[].class };
            getByAddress = inetAddressClass.getMethod("getByAddress", parameterTypes);
        } catch (Exception e) {
            getByAddress = null;
        }
    }

    private static InetAddress getByAddress(byte[] ip) throws java.net.UnknownHostException
    {
        InetAddress addr = null;
        if (getByAddress != null) try {
            addr = (InetAddress) getByAddress.invoke(null, new Object[] { ip });
        } catch (IllegalAccessException e) {
        } catch (java.lang.reflect.InvocationTargetException e) {
        }

        if (addr == null) {
            addr = InetAddress.getByName
                   (
                    Integer.toString(ip[0] & 0xFF, 10) + "." +
                    Integer.toString(ip[1] & 0xFF, 10) + "." +
                    Integer.toString(ip[2] & 0xFF, 10) + "." +
                    Integer.toString(ip[3] & 0xFF, 10)
                   );
        }
        return addr;
    }
}
