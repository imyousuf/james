package org.apache.james.mailboxmanager;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.james.mailboxmanager.mailbox.GeneralMailboxSession;
import org.apache.james.mailboxmanager.manager.GeneralManager;

import junit.framework.TestCase;

public abstract class AbstractMailboxManagerSelfTestCase extends TestCase {
    
    protected GeneralManager mailboxManager;
    
    
    public void testCreateList() throws MailboxManagerException {
        ListResult[] listResult;
        listResult=mailboxManager.list("","*",false);
        assertNotNull(listResult);
        assertEquals(0,mailboxManager.list("","*",false).length);
        Set boxes=new HashSet();
        boxes.add("#users.joachim.INBOX");
        boxes.add("#users.joachim.INBOX.Drafts");
        boxes.add("#users.joachim2.INBOX");
        for (Iterator iter = boxes.iterator(); iter.hasNext();) {
            String box = (String) iter.next();
            mailboxManager.createMailbox(box);    
        }
        listResult=mailboxManager.list("","*",false);
        assertEquals(3,listResult.length);
        for (int i = 0; i < listResult.length; i++) {
            assertTrue(boxes.contains(listResult[i].getName()));
        }
    }
    
    public void testGetSessionMailbox() throws MailboxManagerException {
        mailboxManager.createMailbox("#users.joachim3.INBOX");
        GeneralMailboxSession sessionMailbox=mailboxManager.getGenericGeneralMailboxSession("#users.joachim3.INBOX");
        assertNotNull(sessionMailbox);
    }
    
    public void testListOne() throws MailboxManagerException {
        mailboxManager.createMailbox("test1");    
        mailboxManager.createMailbox("INBOX");
        mailboxManager.createMailbox("INBOX2");
        
        ListResult[] listResult=mailboxManager.list("","INBOX",false);
        assertNotNull(listResult);
        assertEquals(1, listResult.length);
        assertEquals("INBOX", listResult[0].getName());
    }
    

}
