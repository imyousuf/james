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
public abstract class AbstractRecipientMatcher extends AbstractMatcher {

    public Collection match(Mail mail) {
        Collection matching = new Vector();
        for (Iterator i = mail.getRecipients().iterator(); i.hasNext(); ) {
            String rec = (String) i.next();
            if (matchRecipient(rec)) {
                matching.add(rec);
            }
        }
        return matching;
    }

    public abstract boolean matchRecipient(String recipient);

}

