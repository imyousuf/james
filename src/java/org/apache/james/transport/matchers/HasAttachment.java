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

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Collection;

/**
 * Checks whether this message has an attachment
 *
 * @author  Serge Knystautas <sergek@lokitech.com>
 */
public class HasAttachment extends GenericMatcher {

    public Collection match(Mail mail) throws MessagingException {
        MimeMessage message = mail.getMessage();

        if (message.getContentType() != null &&
                message.getContentType().startsWith("multipart/mixed")) {
            return mail.getRecipients();
        } else {
            return null;
        }
    }
}
