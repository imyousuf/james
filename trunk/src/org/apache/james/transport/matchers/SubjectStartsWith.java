/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport.matchers;

import org.apache.mail.Mail;
import javax.mail.internet.*;
import javax.mail.*;
import java.util.*;

/**
 *
 * @author  Dino Fancellu <dino.fancellu@ntlworld.com>
 * @version 1.0.0, 1/5/2000
 */

public class SubjectStartsWith extends AbstractMatch {
    String condition;

    public void init(String condition) {
        this.condition = condition;
    }

    public Collection match(Mail mail, String condition) {
        try {
            MimeMessage mm = mail.getMessage();
            String subject = mm.getSubject();
            if (subject != null && subject.startsWith(condition)) {
                return mail.getRecipients();
            }
        } catch (MessagingException ex) {
        }
        return null;
    }
}