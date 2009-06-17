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

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;

/**
 * Checks whether the message (entire message, not just content) is greater
 * than a certain number of bytes.  You can use 'k' and 'm' as optional postfixes.
 * In other words, "1m" is the same as writing "1024k", which is the same as
 * "1048576".
 *
 */
public class SizeGreaterThan extends GenericMatcher {

    int cutoff = 0;

    public void init() {
        String amount = getCondition().trim().toLowerCase(Locale.US);
        if (amount.endsWith("k")) {
            amount = amount.substring(0, amount.length() - 1);
            cutoff = Integer.parseInt(amount) * 1024;
        } else if (amount.endsWith("m")) {
            amount = amount.substring(0, amount.length() - 1);
            cutoff = Integer.parseInt(amount) * 1024 * 1024;
        } else {
            cutoff = Integer.parseInt(amount);
        }
    }

    public Collection match(Mail mail) throws MessagingException {
        MimeMessage message = mail.getMessage();
        //Calculate the size
        int size = message.getSize();
        Enumeration e = message.getAllHeaders();
        while (e.hasMoreElements()) {
            size += ((Header)e.nextElement()).toString().length();
        }
        if (size > cutoff) {
            return mail.getRecipients();
        } else {
            return null;
        }
    }
}
