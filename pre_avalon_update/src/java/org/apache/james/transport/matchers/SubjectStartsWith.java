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
 *
 * @author  Dino Fancellu <dino.fancellu@ntlworld.com>
 * @version 1.0.0, 1/5/2000
 */

public class SubjectStartsWith extends GenericMatcher {

    public Collection match(Mail mail) throws MessagingException {
        MimeMessage mm = mail.getMessage();
        String subject = mm.getSubject();
        if (subject != null && subject.startsWith(getCondition())) {
            return mail.getRecipients();
        }
        return null;
    }
}
