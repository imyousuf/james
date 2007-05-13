package org.apache.james.imapserver.handler.session;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.imapserver.ProtocolException;
import org.apache.james.imapserver.client.LoginCommand;
import org.apache.james.imapserver.client.LogoutClientCommand;
import org.apache.james.imapserver.client.RenameClientCommand;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.imapserver.util.MessageGenerator;
import org.apache.james.mailboxmanager.MailboxManagerException;

public class RenameSessionTest extends AbstractSessionTest {

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
        msgs = MessageGenerator.generateSimpleMessages(2);
        createFolders(folders);
        for (int i = 0; i < folders.length; i++) {
            appendMessagesClosed(folders[i], msgs);
        }
    }

    public void tearDown() throws Exception {
        super.tearDown();
        for (int i = 0; i < folders.length; i++) {
            assertFalse(folders[i] + " is still in use!", isOpen(folders[i]));
        }
    }

    public void testRenameSubfolder() throws ProtocolException, IOException,
            MessagingException, MailboxManagerException {
        verifyCommand(new LoginCommand(USER_NAME, USER_PASSWORD));

        verifyCommand(new RenameClientCommand("test1.test1a", "test1.test1neu"));

        String[] expected = { USER_MAILBOX_ROOT + ".INBOX",
                USER_MAILBOX_ROOT + ".test", USER_MAILBOX_ROOT + ".test1",
                USER_MAILBOX_ROOT + ".test1.test1neu",
                USER_MAILBOX_ROOT + ".test1.test1b",
                USER_MAILBOX_ROOT + ".test2.test2a",
                USER_MAILBOX_ROOT + ".test2.test2b" };
        verifyFolderList(expected, getFolderNames());

        verifyCommand(new LogoutClientCommand());
    }

    public void testRenameParentfolder() throws ProtocolException, IOException,
            MessagingException, MailboxManagerException {
        verifyCommand(new LoginCommand(USER_NAME, USER_PASSWORD));

        verifyCommand(new RenameClientCommand("test1", "test2"));

        String[] expected = { USER_MAILBOX_ROOT + ".INBOX",
                USER_MAILBOX_ROOT + ".test", USER_MAILBOX_ROOT + ".test2",
                USER_MAILBOX_ROOT + ".test2.test1a",
                USER_MAILBOX_ROOT + ".test2.test1b",
                USER_MAILBOX_ROOT + ".test2.test2a",
                USER_MAILBOX_ROOT + ".test2.test2b" };
        verifyFolderList(expected, getFolderNames());

        verifyCommand(new LogoutClientCommand());
    }

}
