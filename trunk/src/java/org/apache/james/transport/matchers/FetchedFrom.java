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

import javax.mail.internet.MimeMessage;
import java.util.Collection;

/**
 * Matches mail with a header set by Fetchpop X-fetched-from <br>
 * fetchpop sets X-fetched-by to the "name" of the fetchpop fetch task.<br>
 * This is used to match all mail fetched from a specific pop account.
 * Once the condition is met the header is stripped from the message to prevent looping if the mail is re-inserted into the spool.
 *
 * $version
 */

public class FetchedFrom extends GenericMatcher {
    public Collection match(Mail mail) throws javax.mail.MessagingException {
        MimeMessage message = mail.getMessage();
        String fetch = message.getHeader("X-fetched-from", null);
        if (fetch != null && fetch.equals(getCondition())) {
            mail.getMessage().removeHeader("X-fetched-from");
            return mail.getRecipients();
        }
        return null;
    }
}
