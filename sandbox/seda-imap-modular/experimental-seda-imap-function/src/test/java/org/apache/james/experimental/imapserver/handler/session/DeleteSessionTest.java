package org.apache.james.experimental.imapserver.handler.session;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.api.imap.ProtocolException;
import org.apache.james.experimental.imapserver.client.DeleteClientCommand;
import org.apache.james.experimental.imapserver.client.LoginCommand;
import org.apache.james.experimental.imapserver.client.LogoutClientCommand;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.experimental.imapserver.util.MessageGenerator;
import org.apache.james.mailboxmanager.MailboxManagerException;

public class DeleteSessionTest extends AbstractSessionTest {

    String[] folders = { USER_MAILBOX_ROOT + ".INBOX",
            USER_MAILBOX_ROOT + ".test", USER_MAILBOX_ROOT + ".test1",
            USER_MAILBOX_ROOT + ".test1.test1a",
            USER_MAILBOX_ROOT + ".test1.test1b",
            USER_MAILBOX_ROOT + ".test2.test2a",
            USER_MAILBOX_ROOT + ".test2.test2b" };

    MimeMessage[] msgs = null;

    public void setUp() throws MailboxException, MessagingException,
            IOException, MailboxManagerException {
        super.setUp();
        msgs = MessageGenerator.generateSimpleMessages(4);
        createFolders(folders);
        // increase the uid
        appendMessagesClosed(USER_MAILBOX_ROOT+".test1.test1a", msgs);
 
    }

    public void tearDown() throws Exception {
        super.tearDown();
        for (int i = 0; i < folders.length; i++) {
            assertFalse(folders[i] + " is still in use!", isOpen(folders[i]));
        }
    }

    public void testDeleteExisting() throws ProtocolException, IOException,
            MessagingException, MailboxManagerException {
        verifyCommand(new LoginCommand(USER_NAME, USER_PASSWORD));
        assertTrue(folderExists(USER_MAILBOX_ROOT + ".test1.test1a"));
        verifyCommand(new DeleteClientCommand("test1.test1a"));
        assertFalse(folderExists(USER_MAILBOX_ROOT + ".test1.test1a"));
        verifyCommand(new LogoutClientCommand());
    }

}
