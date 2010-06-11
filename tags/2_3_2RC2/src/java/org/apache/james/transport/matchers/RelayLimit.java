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

import org.apache.mailet.RFC2822Headers;
import org.apache.mailet.GenericMatcher;
import org.apache.mailet.Mail;

import javax.mail.Header;
import javax.mail.internet.MimeMessage;
import java.util.Collection;
import java.util.Enumeration;

/**
 *
 * @version 1.0.0, 1/5/2000
 */

public class RelayLimit extends GenericMatcher {
    int limit = 30;

    public void init() {
        limit = Integer.parseInt(getCondition());
    }

    public Collection match(Mail mail) throws javax.mail.MessagingException {
        MimeMessage mm = mail.getMessage();
        int count = 0;
        for (Enumeration e = mm.getAllHeaders(); e.hasMoreElements();) {
            Header hdr = (Header)e.nextElement();
            if (hdr.getName().equals(RFC2822Headers.RECEIVED)) {
                count++;
            }
        }
        if (count >= limit) {
            return mail.getRecipients();
        } else {
            return null;
        }
    }
}
