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
public class HostIs extends AbstractRecipientMatcher {

    private Collection hosts;

    public void init(String condition) {
        StringTokenizer st = new StringTokenizer(condition, ", ");
        hosts = new Vector();
        while (st.hasMoreTokens()) {
            hosts.add(st.nextToken());
        }
    }

    public boolean matchRecipient(String recipient) {
        return hosts.contains(Mail.getHost(recipient));
    }
}

