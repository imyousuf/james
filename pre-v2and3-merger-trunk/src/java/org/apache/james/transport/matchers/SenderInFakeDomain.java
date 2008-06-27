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

package org.apache.james.transport.matchers;

import org.apache.mailet.GenericMatcher;
import org.apache.mailet.Mail;

import java.util.Collection;

/**
 * Does a DNS lookup (MX and A/CNAME records) on the sender's domain.  If
 * there are no entries, the domain is considered fake and the match is
 * successful.
 *
 */
public class SenderInFakeDomain extends GenericMatcher {

    public Collection match(Mail mail) {
        if (mail.getSender() == null) {
            return null;
        }
        String domain = mail.getSender().getHost();
        //DNS Lookup for this domain
        Collection servers = getMailetContext().getMailServers(domain);
        if (servers.size() == 0) {
            //No records...could not deliver to this domain, so matches criteria.
            log("No MX, A, or CNAME record found for domain: " + domain);
            return mail.getRecipients();
        } else {
            //Some servers were found... the domain is not fake.
            return null;
        }
    }
}
