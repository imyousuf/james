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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.james.core.MailImpl;
import org.apache.james.core.MimeMessageCopyOnWriteProxy;
import org.apache.james.core.MimeMessageInputStreamSource;
import org.apache.james.queue.MailQueue;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

/**
 * {@link MailQueue} implementation which use a JMS Queue for the
 * {@link MailQueue}. This implementation should work with every JMS 1.1.0
 * implementation
 * 
 * 
 */
public class JMSMailQueue implements MailQueue {

    protected final String queuename;
    protected final ConnectionFactory connectionFactory;
    protected final Log logger;
    protected final static String JAMES_MAIL_RECIPIENTS = "JAMES_MAIL_RECIPIENTS";
    protected final static String JAMES_MAIL_SENDER = "JAMES_MAIL_SENDER";
    protected final static String JAMES_MAIL_ERROR_MESSAGE = "JAMES_MAIL_ERROR_MESSAGE";
    protected final static String JAMES_MAIL_LAST_UPDATED = "JAMES_MAIL_LAST_UPDATED";
    protected final static String JAMES_MAIL_MESSAGE_SIZE = "JAMES_MAIL_MESSAGE_SIZE";
    protected final static String JAMES_MAIL_NAME = "JAMES_MAIL_NAME";
    protected final static String JAMES_MAIL_SEPERATOR = ";";
    protected final static String JAMES_MAIL_REMOTEHOST = "JAMES_MAIL_REMOTEHOST";
    protected final static String JAMES_MAIL_REMOTEADDR = "JAMES_MAIL_REMOTEADDR";
    protected final static String JAMES_MAIL_STATE = "JAMES_MAIL_STATE";
    protected final static String JAMES_MAIL_ATTRIBUTE_NAMES = "JAMES_MAIL_ATTRIBUTE_NAMES";
    protected final static String JAMES_NEXT_DELIVERY = "JAMES_NEXT_DELIVERY";

    /**
     * Handle mail with lowest priority
     */
    public final static int LOW_PRIORITY = 0;

    /**
     * Handle mail with normal priority (this is the default)
     */
    public final static int NORMAL_PRIORITY = Message.DEFAULT_DELIVERY_MODE;

    /**
     * Handle mail with highest priority
     */
    public final static int HIGH_PRIORITY = 9;

    /**
     * Attribute name for support if priority. If the attribute is set and
     * priority handling is enabled it will take care of move the Mails with
     * higher priority to the head of the queue (so the mails are faster
     * handled).
     * 
     */
    public final static String MAIL_PRIORITY = "MAIL_PRIORITY";

    public JMSMailQueue(final ConnectionFactory connectionFactory, final String queuename, final Log logger) {
        this.connectionFactory = connectionFactory;
        this.queuename = queuename;
        this.logger = logger;
    }

/**
     * Execute the given {@link DequeueOperation} when a mail is ready to precoess. As JMS does not support delay scheduling out-of-the box, we use 
     * a messageselector to check if a mail is ready. For this a {@link MessageConsumer#receive(long) is used with a timeout of 10 seconds. 
     * 
     * Many JMS implementations support better solutions for this, so this should get overridden by these implementations
     * 
     * @see org.apache.james.queue.MailQueue#deQueue(org.apache.james.queue.MailQueue.DequeueOperation)
     */
    public MailQueueItem deQueue() throws MailQueueException {
        Connection connection = null;
        Session session = null;
        Message message = null;
        MessageConsumer consumer = null;

        while (true) {
            try {
                connection = connectionFactory.createConnection();
                connection.start();

                session = connection.createSession(true, Session.SESSION_TRANSACTED);
                Queue queue = session.createQueue(queuename);
                consumer = session.createConsumer(queue, JAMES_NEXT_DELIVERY + " <= " + System.currentTimeMillis());

                message = consumer.receive(10000);

                if (message != null) {
                    return createMailQueueItem(connection, session, consumer, message);
                } else {
                    session.commit();

                    if (consumer != null) {

                        try {
                            consumer.close();
                        } catch (JMSException e1) {
                            // ignore on rollback
                        }
                    }
                    try {
                        if (session != null)
                            session.close();
                    } catch (JMSException e1) {
                        // ignore here
                    }

                    try {
                        if (connection != null)
                            connection.close();
                    } catch (JMSException e1) {
                        // ignore here
                    }
                }

            } catch (Exception e) {
                try {
                    session.rollback();
                } catch (JMSException e1) {
                    // ignore on rollback
                }

                if (consumer != null) {

                    try {
                        consumer.close();
                    } catch (JMSException e1) {
                        // ignore on rollback
                    }
                }
                try {
                    if (session != null)
                        session.close();
                } catch (JMSException e1) {
                    // ignore here
                }

                try {
                    if (connection != null)
                        connection.close();
                } catch (JMSException e1) {
                    // ignore here
                }
                throw new MailQueueException("Unable to dequeue next message", e);
            }
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.queue.MailQueue#enQueue(org.apache.mailet.Mail,
     * long, java.util.concurrent.TimeUnit)
     */
    public void enQueue(Mail mail, long delay, TimeUnit unit) throws MailQueueException {
        Connection connection = null;
        Session session = null;
        MessageProducer producer = null;

        long mydelay = 0;

        if (delay > 0) {
            mydelay = TimeUnit.MILLISECONDS.convert(delay, unit);
        }

        try {

            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(queuename);
            producer = session.createProducer(queue);
            Message message = createMessage(session, mail, mydelay);
            populateJMSProperties(message, mail, mydelay);

            int msgPrio = NORMAL_PRIORITY;
            Object prio = mail.getAttribute(MAIL_PRIORITY);
            if (prio instanceof Integer) {
                msgPrio = (Integer) prio;
            }

            producer.send(message, Message.DEFAULT_DELIVERY_MODE, msgPrio, Message.DEFAULT_TIME_TO_LIVE);
        } catch (Exception e) {
            if (session != null) {
                try {
                    session.rollback();
                } catch (JMSException e1) {
                    // ignore on rollback
                }
            }
            throw new MailQueueException("Unable to enqueue mail " + mail, e);

        } finally {

            try {
                if (producer != null)
                    producer.close();
            } catch (JMSException e) {
                // ignore here
            }
            try {
                if (session != null)
                    session.close();
            } catch (JMSException e) {
                // ignore here
            }

            try {
                if (connection != null)
                    connection.close();
            } catch (JMSException e) {
                // ignore here
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.queue.MailQueue#enQueue(org.apache.mailet.Mail)
     */
    public void enQueue(Mail mail) throws MailQueueException {
        enQueue(mail, -1, TimeUnit.MILLISECONDS);
    }

    /**
     * Create Message which holds the {@link MimeMessage} of the given Mail
     * 
     * @param session
     * @param mail
     * @return jmsMessage
     * @throws JMSException
     * @throws IOException
     * @throws MessagingException
     * @throws IOException
     */
    protected Message createMessage(Session session, Mail mail, long delayInMillis) throws JMSException, MessagingException, IOException {
        BytesMessage message = session.createBytesMessage();
        mail.getMessage().writeTo(new BytesMessageOutputStream(message));
        
        return message;
    }

    /**
     * Populate JMS Message properties with values
     * 
     * @param message
     * @param mail
     * @param delayInMillis
     * @throws JMSException
     * @throws MessagingException
     */
    @SuppressWarnings("unchecked")
    protected void populateJMSProperties(Message message, Mail mail, long delayInMillis) throws JMSException, MessagingException {
        long nextDelivery = -1;
        if (delayInMillis > 0) {
            nextDelivery = System.currentTimeMillis() + delayInMillis;

        }
        message.setLongProperty(JAMES_NEXT_DELIVERY, nextDelivery);
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

    }

    /**
     * Create the complete Mail from the JMS Message. So the created
     * {@link Mail} is completly populated
     * 
     * @param message
     * @return
     * @throws MessagingException
     * @throws JMSException
     */
    protected final Mail createMail(Message message) throws MessagingException, JMSException {
        MailImpl mail = new MailImpl();
        populateMail(message, mail);
        populateMailMimeMessage(message, mail);

        return mail;
    }

    /**
     * Populat the given {@link Mail} instance with a {@link MimeMessage}. The
     * {@link MimeMessage} is read from the JMS Message. This implementation use
     * a {@link BytesMessage}
     * 
     * @param message
     * @param mail
     * @throws MessagingException
     */
    protected void populateMailMimeMessage(Message message, Mail mail) throws MessagingException {
        if (message instanceof BytesMessage) {
            mail.setMessage(new MimeMessageCopyOnWriteProxy(new MimeMessageInputStreamSource(mail.getName(), new BytesMessageInputStream((BytesMessage) message))));
        } else {
            throw new MailQueueException("Not supported JMS Message received " + message);
        }

    }

    /**
     * Populate Mail with values from Message. This exclude the
     * {@link MimeMessage}
     * 
     * @param message
     * @param mail
     * @throws JMSException
     */
    protected void populateMail(Message message, MailImpl mail) throws JMSException {

        mail.setErrorMessage(message.getStringProperty(JAMES_MAIL_ERROR_MESSAGE));
        mail.setLastUpdated(new Date(message.getLongProperty(JAMES_MAIL_LAST_UPDATED)));
        mail.setName(message.getStringProperty(JAMES_MAIL_NAME));

        List<MailAddress> rcpts = new ArrayList<MailAddress>();
        String recipients = message.getStringProperty(JAMES_MAIL_RECIPIENTS);
        StringTokenizer recipientTokenizer = new StringTokenizer(recipients, JAMES_MAIL_SEPERATOR);
        while (recipientTokenizer.hasMoreTokens()) {
            try {
                MailAddress rcpt = new MailAddress(recipientTokenizer.nextToken());
                rcpts.add(rcpt);
            } catch (AddressException e) {
                // Should never happen as long as the user does not modify the
                // the header by himself
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
                // Should never happen as long as the user does not modify the
                // the header by himself
                // Maybe we should log it anyway
            }
        }

        mail.setState(message.getStringProperty(JAMES_MAIL_STATE));

    }

    /**
     * Convert the attribute value if necessary.
     * 
     * @param value
     * @return convertedValue
     */
    protected Object convertAttributeValue(Object value) {
        if (value == null || value instanceof String || value instanceof Byte || value instanceof Long || value instanceof Double || value instanceof Boolean || value instanceof Integer || value instanceof Short || value instanceof Float) {
            return value;
        }
        return value.toString();
    }

    @Override
    public String toString() {
        return "MailQueue:" + queuename;
    }

    protected MailQueueItem createMailQueueItem(Connection connection, Session session, MessageConsumer consumer, Message message) throws JMSException, MessagingException {
        final Mail mail = createMail(message);
        return new JMSMailQueueItem(mail, connection, session, consumer);
    }

}
