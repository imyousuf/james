/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport.matchers;

import org.apache.mailet.*;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.*;

/**
 *
 * @author  Serge Knystautas <sergek@lokitech.com>
 * @version 1.0.0, 1/5/2000
 */

public class RelayLimit extends GenericMatcher {
    int limit = 30;

    public void init() {
        limit = Integer.parseInt(getCondition());
    }

    public Collection match(Mail mail) throws javax.mail.MessagingException {
        MimeMessage mm = mail.getMessage();
        int count = 0;
        for (Enumeration e = mm.getAllHeaders(); e.hasMoreElements();) {
            Header hdr = (Header)e.nextElement();
            if (hdr.getName().equals("Received")) {
                count++;
            }
        }
        if (count >= limit) {
            return mail.getRecipients();
        } else {
            return null;
        }
    }
}