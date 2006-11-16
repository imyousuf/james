package org.apache.james.imapserver.handler.session;

import java.io.IOException;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.imapserver.ProtocolException;
import org.apache.james.imapserver.client.LoginCommand;
import org.apache.james.imapserver.client.SelectCommand;
import org.apache.james.imapserver.client.StatusClientCommand;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.imapserver.util.MessageGenerator;
import org.apache.james.mailboxmanager.MailboxManagerException;

public class StatusSessionTest extends AbstractSessionTest {
    
    String[] folders = {USER_INBOX,USER_MAILBOX_ROOT+".f2"};
    MimeMessage[] f2_msgs= null;
    MimeMessage[] inbox_msgs= new MimeMessage[0];
    
    boolean setup=false;
    private long f2_uidV;
    private long f2_uidNext;
    
    public void setUp() throws MailboxManagerException, MailboxException, MessagingException, IOException {
        super.setUp();
        f2_msgs=MessageGenerator.generateSimpleMessages(4);
        createFolders(folders);
        appendMessagesClosed(folders[1], new MimeMessage[]{f2_msgs[0]});
        f2_msgs[1].setFlag(Flags.Flag.SEEN, true);
        f2_msgs[2].setFlag(Flags.Flag.SEEN, true);
        addUIDMessagesOpen(folders[1], new MimeMessage[]{f2_msgs[1],f2_msgs[2],f2_msgs[3]});
        f2_uidV = getUidValidity(folders[1]);
        f2_uidNext = getUidNext(folders[1]);
    }
    
    protected void doTestStatus() throws MessagingException, ProtocolException, IOException {
        StatusClientCommand statusCommand;
        
        // Empty
        statusCommand = new StatusClientCommand(folders[1],f2_msgs,f2_uidNext,f2_uidV);
        verifyCommand(statusCommand);

        // One
        statusCommand = new StatusClientCommand(folders[1],f2_msgs,f2_uidNext,f2_uidV);
        statusCommand.setStatusMessages(true);
        verifyCommand(statusCommand);
        
        // Some
        statusCommand = new StatusClientCommand(folders[1],f2_msgs,f2_uidNext,f2_uidV);
        statusCommand.setStatusMessages(true);
        statusCommand.setStatusUidNext(true);
        verifyCommand(statusCommand);
        
        // All
        statusCommand = new StatusClientCommand(folders[1],f2_msgs,f2_uidNext,f2_uidV);
        statusCommand.setStatusMessages(true);
        statusCommand.setStatusRecent(true);
        statusCommand.setStatusUidNext(true);
        statusCommand.setStatusUidValidity(true);
        statusCommand.setStatusUnseen(true);
        verifyCommand(statusCommand);
    }

    
    public void testStatusAuthState() throws ProtocolException, IOException, MessagingException {
        verifyCommand(new LoginCommand(USER_NAME,USER_PASSWORD));
        doTestStatus();
    }
    
    
    public void testStatusSelectedState() throws ProtocolException, IOException, MessagingException {
        verifyCommand(new LoginCommand(USER_NAME,USER_PASSWORD));
        verifyCommand(new SelectCommand("INBOX", inbox_msgs, getUidValidity(USER_INBOX)));
        doTestStatus();
    }
    

}
