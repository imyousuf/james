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
public class HostIs extends AbstractMatcher {
    
    private Mail[] res = {(Mail) null, (Mail) null};
    private Vector hosts;
    
    public void init(String condition) {
        StringTokenizer st = new StringTokenizer(condition, ", ");
        hosts = new Vector();
        while (st.hasMoreTokens()) {
            hosts.addElement(st.nextToken());
        }
    }

    public Mail[] match(Mail mail) {
        Vector matching = new Vector();
        Vector notMatching = mail.getRecipients();
        for (Enumeration e = notMatching.elements(); e.hasMoreElements(); ) {
            String rec = (String) e.nextElement();
            if (hosts.contains(Mail.getHost(rec))) {
                matching.addElement(rec);
            }
        }
        if (matching.isEmpty()) {
            res[0] = (Mail) null;
            res[1] = mail;
            return res;
        }
        for (Enumeration e = matching.elements(); e.hasMoreElements(); ) {
            notMatching.removeElement(e.nextElement());
        }
        res[0] = mail;
        if (notMatching.isEmpty()) {
            res[1] = (Mail) null;
            return res;
        }
        Mail notmail = mail.duplicate(mail.getName() + "!");
        mail.setRecipients(matching);
        res[1] = notmail;
        return res;
    }
}
    
