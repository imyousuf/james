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

import java.util.Collection;

/**
 * Does a DNS lookup (MX and A/CNAME records) on the sender's domain.  If
 * there are no entries, the domain is considered fake and the match is 
 * successful.
 *
 */
public class SenderInFakeDomain extends AbstractNetworkMatcher {

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
        } else if (matchNetwork(servers.iterator().next().toString())){
            /*
             * It could be a wildcard address like these:
             *
             * 64.55.105.9/32          # Allegiance Telecom Companies Worldwide (.nu)
             * 64.94.110.11/32         # VeriSign (.com .net)
             * 194.205.62.122/32       # Network Information Center - Ascension Island (.ac)
             * 194.205.62.62/32        # Internet Computer Bureau (.sh)
             * 195.7.77.20/32          # Fredrik Reutersward Data (.museum)
             * 206.253.214.102/32      # Internap Network Services (.cc)
             * 212.181.91.6/32         # .NU Domain Ltd. (.nu)
             * 219.88.106.80/32        # Telecom Online Solutions (.cx)
             * 194.205.62.42/32        # Internet Computer Bureau (.tm)
             * 216.35.187.246/32       # Cable & Wireless (.ws)
             * 203.119.4.6/32          # .PH TLD (.ph)
             *
             */
            log("Banned IP found for domain: " + domain);
            log(" --> :" + servers.iterator().next().toString());
            return mail.getRecipients();
        } else {
            // Some servers were found... the domain is not fake.

            return null;
        }
    }
}
