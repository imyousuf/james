/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport;

import org.apache.mail.*;
import java.util.*;
import org.apache.java.lang.*;

/**
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public abstract class AbstractMatcher implements Matcher {
    
    private MailetContext context;

    public abstract void init(String condition);
    
    public abstract Mail[] match(Mail mail);
    
    public MailetContext getContext() {
        return context;
    }
    
    public void setMailetContext(MailetContext context) {
        this.context = context;
    }

    public Mail[] split(Mail mail, Vector matching, Vector notMatching) {

        Mail[] res = {null, null};
        if (matching.isEmpty()) {
            mail.setRecipients(notMatching);
            res[0] = (Mail) null;
            res[1] = mail;
            return res;
        }
        if (notMatching.isEmpty()) {
            mail.setRecipients(matching);
            res[0] = mail;
            res[1] = (Mail) null;
            return res;
        }
        Mail notMail = mail.duplicate(mail.getName() + "!");
        mail.setRecipients(matching);
        notMail.setRecipients(notMatching);
        res[0] = mail;
        res[1] = notMail;
        return res;
    }
}
    
