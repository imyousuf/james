/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.transport.matchers;

import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.mailet.*;

/**
 *
 * @author  Dino Fancellu <dino.fancellu@ntlworld.com>
 * @version 1.0.0, 1/5/2000
 */

public class SubjectIs extends GenericMatcher {
    public Collection match(Mail mail) throws javax.mail.MessagingException {
        MimeMessage mm = mail.getMessage();
        String subject = mm.getSubject();
        if (subject != null && subject.equals(getCondition())) {
            return mail.getRecipients();
        }
        return null;
    }
}
