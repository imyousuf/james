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

package org.apache.james.transport.matchers;

import org.apache.mailet.GenericMatcher;
import org.apache.mailet.Mail;

import javax.mail.MessagingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Checks the IP address of the sending server against a comma-
 * delimited list of IP addresses or domain names.
 * <P>Networks should be indicated with a wildcard *, e.g. 192.168.* 
 * <br>Note: The wildcard can go at any level, the matcher will match if the
 * sending host's IP address (as a String based on the octet representation)
 * starts with the String indicated in the configuration file, excluding the
 * wildcard.
 * <p>Multiple addresses can be indicated, e.g: '127.0.0.1,192.168.*,domain.tld'
 *
 * @author  Serge Knystautas <sergek@lokitech.com>
 */
public class RemoteAddrInNetwork extends GenericMatcher {
    private Collection networks = null;

    public void init() throws MessagingException {
        StringTokenizer st = new StringTokenizer(getCondition(), ", ", false);
        networks = new Vector();
        while (st.hasMoreTokens()) {
            String addr = st.nextToken();
            if (addr.equals("127.0.0.1")) {
                //Add address of local machine as a match
                try {
                    InetAddress localaddr = InetAddress.getLocalHost();
                    networks.add(localaddr.getHostAddress());
                } catch (UnknownHostException uhe) {
                }
            }

            try {
                if (addr.endsWith("*")) {
                    addr = addr.substring(0, addr.length() - 1);
                }
                else {
                    addr = InetAddress.getByName(addr).getHostAddress();
                }
                networks.add(addr);
            } catch (UnknownHostException uhe) {
                log("Cannot match against invalid domain: " + uhe.getMessage());
            }
        }
    }

    public Collection match(Mail mail) {
        String host = mail.getRemoteAddr();
        //Check to see whether it's in any of the networks... needs to be smarter to
        // support subnets better
        for (Iterator i = networks.iterator(); i.hasNext(); ) {
            String networkAddr = i.next().toString();
            if (host.startsWith(networkAddr)) {
                //This is in this network... that's all we need for a match
                return mail.getRecipients();
            }
        }
        //Could not match this to any network
        return null;
    }
}
