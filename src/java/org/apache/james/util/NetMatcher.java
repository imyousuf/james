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
        for (Iterator iter = nets.iterator(); iter.hasNext(); ) try
        {
            networks.add(InetNetwork.getFromString((String) iter.next()));
        }
        catch (java.net.UnknownHostException uhe)
        {
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
        }
        networks.trimToSize();
    }

    public boolean matchInetNetwork(final String hostIP)
    {
        InetAddress ip = null;

        try
        {
            ip = InetAddress.getByName(hostIP);
        }
        catch (java.net.UnknownHostException uhe)
        {
        }

        boolean sameNet = false;

        for (Iterator iter = networks.iterator(); (!sameNet) && iter.hasNext(); )
        {
            InetNetwork network = (InetNetwork) iter.next();
            sameNet = network.contains(ip);
        }
        return sameNet;
    }

    public boolean matchInetNetwork(final InetAddress ip)
    {
        boolean sameNet = false;

        for (Iterator iter = networks.iterator(); (!sameNet) && iter.hasNext(); )
        {
            InetNetwork network = (InetNetwork) iter.next();
            sameNet = network.contains(ip);
        }
        return sameNet;
    }

    public NetMatcher()
    {
    }

    public NetMatcher(final String[] nets)
    {
        initInetNetworks(nets);
    }

    public NetMatcher(final Collection nets)
    {
        initInetNetworks(nets);
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
        if (netspec.endsWith("*")) netspec = normalizeFromAsterisk(netspec);
        else
        {
            int iSlash = netspec.indexOf('/');
            if (iSlash == -1) netspec += "/255.255.255.255";
            else if (netspec.indexOf('.', iSlash) == -1) netspec = normalizeFromCIDR(netspec);
        }

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
