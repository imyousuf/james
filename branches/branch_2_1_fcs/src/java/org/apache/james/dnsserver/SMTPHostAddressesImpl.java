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

import org.apache.mailet.MailetContext;
import java.net.InetAddress;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Record;

/**
 * A holder of data generated in the DNSServer and needed in 
 * RemoteDelivery.  Especially needed for multihomed hosts.
 */
public class SMTPHostAddressesImpl implements MailetContext.SMTPHostAddresses
{
    public static final int SMTP_PORT = 25;
    protected String hostName;
    protected InetAddress[] ipAddresses;

    /**
     * @param aRecords array of addresses for the host
     * @param hostName name of host
     */
    public SMTPHostAddressesImpl (Record[] aRecords, String hostName) {
        this.hostName = hostName;
        if (aRecords == null){
            ipAddresses = new InetAddress[0];
        } else {
            ipAddresses = new InetAddress[aRecords.length];
            for (int i = 0; i < ipAddresses.length; i++){
                ipAddresses[i] = ((ARecord)aRecords[i]).getAddress();
            }
        }
    }
    
    /**
     *  @return the hostName of the SMTP server (from the MX record lookup)
     */
    public String getHostname() {
        return hostName;
    }
    
    /**
     * @return an array with the ip addresses of the hostname. An array is
     * used because a host can have multiple homes (addresses)
     */
    public InetAddress[] getAddresses() {
        return ipAddresses;
    }


    /**
     * @param address for which we need the port to use in SMTP connection
     * @return the port number to use for the given address (this will usually be 25 for SMTP)
     */
    public int getPort(InetAddress address) {
        //this implementation will always return 25
        return SMTP_PORT;
    }
    
}
