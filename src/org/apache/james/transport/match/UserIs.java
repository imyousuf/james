/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport.match;

import java.util.*;
import org.apache.mail.Mail;

/**
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class UserIs extends AbstractMatch {
    
    public Vector match(Mail mail, String condition) {
        Vector matchingRecipients = new Vector();
        for (Enumeration e = mail.getRecipients().elements(); e.hasMoreElements(); ) {
            String rec = (String) e.nextElement();
            if (condition.indexOf(getUser(rec)) != -1) {
                matchingRecipients.addElement(rec);
            }
        }
        return matchingRecipients;
    }
    
    private String getUser(String recipient) {
        return recipient.substring(0, recipient.indexOf("@"));
    }
}
    
