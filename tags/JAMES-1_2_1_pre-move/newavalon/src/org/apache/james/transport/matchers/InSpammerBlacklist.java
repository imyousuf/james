/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport.matchers;

import org.apache.mailet.*;
import java.net.*;
import java.util.*;
import javax.mail.*;

/**
 * Checks the network IP address of the sending server against a
 * blacklist of spammers.  There are 3 lists that support this...
 * <ul>
 * <li><b>blackholes.mail-abuse.org</b>: Rejected - see  http://www.mail-abuse.org/rbl/
 * <li><b>dialups.mail-abuse.org</b>: Dialup - see http://www.mail-abuse.org/dul/
 * <li><b>relays.mail-abuse.org</b>: Open spam relay - see http://www.mail-abuse.org/rss/
 * </ul>
 *
 * Example:
 * &lt;mailet match="InSpammerBlacklist=blackholes.mail-abuse.org" class="ToProcessor"&gt;
 *   &lt;processor&gt;spam&lt;/processor&gt;
 * &lt;/mailet&gt;
 *
 * @author  Serge Knystautas <sergek@lokitech.com>
 */
public class InSpammerBlacklist extends GenericMatcher {
    String network = null;

    public void init() throws MessagingException {
        network = getCondition();
    }

    public Collection match(Mail mail) {
        String host = mail.getRemoteAddr();
        try {
            //Have to reverse the octets first
            StringTokenizer st = new StringTokenizer(host, " .", false);
            host = network;
            while (st.hasMoreTokens()) {
                host = st.nextToken() + ".";
            }

            //Try to look it up
            InetAddress.getByName(host);

            //If we got here, that's bad... it means the host
            //  was found in the blacklist
            return mail.getRecipients();
        } catch (UnknownHostException uhe) {
            //This is good... it's not on the list
            return null;
        }
    }
}
