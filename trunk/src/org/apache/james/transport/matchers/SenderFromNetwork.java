/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport.matchers;

import org.apache.mailet.*;
import java.util.*;

/**
 * Checks the network IP address of the sending server against a comma-
 * delimited list of network addresses.
 *
 * @author  Serge Knystautas <sergek@lokitech.com>
 */
public class SenderFromNetwork extends GenericMatcher {
    private Collection networks = null;

    public void init(String condition) throws MailetException {
        StringTokenizer st = new StringTokenizer(condition, ", ", false);
        networks = new Vector();
        while (st.hasMoreTokens()) {
            String addr = st.nextToken();
            if (addr.endsWith("*")) {
                addr = addr.substring(0, addr.length() - 1);
            }
            networks.add(addr);
        }
    }

    public Collection match(Mail mail) {
        String host = mail.getSender().getHost();
        //Check to see whether it's in any of the networks... needs to be smarter to
        // support subnets better
        for (Iterator i = networks.iterator(); i.hasNext(); ) {
            if (host.startsWith(i.next().toString())) {
                //This is in this network... that's all we need for a match
                return mail.getRecipients();
            }
        }
        //Could not match this to any network
        return null;
    }
}