/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver.store;

import javax.mail.internet.MimeMessage;
import java.util.Date;

/**
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.1 $
 */
public interface ImapMessage1
{
    MimeMessage getMimeMessage();

    MessageFlags getFlags();

    Date getInternalDate();

    long getUid();

    ImapMessageAttributes getAttributes() throws MailboxException;
}
