/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport.matchers;

import org.apache.mail.*;
import org.apache.james.transport.*;
import org.apache.java.util.*;
import java.util.*;

/**
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class RecipientIs extends AbstractMatcher {
    
    private Vector recipients;
    
    public void init(String condition) {
        StringTokenizer st = new StringTokenizer(condition, ", ");
        recipients = new Vector();
        while (st.hasMoreTokens()) {
            recipients.addElement(st.nextToken());
        }
    }

    public Mail[] match(Mail mail) {
        Vector matching = new Vector();
        Vector notMatching = new Vector();
        for (Enumeration e = mail.getRecipients().elements(); e.hasMoreElements(); ) {
            String rec = (String) e.nextElement();
            if (recipients.contains(rec)) {
                matching.addElement(rec);
            }
        }
        notMatching = VectorUtils.subtract(mail.getRecipients(), matching);
        return split(mail, matching, notMatching);
    }
}    
