/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.transport.matchers;

import org.apache.mailet.GenericMatcher;
import org.apache.mailet.Mail;

import java.util.Collection;

/**
 * use: <mailet match="HasHeader=<header>" class="..." />
 *
 * This matcher simply checks to see if the header named is present.
 * If complements the AddHeader mailet.
 *
 * TODO: support lists of headers and values, e.g, match="{<header>[=value]}+"
 *       [will require a complete rewrite from the current trivial one-liner]
 *
 * @author  Noel J. Bergman <noel@devtech.com>
 */
public class HasHeader extends GenericMatcher {

    public Collection match(Mail mail) throws javax.mail.MessagingException {
        return (mail.getMessage().getHeader(getCondition(), null) != null) ? mail.getRecipients() : null;
    }
}

