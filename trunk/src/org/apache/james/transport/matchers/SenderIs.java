/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.transport.matchers;

import org.apache.mailet.*;
import java.util.*;

/**
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class SenderIs extends GenericMatcher {

    private Collection senders;

    public void init() {
        StringTokenizer st = new StringTokenizer(getCondition(), ", ", false);
        senders = new Vector();
        while (st.hasMoreTokens()) {
            senders.add(st.nextToken());
        }
    }

    public Collection match(Mail mail) {
        if (senders.contains(mail.getSender())) {
            return mail.getRecipients();
        } else {
            return null;
        }
    }
}
