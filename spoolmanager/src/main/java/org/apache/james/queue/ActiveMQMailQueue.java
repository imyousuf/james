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
package org.apache.james.queue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.apache.james.core.MailImpl;
import org.apache.james.core.MimeMessageCopyOnWriteProxy;
import org.apache.james.core.MimeMessageInputStreamSource;
import org.apache.james.mailbox.MailboxException;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

/**
 * *{@link MailQueue} implementation which use an ActiveMQ Queue.
 * 
 * This implementation require at ActiveMQ 5.4.0+. 
 * 
 * When a {@link Mail} attribute is found and is not one of the supported primitives, then the 
 * toString() method is called on the attribute value to convert it 
 * 
 *
 */
public class ActiveMQMailQueue implements MailQueue {

    private final String queuename;
    private final ConnectionFactory connectionFactory;

    private final static String JAMES_MAIL_RECIPIENTS = "JAMES_MAIL_RECIPIENTS";
    private final static String JAMES_MAIL_SENDER = "JAMES_MAIL_SENDER";
    private final static String JAMES_MAIL_ERROR_MESSAGE = "JAMES_MAIL_ERROR_MESSAGE";
    private final static String JAMES_MAIL_LAST_UPDATED = "JAMES_MAIL_LAST_UPDATED";
    private final static String JAMES_MAIL_MESSAGE_SIZE = "JAMES_MAIL_MESSAGE_SIZE";
    private final static String JAMES_MAIL_NAME = "JAMES_MAIL_NAME";
    private final static String JAMES_MAIL_SEPERATOR = ";";
    private final static String JAMES_MAIL_REMOTEHOST = "JAMES_MAIL_REMOTEHOST";
    private final static String JAMES_MAIL_REMOTEADDR = "JAMES_MAIL_REMOTEADDR";
    private final static String JAMES_MAIL_STATE = "JAMES_MAIL_STATE";
    private final static String JAMES_MAIL_ATTRIBUTE_NAMES = "JAMES_MAIL_ATTRIBUTE_NAMES";
    private final static int NO_DELAY = -1;
    
    public ActiveMQMailQueue(final ConnectionFactory connectionFactory, final String queuename) {
        this.connectionFactory = connectionFactory;     
        this.queuename = queuename;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.queue.MailQueue#deQueue()
     */
    public void deQueue(DequeueOperation operation) throws MailboxException, MessagingException {   
        Connection connection = null;
        Session session = null;
        try {
            connection = connectionFactory.createConnection();
            connection.start();
            
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue queue = session.createQueue(queuename);
            MessageConsumer consumer = session.createConsumer(queue);
            
            Mail mail = createMail((ObjectMessage)consumer.receive());
            operation.process(mail);
            session.commit();
        } catch (JMSException e) {
            throw new MailboxException("Unable to dequeue next message", e);
        } catch (MessagingException e) {
            if (session != null) {
                try {
                    session.rollback();
                } catch (JMSException e1) {
                    // ignore on rollback
                }
            }
        } finally {
            try {
                if (session != null) session.close();
            } catch (JMSException e) {
                // ignore here
            }
            
            try {
                if (connection != null)  connection.close();
            } catch (JMSException e) {
                // ignore here
            }
        }
      
       
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.queue.MailQueue#enQueue(org.apache.mailet.Mail, long, java.util.concurrent.TimeUnit)
     */
    public void enQueue(Mail mail, long delay, TimeUnit unit) throws MailQueueException, MessagingException {
        
        long mydelay = NO_DELAY;
        
        if (delay >= 0) {
            mydelay = TimeUnit.MILLISECONDS.convert(delay, unit);
        }
        
        Connection connection = null;
        Session session = null;
        try {

            
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(queuename);
            MessageProducer producer = session.createProducer(queue);

            producer.send(createMessage(session, mail, mydelay));
        } catch (JMSException e) {
            throw new MailboxException("Unable to enqueue mail " + mail, e);
        } catch (MessagingException e) {
            if (session != null) {
                try {
                    session.rollback();
                } catch (JMSException e1) {
                    // ignore on rollback
                }
            }
        } finally {
            try {
                if (session != null) session.close();
            } catch (JMSException e) {
                // ignore here
            }
            
            try {
                if (connection != null)  connection.close();
            } catch (JMSException e) {
                // ignore here
            }
        }
      
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.queue.MailQueue#enQueue(org.apache.mailet.Mail)
     */
    public void enQueue(Mail mail) throws MailQueueException, MessagingException {
        enQueue(mail, NO_DELAY, null);
    }

    
    @Override
    public String toString() {
        return "MailQueue:" + queuename;
    }
    
    private Mail createMail(ObjectMessage message) throws MailQueueException, JMSException {
        MailImpl mail = new MailImpl();

        mail.setErrorMessage(message.getStringProperty(JAMES_MAIL_ERROR_MESSAGE));
        mail.setLastUpdated(new Date(message.getLongProperty(JAMES_MAIL_LAST_UPDATED)));
        mail.setName(message.getStringProperty(JAMES_MAIL_NAME));
        
        List<MailAddress> rcpts = new ArrayList<MailAddress>();       
        String recipients = message.getStringProperty(JAMES_MAIL_RECIPIENTS);
        StringTokenizer recipientTokenizer = new StringTokenizer(recipients, JAMES_MAIL_SEPERATOR);
        while(recipientTokenizer.hasMoreTokens()) {
            try {
                MailAddress rcpt = new MailAddress(recipientTokenizer.nextToken());
                rcpts.add(rcpt);
            } catch (AddressException e) {
                // Should never happen as long as the user does not modify the the header by himself
                // Maybe we should log it anyway
            }
        }
        mail.setRecipients(rcpts);
        mail.setRemoteAddr(message.getStringProperty(JAMES_MAIL_REMOTEADDR));
        mail.setRemoteHost(message.getStringProperty(JAMES_MAIL_REMOTEHOST));
        
        String attributeNames = message.getStringProperty(JAMES_MAIL_ATTRIBUTE_NAMES);
        StringTokenizer namesTokenizer = new StringTokenizer(attributeNames, JAMES_MAIL_SEPERATOR);
        while (namesTokenizer.hasMoreTokens()) {
            String name = namesTokenizer.nextToken();
            Serializable attrValue = message.getStringProperty(name);
            
            mail.setAttribute(name, attrValue);
        }
        
        String sender = message.getStringProperty(JAMES_MAIL_SENDER);
        if (sender == null || sender.trim().length() <= 0) {
            mail.setSender(null);
        } else {
            try {
                mail.setSender(new MailAddress(sender));
            } catch (AddressException e) {
             // Should never happen as long as the user does not modify the the header by himself
             // Maybe we should log it anyway
            }
        }
        
        mail.setState(message.getStringProperty(JAMES_MAIL_STATE));
            
        try {
            mail.setMessage(new MimeMessageCopyOnWriteProxy(new MimeMessageInputStreamSource(mail.getName(), new ByteArrayInputStream((byte[])message.getObject()))));
        } catch (MessagingException e) {
            throw new MailQueueException("Unable to prepare Mail for dequeue", e);
        }
        return mail; 
    }
    
    @SuppressWarnings("unchecked")
    private Message createMessage(Session session, Mail mail, long delayInMillis) throws MailQueueException{
        try {
            ObjectMessage message  = session.createObjectMessage();
     
            
            if (delayInMillis > 0) {
                // This will get picked up by activemq for delay message
                message.setLongProperty(org.apache.activemq.ScheduledMessage.AMQ_SCHEDULED_DELAY, delayInMillis);
            }
            
            message.setStringProperty(JAMES_MAIL_ERROR_MESSAGE, mail.getErrorMessage());
            message.setLongProperty(JAMES_MAIL_LAST_UPDATED, mail.getLastUpdated().getTime());
            message.setLongProperty(JAMES_MAIL_MESSAGE_SIZE, mail.getMessageSize());
            message.setStringProperty(JAMES_MAIL_NAME, mail.getName());
            
            StringBuilder recipientsBuilder = new StringBuilder();
            
            Iterator<MailAddress> recipients = mail.getRecipients().iterator();
            while (recipients.hasNext()) {
                String recipient = recipients.next().toString();
                recipientsBuilder.append(recipient.trim());
                if (recipients.hasNext()) {
                    recipientsBuilder.append(JAMES_MAIL_SEPERATOR);
                }
            }
            message.setStringProperty(JAMES_MAIL_RECIPIENTS, recipientsBuilder.toString());
            message.setStringProperty(JAMES_MAIL_REMOTEADDR, mail.getRemoteAddr());
            message.setStringProperty(JAMES_MAIL_REMOTEHOST, mail.getRemoteHost());
            
            String sender;
            MailAddress s = mail.getSender();
            if (s == null) {
                sender = "";
            } else {
                sender = mail.getSender().toString();
            }
            
            StringBuilder attrsBuilder = new StringBuilder();
            Iterator<String> attrs = mail.getAttributeNames();
            while (attrs.hasNext()) {
                String attrName = attrs.next();
                attrsBuilder.append(attrName);
                
                Object value = convertAttributeValue(mail.getAttribute(attrName));
                message.setObjectProperty(attrName, value);
                
                if (attrs.hasNext()) {
                    attrsBuilder.append(JAMES_MAIL_SEPERATOR);
                }
            }
            message.setStringProperty(JAMES_MAIL_ATTRIBUTE_NAMES, attrsBuilder.toString());
            message.setStringProperty(JAMES_MAIL_SENDER, sender);
            message.setStringProperty(JAMES_MAIL_STATE, mail.getState());
            
            
            ByteArrayOutputStream messageStream = new ByteArrayOutputStream();
            mail.getMessage().writeTo(messageStream);
            message.setObject(messageStream.toByteArray());
            return message;

        } catch (MessagingException e) {
            throw new MailQueueException("Unable to prepare Mail for enqueue" , e);
        } catch (IOException e) {
            throw new MailQueueException("Unable to prepare Mail for enqueue" , e);
        } catch (JMSException e) {
            throw new MailQueueException("Unable to prepare Mail for enqueue" , e);
        }
    }
    
    /**
     * Convert the attribute value if necessary. 
     * 
     * @param value
     * @return convertedValue
     */
    protected Object convertAttributeValue(Object value){
        if (value == null || value instanceof String || value instanceof Byte || value instanceof Long || value instanceof Double || value instanceof Boolean || value instanceof Integer || value instanceof Short || value instanceof Float) {
            return value;
        }
        return value.toString();
    }
}
