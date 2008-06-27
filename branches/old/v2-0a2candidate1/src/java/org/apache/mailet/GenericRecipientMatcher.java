/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.mailet;

import java.util.*;
import javax.mail.*;

/**
 * GenericMatcher makes writing recipient based matchers easier. It provides
 * simple versions of the lifecycle methods init and destroy and of the methods
 * in the MatcherConfig interface. GenericMatcher also implements the log method,
 * declared in the
 * MatcherContext interface.
 *
 * @version 1.0.0, 24/04/1999
 * @author Federico Barbieri <scoobie@pop.systemy.it>
 * @author Serge Knystautas <sergek@lokitech.com>
 */
public abstract class GenericRecipientMatcher extends GenericMatcher {

    /**
     * Matches each recipient one by one through matchRecipient(MailAddress
     * recipient) method.  Handles splitting the recipients Collection
     * as appropriate.
     *
     * @param mail - the message and routing information to determine whether to match
     * @return Collection the Collection of MailAddress objects that have been matched
     */
    public final Collection match(Mail mail) throws MessagingException {
        Collection matching = new Vector();
        for (Iterator i = mail.getRecipients().iterator(); i.hasNext(); ) {
            MailAddress rec = (MailAddress) i.next();
            if (matchRecipient(rec)) {
                matching.add(rec);
            }
        }
        return matching;
    }

    /**
     * Simple check to match exclusively on the email address (not
     * message information).
     *
     * @param recipient - the address to determine whether to match
     * @return boolean whether the recipient is a match
     */
    public abstract boolean matchRecipient(MailAddress recipient) throws MessagingException;
}
