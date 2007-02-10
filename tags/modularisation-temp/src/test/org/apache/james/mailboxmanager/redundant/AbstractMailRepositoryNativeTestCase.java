package org.apache.james.mailboxmanager.redundant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.mail.MessagingException;

import org.apache.mailet.Mail;

public abstract class AbstractMailRepositoryNativeTestCase extends
        AbstractMailRepositorySelfTestCase {

    public void testStore() throws MessagingException, IOException {
        Collection added = new ArrayList();
        for (int i = 0; i < 10; i++) {
            Mail m = generateMail();
            mailRepository.store(m);
            assertNativeMessageCountEquals(i + 1);
            added.add(m.getMessage());
            assertNativeMessagesEqual(added);
        }
    }

    public void testStoreRemoveByMailOrKey() throws MessagingException,
            IOException {
        Collection added = new ArrayList();
        for (int i = 0; i < 10; i++) {
            Mail m1 = generateMail();
            Mail m2 = generateMail();
            mailRepository.store(m1);
            assertNativeMessageCountEquals(i + 1);
            mailRepository.store(m2);
            assertNativeMessageCountEquals(i + 2);
            if (i % 2 == 0) {
                mailRepository.remove(m1);
            } else {
                mailRepository.remove(m1.getName());
            }
            assertNativeMessageCountEquals(i + 1);
            added.add(m2.getMessage());
        }
        assertNativeMessagesEqual(added);
    }

    public void testStoreRemoveByCollection() throws MessagingException,
            IOException {
        Collection retain = new ArrayList();
        Collection delete = new ArrayList();

        for (int i = 0; i < 10; i++) {
            Mail m1 = generateMail();
            mailRepository.store(m1);
            assertNativeMessageCountEquals(i + 1);
            if (i % 2 == 0) {
                delete.add(m1);
            } else {
                retain.add(m1.getMessage());
            }
        }
        mailRepository.remove(delete);
        assertNativeMessageCountEquals(5);
        assertNativeMessagesEqual(retain);
    }
    
    /*
     * Test method for
     * 'org.apache.james.mailrepository.UIDPlusFolderMailRepository.list()'
     */
    public void testListRetrieve() throws MessagingException, IOException {
        try {
            Collection added = new ArrayList();
            for (int i = 0; i < 10; i++) {
                Mail m = generateMail();
                nativeStoreMessage(m.getMessage());
                added.add(m.getMessage());
            }
            assertNativeMessageCountEquals(10);
            assertNativeMessagesEqual(added);
            
            Collection retrieved = new ArrayList();
            for (Iterator it= mailRepository.list(); it.hasNext();) {
                String key = (String) it.next();
                assertNotNull("key is null",key);
                Mail m=mailRepository.retrieve(key);
                assertNotNull("Mail is null",m);
                assertNotNull("key of Mail is null",key);
                assertEquals("key differs",key,m.getName());
                retrieved.add(m.getMessage());
            }
            assertEquals("number of retrieved messages differs",10,retrieved.size());
            assertTrue("messages differ",messageSetsEqual(added,retrieved));
            
            
        } catch (NativeMethodNotSupportetException e) {

        }
    }
    public void testListRemove() throws MessagingException, IOException {
        try {
            Collection added = new ArrayList();
            for (int i = 0; i < 10; i++) {
                Mail m = generateMail();
                nativeStoreMessage(m.getMessage());
                added.add(m.getMessage());
            }
            assertNativeMessageCountEquals(10);
            int count=10;
            for (Iterator it= mailRepository.list(); it.hasNext();) {
                String key = (String) it.next();
                assertNotNull("key is null",key);
                mailRepository.remove(key);
                count--;
                assertNativeMessageCountEquals(count);
            }
        } catch (NativeMethodNotSupportetException e) {

        }
    }


}
