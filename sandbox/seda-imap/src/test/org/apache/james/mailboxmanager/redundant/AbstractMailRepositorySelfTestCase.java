package org.apache.james.mailboxmanager.redundant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.mailet.Mail;

public abstract class AbstractMailRepositorySelfTestCase extends AbstractMailRepositoryTestCase {

    /*
     * Test method for
     * 'org.apache.james.mailrepository.UIDPlusFolderMailRepository.retrieve(String)'
     */
    public void testStoreRetrieve() throws MessagingException, IOException {
        int addedCount = 0;
        Collection added=new ArrayList();
        for (int i = 1; i < 10; i++) {
            Mail[] ms = new Mail[i];
            
            for (int j = 0; j < i; j++) {
                ms[j] = generateMail();
                added.add(ms[j].getMessage());
                mailRepository.store(ms[j]);
                addedCount++;
                assertNativeMessageCountEquals(addedCount);
            }
            assertNativeMessagesEqual(added);
            for (int j = 0; j < i; j++) {
                Mail originalMail = ms[j];
                Mail retrievedMail = mailRepository.retrieve(ms[j].getName());
                assertNotNull("Mail is null", retrievedMail);
                assertEquals("keys differs!", originalMail.getName(),
                        retrievedMail.getName());
                assertEquals("subject differs", originalMail.getMessage()
                        .getSubject(), retrievedMail.getMessage().getSubject());
                assertTrue("content differs!", contentEquals(originalMail
                        .getMessage(), retrievedMail.getMessage()));
            }
        }
    }
    
    public void testStoreUpdateRetrieve() throws MessagingException, IOException {
        Mail[] ms = new Mail[15];
        MimeMessage[] mms = new MimeMessage[15];
        int addedCount = 0;
        for (int j = 0; j < 15; j++) {
            ms[j] = generateMail();
            mms[j]=ms[j].getMessage();
            mailRepository.store(ms[j]);
            addedCount++;
            assertNativeMessageCountEquals(addedCount);
        }
        assertNativeMessagesEqual(Arrays.asList(mms));
        System.out.println(" ####### Test: doing updates #####");
        for (int j = 5; j < 10 ; j++) {
            Mail m= generateMail();
            m.setName(ms[j].getName());
            mailRepository.store(m);
            mms[j]=m.getMessage();
            assertNativeMessageCountEquals(addedCount);
        }
        assertNativeMessagesEqual(Arrays.asList(mms));
        for (int j = 0; j < 15; j++) {
            Mail m=mailRepository.retrieve(ms[j].getName());
            assertTrue(contentEquals(mms[j], m.getMessage()));
        }
    }
    
    class RemoveThread extends Thread {
        
        String key;
        boolean exception=false;        
        
        RemoveThread(String key) {
            super("rt");
            this.key=key;
        }
        
        public void run() {
            try {
                mailRepository.remove(key);
            } catch (MessagingException e) {
                exception=true;
            }
        }
        public synchronized void doWait(){
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
    }
    
    /*
     * Test method for
     * 'org.apache.james.mailrepository.UIDPlusFolderMailRepository.lock(String)'
     */
    public void testLock() throws MessagingException {
        Mail m1=generateMail();
        Mail m2=generateMail();
        mailRepository.store(m1);
        mailRepository.store(m2);
        assertNativeMessageCountEquals(2);
        
        // Try to remove locked message by different Thread
        mailRepository.lock(m1.getName());
        RemoveThread rt=new RemoveThread(m1.getName());
        rt.start();
        rt.doWait();
        assertTrue("RemoveThread1 should have thrown exception",rt.exception);
        assertNativeMessageCountEquals(2);
        
        // Try to remove unlocked message by different Thread
        mailRepository.unlock(m1.getName());
        rt=new RemoveThread(m1.getName());
        rt.start();
        rt.doWait();
        assertFalse("RemoveThread2 should not have thrown an exception",rt.exception);
        assertNativeMessageCountEquals(1);
        
        // try to remove locked message by same Thread
        mailRepository.lock(m2.getName());
        mailRepository.remove(m2.getName());
        assertNativeMessageCountEquals(0);
    }


}
