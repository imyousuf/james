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
package org.apache.james.queue.jms;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.UUID;

import javax.jms.ConnectionFactory;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.commons.logging.impl.SimpleLog;
import org.apache.james.core.MailImpl;
import org.apache.james.queue.api.MailQueue.MailQueueItem;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import junit.framework.TestCase;

public class JMSMailQueueTest extends TestCase{
    private JMSMailQueue queue;
    private BrokerService broker;
    
    public void setUp() throws Exception{
        broker = createBroker();
        broker.start();
        
        ConnectionFactory connectionFactory = createConnectionFactory();
        queue = createQueue(connectionFactory);
        
        super.setUp();
        
    }

    protected ActiveMQConnectionFactory createConnectionFactory() {
        return new ActiveMQConnectionFactory("vm://localhost?create=false");
    }
    protected BrokerService createBroker() throws Exception {
        BrokerService broker = new BrokerService();
        broker.setPersistent(false);
        broker.setUseJmx(false);
        broker.addConnector("tcp://127.0.0.1:61616");
        return broker;
        
    }
    
    protected JMSMailQueue createQueue(ConnectionFactory factory) {
        SimpleLog log = new SimpleLog("MockLog");
        log.setLevel(SimpleLog.LOG_LEVEL_DEBUG);
        JMSMailQueue queue = new JMSMailQueue(factory, "testqueue", log );
        return queue;
    }
    
    

    @Override
    protected void tearDown() throws Exception {
        if (broker != null) {
            broker.stop();
        }
    }

    public void testFIFO() throws MessagingException, InterruptedException, IOException {
        // should be empty
        assertEquals(0, queue.getSize());
        
        Mail mail = createMail();
        Mail mail2 =createMail();

        queue.enQueue(mail);
        queue.enQueue(mail2);
        
        Thread.sleep(200);
        
        assertEquals(2, queue.getSize());
        
        MailQueueItem item = queue.deQueue();
        checkMail(mail, item.getMail());
        item.done(false);
        
        Thread.sleep(200);

        
        // ok we should get the same email again
        assertEquals(2, queue.getSize());
        MailQueueItem item2 = queue.deQueue();
        checkMail(mail, item2.getMail());
        item2.done(true);

        Thread.sleep(200);

        
        
        assertEquals(1, queue.getSize());
        MailQueueItem item3 = queue.deQueue();
        checkMail(mail2, item3.getMail());
        item3.done(true);
        
        Thread.sleep(200);

        // should be empty
        assertEquals(0, queue.getSize());
    }
    
    private Mail createMail() throws MessagingException {
        MailImpl mail = new MailImpl();
        mail.setName("" + System.currentTimeMillis());
        mail.setAttribute("test1", System.currentTimeMillis());
        mail.setErrorMessage(UUID.randomUUID().toString());
        mail.setLastUpdated(new Date());
        mail.setRecipients(Arrays.asList(new MailAddress("test@test"), new MailAddress("test@test2")));
        mail.setSender(new MailAddress("sender@senderdomain"));
        
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        message.setText("test");
        message.setHeader("testheader", "testvalie");
        message.saveChanges();
        mail.setMessage(message);
        return mail;

    }
    @SuppressWarnings("unchecked")
    private void checkMail(Mail enqueuedMail, Mail dequeuedMail) throws MessagingException, IOException {
        assertEquals(enqueuedMail.getErrorMessage(), dequeuedMail.getErrorMessage());
        assertEquals(enqueuedMail.getMessageSize(), dequeuedMail.getMessageSize());
        assertEquals(enqueuedMail.getName(), dequeuedMail.getName());
        assertEquals(enqueuedMail.getRemoteAddr(), dequeuedMail.getRemoteAddr());
        assertEquals(enqueuedMail.getState(), dequeuedMail.getState());
        assertEquals(enqueuedMail.getLastUpdated(), dequeuedMail.getLastUpdated());
        assertEquals(enqueuedMail.getRemoteHost(), dequeuedMail.getRemoteHost());
        assertEquals(enqueuedMail.getSender(), dequeuedMail.getSender());
        
        assertEquals(enqueuedMail.getRecipients().size(), dequeuedMail.getRecipients().size());
        Iterator<String> attributes = enqueuedMail.getAttributeNames();
        while(attributes.hasNext()) {
            String name = attributes.next();
            assertNotNull(dequeuedMail.getAttribute(name));
        }


        MimeMessage enqueuedMsg = enqueuedMail.getMessage();
        MimeMessage dequeuedMsg = dequeuedMail.getMessage();
        Enumeration<String> enQueuedHeaders = enqueuedMsg.getAllHeaderLines();
        Enumeration<String> deQueuedHeaders = dequeuedMsg.getAllHeaderLines();
        while(enQueuedHeaders.hasMoreElements()) {
            assertEquals(enQueuedHeaders.nextElement(), deQueuedHeaders.nextElement());
            
        }
        assertFalse(deQueuedHeaders.hasMoreElements());
        
        assertEquals(enqueuedMsg.getContent(), dequeuedMsg.getContent());


    }
}
