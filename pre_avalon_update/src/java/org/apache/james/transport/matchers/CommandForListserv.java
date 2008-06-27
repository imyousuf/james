/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.transport.matchers;

import org.apache.mailet.GenericRecipientMatcher;
import org.apache.mailet.MailAddress;

import javax.mail.MessagingException;

/**
 * Returns positive if the recipient is a command for a listserv.  For example,
 * if my listserv is james@list.working-dogs.com, this matcher will return true
 * for james-on@list.working-dogs.com and james-off@list.working-dogs.com.
 *
 * @author Serge Knystautas <sergek@lokitech.com>
 */
public class CommandForListserv extends GenericRecipientMatcher {

    private MailAddress listservAddress;

    public void init() throws MessagingException {
        listservAddress = new MailAddress(getCondition());
    }

    public boolean matchRecipient(MailAddress recipient) {
        if (recipient.getHost().equals(listservAddress.getHost())) {
            if (recipient.getUser().equals(listservAddress.getUser() + "-on")
                || recipient.getUser().equals(listservAddress.getUser() + "-off")) {
                return true;
            }
        }
        return false;
    }
}
