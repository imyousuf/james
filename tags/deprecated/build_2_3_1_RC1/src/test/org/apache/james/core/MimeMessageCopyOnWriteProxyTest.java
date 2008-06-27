/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.core;

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.util.SharedByteArrayInputStream;

import java.util.ArrayList;
import java.util.Properties;

public class MimeMessageCopyOnWriteProxyTest extends MimeMessageFromStreamTest {

    String content = "Subject: foo\r\nContent-Transfer-Encoding2: plain";
    String sep = "\r\n\r\n";
    String body = "bar\r\n.\r\n";

    protected MimeMessage getMessageFromSources(String sources) throws Exception {
        MimeMessageInputStreamSource mmis = null;
        try {
            mmis = new MimeMessageInputStreamSource("test", new SharedByteArrayInputStream(sources.getBytes()));
        } catch (MessagingException e) {
        }
        return new MimeMessageCopyOnWriteProxy(mmis);
//        return new MimeMessage(Session.getDefaultInstance(new Properties()),new ByteArrayInputStream(sources.getBytes()));
    }

    public void testMessageCloning1() throws Exception {
        ArrayList r = new ArrayList();
        r.add(new MailAddress("recipient@test.com"));
        MimeMessageCopyOnWriteProxy messageFromSources = (MimeMessageCopyOnWriteProxy) getMessageFromSources(content+sep+body);
        MailImpl mail = new MailImpl("test",new MailAddress("test@test.com"),r,messageFromSources);
        MailImpl m2 = (MailImpl) mail.duplicate();
        System.out.println("mail: "+getReferences(mail.getMessage())+" m2: "+getReferences(m2.getMessage()));
        assertNotSame(m2,mail);
        assertNotSame(m2.getMessage(),mail.getMessage());
        // test that the wrapped message is the same
        assertTrue(isSameMimeMessage(m2.getMessage(),mail.getMessage()));
        // test it is the same after read only operations!
        mail.getMessage().getSubject();
        assertTrue(isSameMimeMessage(m2.getMessage(),mail.getMessage()));
        mail.getMessage().setText("new body");
        mail.getMessage().saveChanges();
        // test it is different after a write operation!
        mail.getMessage().setSubject("new Subject");
        assertTrue(!isSameMimeMessage(m2.getMessage(),mail.getMessage()));
        ContainerUtil.dispose(mail);
        ContainerUtil.dispose(m2);
        ContainerUtil.dispose(messageFromSources);
    }

    
    public void testMessageCloning2() throws Exception {
        ArrayList r = new ArrayList();
        r.add(new MailAddress("recipient@test.com"));
        MimeMessageCopyOnWriteProxy messageFromSources = (MimeMessageCopyOnWriteProxy) getMessageFromSources(content+sep+body);
        MailImpl mail = new MailImpl("test",new MailAddress("test@test.com"),r,messageFromSources);
        MailImpl m2 = (MailImpl) mail.duplicate();
        System.out.println("mail: "+getReferences(mail.getMessage())+" m2: "+getReferences(m2.getMessage()));
        assertNotSame(m2,mail);
        assertNotSame(m2.getMessage(),mail.getMessage());
        // test that the wrapped message is the same
        assertTrue(isSameMimeMessage(m2.getMessage(),mail.getMessage()));
        // test it is the same after real only operations!
        m2.getMessage().getSubject();
        assertTrue(isSameMimeMessage(m2.getMessage(),mail.getMessage()));
        m2.getMessage().setText("new body");
        m2.getMessage().saveChanges();
        // test it is different after a write operation!
        m2.getMessage().setSubject("new Subject");
        assertTrue(!isSameMimeMessage(m2.getMessage(),mail.getMessage()));
        // check that the subjects are correct on both mails!
        assertEquals(m2.getMessage().getSubject(),"new Subject");
        assertEquals(mail.getMessage().getSubject(),"foo");
        // cloning again the messages
        Mail m2clone = m2.duplicate();
        assertTrue(isSameMimeMessage(m2clone.getMessage(),m2.getMessage()));
        MimeMessage mm = getWrappedMessage(m2.getMessage());
        assertNotSame(m2.getMessage(),m2clone.getMessage());
        // test that m2clone has a valid wrapped message
        MimeMessage mm3 = getWrappedMessage(m2clone.getMessage());
        assertNotNull(mm3);
        // dispose m2 and check that the clone has still a valid message and it is the same!
        ((MailImpl) m2).dispose();
        assertEquals(mm3,getWrappedMessage(m2clone.getMessage()));
        // change the message that should be not referenced by m2 that has
        // been disposed, so it should not clone it!
        m2clone.getMessage().setSubject("new Subject 2");
        m2clone.getMessage().setText("new Body 3");
        assertTrue(isSameMimeMessage(m2clone.getMessage(),mm));
        ContainerUtil.dispose(mail);
        ContainerUtil.dispose(messageFromSources);
    }
    
    /**
     * If I create a new MimeMessageCopyOnWriteProxy from another MimeMessageCopyOnWriteProxy,
     * I remove references to the first and I change the second, then it should not clone
     */
    public void testMessageAvoidCloning() throws Exception {
        ArrayList r = new ArrayList();
        r.add(new MailAddress("recipient@test.com"));
        MimeMessageCopyOnWriteProxy messageFromSources = (MimeMessageCopyOnWriteProxy) getMessageFromSources(content+sep+body);
        MailImpl mail = new MailImpl("test",new MailAddress("test@test.com"),r,messageFromSources);
        // cloning the message
        Mail mailClone = mail.duplicate();
        assertTrue(isSameMimeMessage(mailClone.getMessage(),mail.getMessage()));
        MimeMessage mm = getWrappedMessage(mail.getMessage());
        assertNotSame(mail.getMessage(),mailClone.getMessage());
        // dispose mail and check that the clone has still a valid message and it is the same!
        ((MailImpl) mail).dispose();
        ContainerUtil.dispose(messageFromSources);
        // need to add a gc and a wait, because the original mimemessage should be finalized before the test.
        System.gc();
        Thread.sleep(1000);
        // dumb test
        assertTrue(isSameMimeMessage(mailClone.getMessage(),mailClone.getMessage()));
        // change the message that should be not referenced by mail that has
        // been disposed, so it should not clone it!
        mailClone.getMessage().setSubject("new Subject 2");
        mailClone.getMessage().setText("new Body 3");
        assertTrue(isSameMimeMessage(mailClone.getMessage(),mm));
        ContainerUtil.dispose(mailClone);
        ContainerUtil.dispose(mm);
    }

    
    /**
     * If I create a new MimeMessageCopyOnWriteProxy from a MimeMessage and I change the new 
     * message, the original should be unaltered and the proxy should clone the message.
     */
    public void testMessageCloning3() throws Exception {
        ArrayList r = new ArrayList();
        r.add(new MailAddress("recipient@test.com"));
        MimeMessage m = new MimeMessage(Session.getDefaultInstance(new Properties(null)));
        m.setText("CIPS");
        MailImpl mail = new MailImpl("test",new MailAddress("test@test.com"),r,m);
        assertTrue(isSameMimeMessage(m,mail.getMessage()));
        // change the message that should be not referenced by mail that has
        // been disposed, so it should not clone it!
        System.gc();
        Thread.sleep(100);
        mail.getMessage().setSubject("new Subject 2");
        mail.getMessage().setText("new Body 3");
        System.gc();
        Thread.sleep(100);
        assertFalse(isSameMimeMessage(m,mail.getMessage()));
        ContainerUtil.dispose(mail);
        ContainerUtil.dispose(m);
    }

    
    public void testMessageDisposing() throws Exception {
        ArrayList r = new ArrayList();
        r.add(new MailAddress("recipient@test.com"));
        MimeMessageCopyOnWriteProxy messageFromSources = (MimeMessageCopyOnWriteProxy) getMessageFromSources(content+sep+body);
        MailImpl mail = new MailImpl("test",new MailAddress("test@test.com"),r,messageFromSources);
        // cloning the message
        MailImpl mailClone = (MailImpl) mail.duplicate();
        mail.dispose();

        assertNotNull(getWrappedMessage(mailClone.getMessage()));
        assertNull(mail.getMessage());

        mailClone.dispose();
        
        assertNull(mailClone.getMessage());
        assertNull(mail.getMessage());
        ContainerUtil.dispose(mail);
        ContainerUtil.dispose(messageFromSources);
    }
    
    public void testNPE1() throws MessagingException, InterruptedException {
        ArrayList recipients = new ArrayList();
        recipients.add(new MailAddress("recipient@test.com"));
        MimeMessageCopyOnWriteProxy mw = new MimeMessageCopyOnWriteProxy(
                new MimeMessageInputStreamSource(
                        "test",
                        new SharedByteArrayInputStream(
                                ("Return-path: return@test.com\r\n"+
                                 "Content-Transfer-Encoding: plain\r\n"+
                                 "Subject: test\r\n\r\n"+
                                 "Body Text\r\n").getBytes())));
        
        MimeMessageCopyOnWriteProxy mw2 = new MimeMessageCopyOnWriteProxy(mw);
        ContainerUtil.dispose(mw2);
        mw2 = null;
        System.gc();
        Thread.sleep(1000);
        // the NPE was inside this call
        mw.getMessageSize();
        ContainerUtil.dispose(mw);
    }

    
    /**
     * This test throw a NullPointerException when the original message was created by
     * a MimeMessageInputStreamSource.
     */
    public void testMessageCloningViaCoW3() throws Exception {
        MimeMessage mmorig = getSimpleMessage();
        
        MimeMessage mm = new MimeMessageCopyOnWriteProxy(mmorig);
        
        ContainerUtil.dispose(mmorig);
        mmorig = null;
        System.gc();
        Thread.sleep(200);

        try {
            mm.writeTo(System.out);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception while writing the message to output");
        }
        
        ContainerUtil.dispose(mmorig);
    }

    private static String getReferences(MimeMessage m) {
        StringBuffer ref = new StringBuffer("/");
        while (m instanceof MimeMessageCopyOnWriteProxy) {
            ref.append(((MimeMessageCopyOnWriteProxy) m).refCount.getReferenceCount()+"/");
            m = ((MimeMessageCopyOnWriteProxy) m).getWrappedMessage();
        }
        if (m instanceof MimeMessageWrapper) {
            ref.append("W");
        } else if (m instanceof MimeMessage) {
            ref.append("M");
        } else {
            ref.append(m.getClass());
        }
        return ref.toString();
    }
    
    private static MimeMessage getWrappedMessage(MimeMessage m) {
        while (m instanceof MimeMessageCopyOnWriteProxy) {
          m = ((MimeMessageCopyOnWriteProxy) m).getWrappedMessage();
        }
        return m;
    }
    
    private static boolean isSameMimeMessage(MimeMessage first, MimeMessage second) {
        return getWrappedMessage(first) == getWrappedMessage(second);
        
    }

}
