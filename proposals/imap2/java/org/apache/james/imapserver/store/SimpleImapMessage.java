/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver.store;

import org.apache.avalon.framework.logger.AbstractLogEnabled;

import javax.mail.internet.MimeMessage;
import javax.mail.MessagingException;
import java.util.Date;

/**
 * A mail message with all of the extra stuff that IMAP requires.
 * This is just a placeholder object, while I work out what's really required. A common
 * way of handling *all* messages needs to be available for James (maybe MailImpl?)
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.1 $
 */
public class SimpleImapMessage
        extends AbstractLogEnabled implements ImapMessage1
{
    private MimeMessage mimeMessage;
    private MessageFlags flags;
    private Date internalDate;
    private long uid;
    private SimpleMessageAttributes attributes;

    SimpleImapMessage( MimeMessage mimeMessage, MessageFlags flags,
                 Date internalDate, long uid )
    {
        this.mimeMessage = mimeMessage;
        this.flags = flags;
        this.internalDate = internalDate;
        this.uid = uid;
    }

    public MimeMessage getMimeMessage() {
        return mimeMessage;
    }

    public MessageFlags getFlags() {
        return flags;
    }

    public Date getInternalDate() {
        return internalDate;
    }

    public long getUid() {
        return uid;
    }

    public ImapMessageAttributes getAttributes() throws MailboxException
    {
        if ( attributes == null ) {
            attributes = new SimpleMessageAttributes();
            setupLogger( attributes );
            try {
                attributes.setAttributesFor( mimeMessage );
            }
            catch ( MessagingException e ) {
                throw new MailboxException( "Could not parse mime message." );
            }
        }
        return attributes;
    }
}
