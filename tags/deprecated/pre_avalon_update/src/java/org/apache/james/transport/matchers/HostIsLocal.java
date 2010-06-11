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

/**
 * @version 1.0.0, 24/04/1999
 * @author Federico Barbieri <scoobie@pop.systemy.it>
 * @author Serge Knystautas <sergek@lokitech.com>
 */
public class HostIsLocal extends GenericRecipientMatcher {

    public boolean matchRecipient(MailAddress recipient) {
        return getMailetContext().isLocalServer(recipient.getHost().toLowerCase());
    }
}
