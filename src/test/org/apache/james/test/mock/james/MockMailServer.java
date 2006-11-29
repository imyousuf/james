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

package org.apache.james.test.mock.james;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.core.MailImpl;
import org.apache.james.services.MailRepository;
import org.apache.james.services.MailServer;
import org.apache.james.smtpserver.MessageSizeException;
import org.apache.james.userrepository.MockUsersRepository;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class MockMailServer implements MailServer, Disposable {

    private final MockUsersRepository m_users = new MockUsersRepository();

    private static int m_counter = 0;
    private int m_maxMessageSizeBytes = 0;

    // private final ArrayList mails = new ArrayList();
    
    private final InMemorySpoolRepository mails = new InMemorySpoolRepository();
    private String lastMailKey = null; 

    private HashMap inboxes;
    
    private boolean virtualHosting;

    
    public MockUsersRepository getUsersRepository() {
        return m_users;
    }

    public void sendMail(MailAddress sender, Collection recipients, MimeMessage msg) throws MessagingException {
        //        Object[] mailObjects = new Object[]{sender, recipients, new MimeMessageCopyOnWriteProxy(msg)};
//        mails.add(mailObjects);
//        
        String newId = newId();
        MailImpl m = new MailImpl(newId, sender, recipients, msg);
        sendMail(m);
        m.dispose();
    }

    public void sendMail(MailAddress sender, Collection recipients, InputStream msg) throws MessagingException {
//        Object[] mailObjects = new Object[]{sender, recipients, msg};
//        mails.add(mailObjects);
        MailImpl m = new MailImpl(newId(), sender, recipients, msg);
        sendMail(m);
        m.dispose();
    }

    public void sendMail(Mail mail) throws MessagingException {
        int bodySize = mail.getMessage().getSize();
        try {
            if (m_maxMessageSizeBytes != 0 && m_maxMessageSizeBytes < bodySize) throw new MessageSizeException();
        } catch (MessageSizeException e) {
            throw new MessagingException("message size exception is nested", e);
        }
        
        lastMailKey = mail.getName();
        mails.store(mail);
        // sendMail(mail.getSender(), mail.getRecipients(), mail.getMessage());
    }

    public void sendMail(MimeMessage message) throws MessagingException {
        // taken from class org.apache.james.James 
        MailAddress sender = new MailAddress((InternetAddress)message.getFrom()[0]);
        Collection recipients = new HashSet();
        Address addresses[] = message.getAllRecipients();
        if (addresses != null) {
            for (int i = 0; i < addresses.length; i++) {
                // Javamail treats the "newsgroups:" header field as a
                // recipient, so we want to filter those out.
                if ( addresses[i] instanceof InternetAddress ) {
                    recipients.add(new MailAddress((InternetAddress)addresses[i]));
                }
            }
        }
        sendMail(sender, recipients, message);
    }

    public MailRepository getUserInbox(String userName) {
        if (inboxes==null) {
            return null;
        } else {
            if ((userName.indexOf("@") < 0) == false && supportVirtualHosting() == false) userName = userName.split("@")[0]; 
            return (MailRepository) inboxes.get(userName);
        }
        
    }
    
    public void setUserInbox(String userName, MailRepository inbox) {
        if (inboxes == null) {
            inboxes = new HashMap();
        }
        inboxes.put(userName,inbox);
    }

    public Map getRepositoryCounters() {
        return null; // trivial implementation 
    }


    public synchronized String getId() {
        return newId();
    }

    public static String newId() {
        m_counter++;
        return "MockMailServer-ID-" + m_counter;
    }

    public boolean addUser(String userName, String password) {
        m_users.addUser(userName, password);
        return true;
    }

    public boolean isLocalServer(String serverName) {
        return "localhost".equals(serverName);
    }

    public Mail getLastMail()
    {
        if (mails.size() == 0) return null;
        try {
            return mails.retrieve(lastMailKey);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setMaxMessageSizeBytes(int maxMessageSizeBytes) {
        m_maxMessageSizeBytes = maxMessageSizeBytes;
    }

    public void dispose() {
//        if (mails != null) {
//            Iterator i = mails.iterator();
//            while (i.hasNext()) {
//                Object[] obs = (Object[]) i.next();
//                // this is needed to let the MimeMessageWrapper to dispose.
//                ContainerUtil.dispose(obs[2]);
//            }
//        }
        mails.dispose();
        if (inboxes!=null) {
            Iterator i = inboxes.values().iterator();
            while (i.hasNext()) {
                MailRepository m = (MailRepository) i.next();
                ContainerUtil.dispose(m);
            }
        }
    }
    
    public MailRepository getSentMailsRepository() {
        return mails;
    }
    
    public void setVirtualHosting(boolean virtualHosting) {
        this.virtualHosting = virtualHosting;
    }

    public boolean supportVirtualHosting() {
        return virtualHosting;
    }

    public String getDefaultDomain() {
        return "localhost";
    }
}


