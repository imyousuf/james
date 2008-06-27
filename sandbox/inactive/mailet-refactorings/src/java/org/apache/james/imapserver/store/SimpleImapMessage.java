/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.imapserver.store;

import java.util.Date;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.avalon.framework.logger.AbstractLogEnabled;

/**
 * A mail message with all of the extra stuff that IMAP requires.
 * This is just a placeholder object, while I work out what's really required. A common
 * way of handling *all* messages needs to be available for James (maybe MailImpl?)
 * 
 * @version $Revision: 109034 $
 */
public class SimpleImapMessage
        extends AbstractLogEnabled implements ImapMessage
{
    private MimeMessage mimeMessage;
    private Flags flags;
    private Date internalDate;
    private long uid;
    private SimpleMessageAttributes attributes;

    public SimpleImapMessage(MimeMessage mimeMessage, Date internalDate, long uid)
            throws MessagingException {
        this(mimeMessage, mimeMessage.getFlags(), internalDate, uid);
    }

    SimpleImapMessage( MimeMessage mimeMessage, Flags flags,
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

    public Flags getFlags() {
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
