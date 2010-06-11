/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport.matchers;

import org.apache.mailet.*;
import java.util.*;

/**
 * @version 1.0.0, 24/04/1999
 * @author Federico Barbieri <scoobie@pop.systemy.it>
 * @author Serge Knystautas <sergek@lokitech.com>
 */
public class HostIsLocal extends GenericRecipientMatcher {

    private Collection localhosts;

    public void init() {
        localhosts = new Vector();
        for (Iterator i = getMailetContext().getServerNames().iterator(); i.hasNext(); ) {
            localhosts.add(i.next().toString().toLowerCase());
        }
    }

    public boolean matchRecipient(MailAddress recipient) {
        return localhosts.contains(recipient.getHost().toLowerCase());
    }
}