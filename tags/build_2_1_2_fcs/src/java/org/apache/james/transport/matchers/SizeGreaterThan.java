/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
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
 * @author  Serge Knystautas <sergek@lokitech.com>
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
