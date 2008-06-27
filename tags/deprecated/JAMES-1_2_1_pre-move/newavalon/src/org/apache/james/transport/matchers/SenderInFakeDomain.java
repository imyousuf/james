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
 * Does a DNS lookup (MX and A/CNAME records) on the sender's domain.  If
 * there are no entries, the domain is fake.
 *
 * @author  Serge Knystautas <sergek@lokitech.com>
 */
public class SenderInFakeDomain extends GenericMatcher {

    public Collection match(Mail mail) {
        String domain = mail.getSender().getHost();
        //DNS Lookup for this domain
        Collection servers = getMailetContext().getMailServers(domain);
        if (servers.size() == 0) {
            //No records...could not deliver to this domain, so matches criteria.
	    log("No MX record found for domain: " + domain);
            return mail.getRecipients();
        } else {
            //Some servers were found... the domain is not fake.
            return null;
        }
    }
}
