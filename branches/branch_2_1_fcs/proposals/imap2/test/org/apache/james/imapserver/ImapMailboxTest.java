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

package org.apache.james.imapserver;

import org.apache.james.imapserver.store.ImapStore;
import org.apache.james.imapserver.store.InMemoryStore;
import org.apache.james.imapserver.store.ImapMailbox;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.imapserver.store.MessageFlags;
import org.apache.james.imapserver.store.SimpleImapMessage;
import org.apache.james.core.MimeMessageSource;
import org.apache.james.core.MimeMessageWrapper;
import org.apache.james.core.MailImpl;

import junit.framework.TestCase;

import javax.mail.internet.MimeMessage;
import javax.mail.Address;
import java.util.Date;
import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.net.InetAddress;

/**
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.2.2.2 $
 */
public class ImapMailboxTest extends TestCase
        implements ImapConstants
{
    public ImapMailboxTest( String s )
    {
        super( s );
    }

    public void testAppend() throws Exception
    {
        ImapMailbox mailbox = getMailbox();

        MessageFlags flags = new MessageFlags();
        flags.setFlagged( true );

        Date datetime = new Date();
        String message =
        "Date: Mon, 7 Feb 1994 21:52:25 -0800 (PST)\r\n" +
        "From: Fred Foobar <foobar@Blurdybloop.COM>\r\n" +
        "Subject: afternoon meeting\r\n" +
        "To: mooch@owatagu.siam.edu\r\n" +
        "Message-Id: <B27397-0100000@Blurdybloop.COM>\r\n" +
        "MIME-Version: 1.0\r\n" +
        "Content-Type: TEXT/PLAIN; CHARSET=US-ASCII\r\n" +
        "\r\n" +
        "Hello Joe, do you think we can meet at 3:30 tomorrow?\r\n" +
        "\r\n";
        long uid = appendMessage( message, flags, datetime, mailbox );

        SimpleImapMessage imapMessage = mailbox.getMessage( uid );

        assertEquals( 1, mailbox.getMessageCount() );
        assertTrue( imapMessage.getFlags().isFlagged() );
        assertTrue( ! imapMessage.getFlags().isAnswered() );

        MimeMessage mime = imapMessage.getMimeMessage();
        assertEquals( "TEXT/PLAIN; CHARSET=US-ASCII", mime.getContentType() );
        assertEquals( "afternoon meeting", mime.getSubject() );
        assertEquals( "Fred Foobar <foobar@Blurdybloop.COM>",
                      mime.getFrom()[0].toString() );

    }

    private long appendMessage( String messageContent, MessageFlags flags,
                                Date datetime, ImapMailbox mailbox )
    {
        MimeMessageSource source =
                new MimeMessageByteArraySource( "messageContent:" + System.currentTimeMillis(),
                                                messageContent.getBytes());
        MimeMessage message = new MimeMessageWrapper( source );
        SimpleImapMessage imapMessage = mailbox.createMessage( message, flags, datetime );
        return imapMessage.getUid();
    }

    private ImapMailbox getMailbox() throws MailboxException
    {
        ImapStore store = new InMemoryStore();
        ImapMailbox root = store.getMailbox( ImapConstants.USER_NAMESPACE );
        ImapMailbox test = store.createMailbox( root, "test", true );
        return test;
    }

    class MimeMessageByteArraySource extends MimeMessageSource
    {
        private String sourceId;
        private byte[] byteArray;

        public MimeMessageByteArraySource( String sourceId, byte[] byteArray )
        {
            this.sourceId = sourceId;
            this.byteArray = byteArray;
        }

        public String getSourceId()
        {
            return sourceId;
        }

        public InputStream getInputStream() throws IOException
        {
            return new ByteArrayInputStream( byteArray );
        }
    }


}
