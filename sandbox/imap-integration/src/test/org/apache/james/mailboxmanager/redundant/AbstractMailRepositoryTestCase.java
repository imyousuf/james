/***********************************************************************
 * Copyright (c) 1999-2006 The Apache Software Foundation.             *
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
package org.apache.james.mailboxmanager.redundant;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import junit.framework.TestCase;

import org.apache.james.core.MailImpl;
import org.apache.james.mailrepository.javamail.HashJavamailStoreMailRepository;
import org.apache.james.services.MailRepository;
import org.apache.mailet.Mail;

import com.sun.mail.util.CRLFOutputStream;

public abstract class AbstractMailRepositoryTestCase extends TestCase {

    protected MailRepository mailRepository;

    private static Random random;

    protected void setUp() throws Exception {
        super.setUp();
        destroyRepository();        
        configureRepository();
        assertNotNull(mailRepository);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        destroyRepository();        
        mailRepository = null;
    }

    protected abstract void configureRepository() throws Exception;

    protected abstract void destroyRepository() throws IOException, MessagingException;

    public static Mail generateMail() throws MessagingException {
        Mail m = new MailImpl();
        MimeMessage mm = new MimeMessage((Session) null);
        int r = getRandom().nextInt() % 100000;
        int r2 = getRandom().nextInt() % 100000;
        mm.setSubject("good news" + r);
        mm.setFrom(new InternetAddress("user" + r + "@localhost"));
        mm.setRecipients(Message.RecipientType.TO,
                new InternetAddress[] { new InternetAddress("user" + r2
                        + "@localhost") });
        String text = "Hello User" + r2
                + "!\n\nhave a nice holiday.\r\n\r\ngreetings,\nUser" + r
                + "\n";
        mm.setText(text);
        m.setMessage(mm);
        String key = "james-test-" + System.currentTimeMillis() + "-"
                + getRandom().nextLong();
        m.setName(key);
        return m;
    }

    protected static boolean contentEquals(MimeMessage m1, MimeMessage m2)
            throws IOException, MessagingException {
        ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
        m1.writeTo(new CRLFOutputStream(baos1));
        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        m2.writeTo(new CRLFOutputStream(baos2));
        return Arrays.equals(baos1.toByteArray(), baos2.toByteArray());
    }

    protected static int messageHashSum(MimeMessage mm)
            throws IOException, MessagingException {
        HashJavamailStoreMailRepository.HasherOutputStream hos=new HashJavamailStoreMailRepository.HasherOutputStream(); 
        mm.writeTo(new CRLFOutputStream(hos));
        return hos.getHash();
    }
    
    protected static boolean messageSetsEqual(Collection ma1, Collection ma2)
            throws IOException, MessagingException {
        if (ma1.size() != ma2.size())
            return false;
        Set s1 = new HashSet();
        Set s2 = new HashSet();
        for (Iterator it = ma1.iterator(); it.hasNext();) {
            MimeMessage mm = (MimeMessage) it.next();
            s1.add(new Integer(messageHashSum(mm)));
        }
        for (Iterator it = ma2.iterator(); it.hasNext();) {
            MimeMessage mm = (MimeMessage) it.next();
            s2.add(new Integer(messageHashSum(mm)));
        }
        return s1.equals(s2);
    }

    protected static synchronized Random getRandom() {
        if (random == null) {
            random = new Random();
        }
        return random;

    }
    
    protected void assertNativeMessageCountEquals(int count) {
        try {
            assertEquals("message count differs", count,
                    getNativeMessageCount());
        } catch (NativeMethodNotSupportetException e) {
        }
    }

    protected void assertNativeMessagesEqual(Collection added)
            throws IOException, MessagingException {
        try {
            boolean equ = messageSetsEqual(getNativeMessages(), added);
            assertTrue("messages differ", equ);
        } catch (NativeMethodNotSupportetException e) {
        }

    }

    protected int getNativeMessageCount()
            throws NativeMethodNotSupportetException {
        throw new NativeMethodNotSupportetException(
                "AbstractMailRepositoryTestCase.getNativeMessageCount() not supported");
    }

    protected Collection getNativeMessages()
            throws NativeMethodNotSupportetException {
        throw new NativeMethodNotSupportetException(
                "AbstractMailRepositoryTestCase.getNativeMessages() not supported");
    }

    protected void nativeStoreMessage(MimeMessage mm)
            throws NativeMethodNotSupportetException {
        throw new NativeMethodNotSupportetException(
                "AbstractMailRepositoryTestCase.nativeStoreMessage() not supported");
    }

    class NativeMethodNotSupportetException extends Exception {

        public NativeMethodNotSupportetException(String string) {
            super(string);
        }

        private static final long serialVersionUID = 1477298541686913960L;

    }

}
