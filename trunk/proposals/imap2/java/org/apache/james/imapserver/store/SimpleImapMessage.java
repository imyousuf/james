/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.james.imapserver.store;

import org.apache.avalon.framework.logger.AbstractLogEnabled;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Date;

/**
 * A mail message with all of the extra stuff that IMAP requires.
 * This is just a placeholder object, while I work out what's really required. A common
 * way of handling *all* messages needs to be available for James (maybe MailImpl?)
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.7 $
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
