/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.transport.matchers;

import org.apache.mailet.GenericRecipientMatcher;
import org.apache.mailet.MailAddress;

import java.util.Collection;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class HostIs extends GenericRecipientMatcher {

    private Collection hosts;

    public void init() {
        StringTokenizer st = new StringTokenizer(getCondition(), ", ", false);
        hosts = new Vector();
        while (st.hasMoreTokens()) {
            hosts.add(st.nextToken().toLowerCase());
        }
    }

    public boolean matchRecipient(MailAddress recipient) {
        return hosts.contains(recipient.getHost().toLowerCase());
    }
}
