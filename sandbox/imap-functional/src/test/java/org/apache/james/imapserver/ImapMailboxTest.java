/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.imapserver;

import junit.framework.TestCase;
import org.apache.james.core.MimeMessageSource;
import org.apache.james.core.MimeMessageWrapper;
import org.apache.james.imapserver.store.ImapMailbox;
import org.apache.james.imapserver.store.ImapStore;
import org.apache.james.imapserver.store.InMemoryStore;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.imapserver.store.SimpleImapMessage;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

/**
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision$
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
        ImapMailbox mailbox = getMailbox("test");

        Flags flags = new Flags();
        flags.add(Flags.Flag.FLAGGED);

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
        assertTrue( imapMessage.getFlags().contains(Flags.Flag.FLAGGED) );
        assertTrue( ! imapMessage.getFlags().contains(Flags.Flag.ANSWERED) );

        MimeMessage mime = imapMessage.getMimeMessage();
        assertEquals( "TEXT/PLAIN; CHARSET=US-ASCII", mime.getContentType() );
        assertEquals( "afternoon meeting", mime.getSubject() );
        assertEquals( "Fred Foobar <foobar@Blurdybloop.COM>",
                      mime.getFrom()[0].toString() );

    }

    private long appendMessage( String messageContent, Flags flags,
                                Date datetime, ImapMailbox mailbox )
    {
        MimeMessageSource source =
                new MimeMessageByteArraySource( "messageContent:" + System.currentTimeMillis(),
                                                messageContent.getBytes());
        MimeMessage message = new MimeMessageWrapper( source );
        return mailbox.appendMessage( message, flags, datetime );
    }

    public void testName() throws Exception {
        checkName("named");
        checkName("NaMeD");
        checkName("Na_94Eg");
    }

    private void checkName(String mailboxName) throws Exception {
        ImapMailbox mailbox = getMailbox(mailboxName);
        assertEquals(mailboxName, mailbox.getName());
        assertEquals(ImapConstants.USER_NAMESPACE + ImapConstants.HIERARCHY_DELIMITER + mailboxName,
                mailbox.getFullName());
    }

    public void testMailboxFlags() throws Exception {
        Flags expectedFlags = new Flags();
        expectedFlags.add(Flags.Flag.ANSWERED);
        expectedFlags.add(Flags.Flag.DELETED);
        expectedFlags.add(Flags.Flag.DRAFT);
        expectedFlags.add(Flags.Flag.FLAGGED);
        expectedFlags.add(Flags.Flag.SEEN);

        ImapMailbox mailbox = getMailbox("test");

        Flags permanentFlags = mailbox.getPermanentFlags();
        assertTrue(permanentFlags.contains(expectedFlags));
        assertTrue(! permanentFlags.contains(Flags.Flag.RECENT));
    }

    public void testMessageCount() throws Exception
    {
        ImapMailbox mailbox = getMailbox("messagecount");
        assertEquals(0, mailbox.getMessageCount());

        appendMessage(mailbox);
        appendMessage(mailbox);
        assertEquals(2, mailbox.getMessageCount());

        appendMessage(mailbox);
        assertEquals(3, mailbox.getMessageCount());

        mailbox.deleteAllMessages();
        assertEquals(0, mailbox.getMessageCount());
    }

    public void testUnseen() throws Exception {
        ImapMailbox mailbox = getMailbox("firstUnseen");
        assertEquals(-1, mailbox.getFirstUnseen());
        assertEquals(0, mailbox.getUnseenCount());

        long uid1 = appendMessage(mailbox);
        long uid2 = appendMessage(mailbox);
        assertEquals(1, mailbox.getFirstUnseen());
        assertEquals(2, mailbox.getUnseenCount());

        // Flag the first as seen
        mailbox.setFlags(new Flags(Flags.Flag.SEEN), true, uid1, null, false);
        assertEquals(2, mailbox.getFirstUnseen());
        assertEquals(1, mailbox.getUnseenCount());

        // Flag the second as seen
        mailbox.setFlags(new Flags(Flags.Flag.SEEN), true, uid2, null, false);
        assertEquals(-1, mailbox.getFirstUnseen());
        assertEquals(0, mailbox.getUnseenCount());

        // Unset the seen flag on the first
        mailbox.setFlags(new Flags(Flags.Flag.SEEN), false, uid1, null, false);
        assertEquals(1, mailbox.getFirstUnseen());
        assertEquals(1, mailbox.getUnseenCount());
    }

    public void testSelectable() throws Exception {
        ImapStore store = new InMemoryStore();
        ImapMailbox root = store.getMailbox( ImapConstants.USER_NAMESPACE );
        ImapMailbox selectable = store.createMailbox( root, "selectable", true );
        assertTrue(selectable.isSelectable());

        ImapMailbox nonSelectable = store.createMailbox(root, "nonselectable", false);
        assertTrue(! nonSelectable.isSelectable());
    }

    public void testUidNext() throws Exception {
        ImapMailbox mailbox = getMailbox("uidnext");
        long first = mailbox.getUidNext();
        assertTrue(first != 0);
        assertEquals(first, appendMessage(mailbox));
        long second = mailbox.getUidNext();
        assertEquals(second, appendMessage(mailbox));
        assertTrue(first < second);
    }

    private long appendMessage(ImapMailbox mailbox) {
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

        return appendMessage( message, new Flags(), datetime, mailbox );
    }

    private ImapMailbox getMailbox(String mailboxName) throws MailboxException
    {
        ImapStore store = new InMemoryStore();
        ImapMailbox root = store.getMailbox( ImapConstants.USER_NAMESPACE );
        ImapMailbox test = store.createMailbox( root, mailboxName, true );
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
