/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.transport.matchers;

import org.apache.mailet.GenericMatcher;
import org.apache.mailet.Mail;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * Checkes the sender's displayed domain name against a supplied list.
 *
 * Sample configuration:
 *
 * <mailet match="SenderHostIs=domain.com" class="ToProcessor">
 *   <processor> spam </processor>
 * </mailet>
 *
 * @version 1.0.0, 2002-09-10
 */
public class SenderHostIs extends GenericMatcher {
    /**
     * The collection of host names to match against.
     */
    private Collection senderHosts;

    /**
     * Initialize the mailet.
     */
    public void init()  {
        //Parse the condition...
        StringTokenizer st = new StringTokenizer(getCondition(), ", ", false);

        //..into a vector of domain names.
        senderHosts = new java.util.HashSet();
        while (st.hasMoreTokens()) {
            senderHosts.add(st.nextToken().toLowerCase(Locale.US));
        }
        senderHosts = Collections.unmodifiableCollection(senderHosts);
    }

    /**
     * Takes the message and checks the sender (if there is one) against
     * the vector of host names.
     *
     * Returns the collection of recipients if there's a match.
     *
     * @param mail the mail being processed
     */
    public Collection match(Mail mail) {
        try {
            if (mail.getSender() != null && senderHosts.contains(mail.getSender().getHost().toLowerCase(Locale.US))) {
                return mail.getRecipients();
            }
        } catch (Exception e) {
            log(e.getMessage());
        }

        return null;    //No match.
    }
}
