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
