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
package org.apache.james.queue.activemq;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

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
import javax.mail.internet.MimeMessage;

import org.apache.activemq.ActiveMQSession;
import org.apache.activemq.BlobMessage;
import org.apache.activemq.ScheduledMessage;
import org.apache.commons.logging.Log;
import org.apache.james.core.MimeMessageCopyOnWriteProxy;
import org.apache.james.core.MimeMessageInputStream;
import org.apache.james.core.MimeMessageInputStreamSource;
import org.apache.james.core.MimeMessageSource;
import org.apache.james.core.MimeMessageWrapper;
import org.apache.james.core.NonClosingSharedInputStream;
import org.apache.james.queue.api.MailQueue;
import org.apache.james.queue.jms.JMSMailQueue;
import org.apache.mailet.Mail;
import org.springframework.jms.connection.SessionProxy;

/**
 * *{@link MailQueue} implementation which use an ActiveMQ Queue.
 * 
 * This implementation require at ActiveMQ 5.4.0+.
 * 
 * When a {@link Mail} attribute is found and is not one of the supported
 * primitives, then the toString() method is called on the attribute value to
 * convert it
 * 
 * The implementation use {@link BlobMessage} 
 * 
 * See http://activemq.apache.org/blob-messages.html for more details
 * 
 * 
 * Some other supported feature is handling of priorities. See:
 * 
 * http://activemq.apache.org/how-can-i-support-priority-queues.html
 * 
 * For this just add a {@link Mail} attribute with name {@link #MAIL_PRIORITY}
 * to it. It should use one of the following value {@link #LOW_PRIORITY},
 * {@link #NORMAL_PRIORITY}, {@link #HIGH_PRIORITY}
 * 
 * To have a good throughput you should use a caching connection factory.
 * 
 * 
 */
public class ActiveMQMailQueue extends JMSMailQueue implements ActiveMQSupport{


    /**
     * Construct a new ActiveMQ based {@link MailQueue}. The messageTreshold is
     * used to calculate if a {@link BytesMessage} or a {@link BlobMessage}
     * should be used when queuing the mail in ActiveMQ. A {@link BlobMessage}
     * is used If the message size is bigger then the messageTreshold. The size
     * if in bytes.
     * 
     * 
     * For enabling the priority feature in AMQ see:
     * 
     * http://activemq.apache.org/how-can-i-support-priority-queues.html
     * 
     * @param connectionFactory
     * @param queuename
     * @param logger
     */
    public ActiveMQMailQueue(final ConnectionFactory connectionFactory, final String queuename, final Log logger) {
        super(connectionFactory, queuename, logger);
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.queue.jms.JMSMailQueue#deQueue()
     */
    public MailQueueItem deQueue() throws MailQueueException {
        Connection connection = null;
        Session session = null;
        Message message = null;
        MessageConsumer consumer = null;

        try {
            connection = connectionFactory.createConnection();
            connection.start();

            session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue queue = session.createQueue(queuename);
            consumer = session.createConsumer(queue);

            message = consumer.receive();
            return createMailQueueItem(connection, session, consumer, message);

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

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.queue.jms.JMSMailQueue#populateMailMimeMessage(javax
     * .jms.Message, org.apache.mailet.Mail)
     */
    @SuppressWarnings("unchecked")
    protected void populateMailMimeMessage(Message message, Mail mail) throws MessagingException {
        if (message instanceof BlobMessage) {
            try {
                BlobMessage blobMessage = (BlobMessage) message;
                try {
                    // store URL and queuenamefor later usage
                    mail.setAttribute(JAMES_BLOB_URL, blobMessage.getURL());
                    mail.setAttribute(JAMES_QUEUE_NAME, queuename);
                } catch (MalformedURLException e) {
                    // Ignore on error
                    logger.debug("Unable to get url from blobmessage for mail " + mail.getName());
                }
                InputStream in = blobMessage.getInputStream();
                MimeMessageSource source;
  
                if (in instanceof NonClosingSharedInputStream) {
                    source = new MimeMessageBlobMessageSource(blobMessage);
                } else {
                    source = new MimeMessageInputStreamSource(mail.getName(), blobMessage.getInputStream());
                }
                mail.setMessage(new MimeMessageCopyOnWriteProxy(source));
            } catch (IOException e) {
                throw new MailQueueException("Unable to populate MimeMessage for mail " + mail.getName(), e);
            } catch (JMSException e) {
                throw new MailQueueException("Unable to populate MimeMessage for mail " + mail.getName(), e);
            }
        } else {
            super.populateMailMimeMessage(message, mail);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.queue.jms.JMSMailQueue#createMessage(javax.jms.Session,
     * org.apache.mailet.Mail, long)
     */
    protected void produceMail(Session session, Map<String,Object> props, int msgPrio, Mail mail) throws JMSException, MessagingException, IOException {
        MessageProducer producer = null;
        try {
            
            BlobMessage blobMessage = null;
            MimeMessage mm = mail.getMessage();
            MimeMessage wrapper = mm;
            
            ActiveMQSession amqSession = getAMQSession(session);
            
            if (wrapper instanceof MimeMessageCopyOnWriteProxy) {
                wrapper = ((MimeMessageCopyOnWriteProxy)mm).getWrappedMessage();
            }
            
            if (wrapper instanceof MimeMessageWrapper) {
                URL blobUrl = (URL) mail.getAttribute(JAMES_BLOB_URL);
                String fromQueue = (String) mail.getAttribute(JAMES_QUEUE_NAME);
                MimeMessageWrapper mwrapper = (MimeMessageWrapper) wrapper;

                if (blobUrl != null && fromQueue != null && fromQueue.equals(queuename) && mwrapper.isModified() == false ) {
                    // the message content was not changed so don't need to upload it again and can just point to the url
                    blobMessage = amqSession.createBlobMessage(blobUrl);
                    
                    // thats important so we don't delete the blob file after complete the processing!
                    mail.setAttribute(JAMES_REUSE_BLOB_URL, true);
                    
                }

            }
            if (blobMessage == null) {
                // just use the MimeMessageInputStream which can read every MimeMessage implementation
                blobMessage = amqSession.createBlobMessage(new MimeMessageInputStream(wrapper));
            }
            
            // store the queue name in the props
            props.put(JAMES_QUEUE_NAME, queuename);

              
            Queue queue = session.createQueue(queuename);

            producer = session.createProducer(queue);
            Iterator<String> keys = props.keySet().iterator();
            while (keys.hasNext()) {
                String key = keys.next();
                blobMessage.setObjectProperty(key, props.get(key));
            }
            producer.send(blobMessage, Message.DEFAULT_DELIVERY_MODE, msgPrio, Message.DEFAULT_TIME_TO_LIVE);
        } finally {

            try {
                if (producer != null)
                    producer.close();
            } catch (JMSException e) {
                // ignore here
            }
        }
      
    }

    /**
     * Cast the given {@link Session} to an {@link ActiveMQSession}
     * 
     * @param session
     * @return amqSession
     * @throws JMSException
     */
    protected ActiveMQSession getAMQSession(Session session) throws JMSException {
        ActiveMQSession amqSession;
        
        if (session instanceof SessionProxy) {
            // handle Springs CachingConnectionFactory 
            amqSession = (ActiveMQSession) ((SessionProxy) session).getTargetSession();
        } else {
            // just cast as we have no other idea
            amqSession = (ActiveMQSession) session;
        }
        return amqSession;
    }
    

    @Override
    protected Map<String, Object> getJMSProperties(Mail mail, long delayInMillis) throws JMSException, MessagingException {
        Map<String, Object> props =  super.getJMSProperties(mail, delayInMillis);
       
        // add JMS Property for handling message scheduling
        // http://activemq.apache.org/delay-and-schedule-message-delivery.html
        if (delayInMillis > 0) {
            props.put(ScheduledMessage.AMQ_SCHEDULED_DELAY, delayInMillis);
        }
        return props;
    }

    @Override
    protected MailQueueItem createMailQueueItem(Connection connection, Session session, MessageConsumer consumer, Message message) throws JMSException, MessagingException {
        Mail mail = createMail(message);
        return new ActiveMQMailQueueItem(mail, connection, session, consumer, message, logger);
    }

}
