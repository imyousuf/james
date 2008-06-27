package org.apache.james.experimental.imapserver.handler.session;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.experimental.imapserver.ProtocolException;
import org.apache.james.experimental.imapserver.client.CreateClientCommand;
import org.apache.james.experimental.imapserver.client.LoginCommand;
import org.apache.james.experimental.imapserver.client.LogoutClientCommand;
import org.apache.james.experimental.imapserver.client.SelectCommand;
import org.apache.james.experimental.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.MailboxManagerException;

public class CreateSessionTest extends AbstractSessionTest {
    
    String[] folders = {USER_MAILBOX_ROOT+".INBOX",USER_MAILBOX_ROOT+".test",USER_MAILBOX_ROOT+".test1",USER_MAILBOX_ROOT+".test1.test1a",USER_MAILBOX_ROOT+".test1.test1b",USER_MAILBOX_ROOT+".test2.test2a",USER_MAILBOX_ROOT+".test2.test2b"};
    
    public void setUp() throws MailboxException, MessagingException, IOException, MailboxManagerException {
        super.setUp();
        createFolders(folders);
    }
    
    public void testCreateSelect() throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        verifyCommand(new LoginCommand(USER_NAME,USER_PASSWORD));
        
        verifyCommand(new CreateClientCommand("Drafts"));
        assertTrue(folderExists(USER_MAILBOX_ROOT+".Drafts"));
        
        verifyCommand(new SelectCommand("Drafts", new MimeMessage[0],getUidValidity(USER_MAILBOX_ROOT+".Drafts")));
        
        verifyCommand(new LogoutClientCommand());
    }

}
