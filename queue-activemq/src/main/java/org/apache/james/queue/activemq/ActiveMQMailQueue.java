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
import java.util.List;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.SharedInputStream;

import org.apache.activemq.ActiveMQSession;
import org.apache.activemq.BlobMessage;
import org.apache.activemq.command.ActiveMQBlobMessage;
import org.apache.activemq.util.JMSExceptionSupport;
import org.apache.commons.logging.Log;
import org.apache.james.core.MimeMessageCopyOnWriteProxy;
import org.apache.james.core.MimeMessageInputStream;
import org.apache.james.core.MimeMessageInputStreamSource;
import org.apache.james.core.MimeMessageSource;
import org.apache.james.core.MimeMessageWrapper;
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
 * The implementation use {@link BlobMessage} or {@link ObjectMessage}, depending on the constructor which was used
 * 
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
    
    private boolean useBlob;
    
    
    /**
     * Construct a {@link ActiveMQMailQueue} which only use {@link BlobMessage}
     * @throws NotCompliantMBeanException 
     * 
     * @see #ActiveMQMailQueue(ConnectionFactory, String, boolean, Log)
     */
    public ActiveMQMailQueue(final ConnectionFactory connectionFactory, final String queuename, final Log logger) {
        this(connectionFactory, queuename, true, logger);
    }
    
    /**
     * Construct a new ActiveMQ based {@link MailQueue}.
     * 
     * 
     * 
     * 
     * @param connectionFactory
     * @param queuename
     * @param useBlob
     * @param logger
     * @throws NotCompliantMBeanException 
     */
    public ActiveMQMailQueue(final ConnectionFactory connectionFactory, final String queuename, boolean useBlob, final Log logger) {
        super(connectionFactory, queuename, logger);
        this.useBlob = useBlob;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.queue.jms.JMSMailQueue#populateMailMimeMessage(javax
     * .jms.Message, org.apache.mailet.Mail)
     */
    protected void populateMailMimeMessage(Message message, Mail mail) throws MessagingException, JMSException {
        if (message instanceof BlobMessage) {
            try {
                BlobMessage blobMessage = (BlobMessage) message;
                try {
                    // store URL and queuename for later usage
                    mail.setAttribute(JAMES_BLOB_URL, blobMessage.getURL());
                    mail.setAttribute(JAMES_QUEUE_NAME, queuename);
                } catch (MalformedURLException e) {
                    // Ignore on error
                    logger.debug("Unable to get url from blobmessage for mail " + mail.getName());
                }
                InputStream in = blobMessage.getInputStream();
                MimeMessageSource source;
 
                // if its a SharedInputStream we can make use of some more performant implementation which don't need to copy the message to a temporary file
                if (in instanceof SharedInputStream) {
                    String sourceId = message.getJMSMessageID();
                    long size = message.getLongProperty(JAMES_MAIL_MESSAGE_SIZE);
                    source = new MimeMessageBlobMessageSource((SharedInputStream) in, size, sourceId);
                } else {
                    source = new MimeMessageInputStreamSource(mail.getName(), in);
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
            // check if we should use a blob message here
            if (useBlob) { 
                MimeMessage mm = mail.getMessage();
                MimeMessage wrapper = mm;
                
                ActiveMQSession amqSession = getAMQSession(session);
                
                if (wrapper instanceof MimeMessageCopyOnWriteProxy) {
                    wrapper = ((MimeMessageCopyOnWriteProxy)mm).getWrappedMessage();
                }
                BlobMessage blobMessage = null;
                if (wrapper instanceof MimeMessageWrapper) {
                    URL blobUrl = (URL) mail.getAttribute(JAMES_BLOB_URL);
                    String fromQueue = (String) mail.getAttribute(JAMES_QUEUE_NAME);
                    MimeMessageWrapper mwrapper = (MimeMessageWrapper) wrapper;

                    if (blobUrl != null && fromQueue != null && mwrapper.isModified() == false ) {
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
            } else {
                super.produceMail(session, props, msgPrio, mail);
            }
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
    protected MailQueueItem createMailQueueItem(Connection connection, Session session, MessageConsumer consumer, Message message) throws JMSException, MessagingException {
        Mail mail = createMail(message);
        return new ActiveMQMailQueueItem(mail, connection, session, consumer, message, logger);
    }

    
    @Override
    protected List<Message> removeWithSelector(String selector) throws MailQueueException{
        List<Message> mList = super.removeWithSelector(selector);
        
        // Handle the blob messages
        for (int i = 0; i < mList.size(); i++) {
            Message m = mList.get(i);
            if (m instanceof ActiveMQBlobMessage) {
                try {
                    // Should get remove once this issue is closed:
                    // https://issues.apache.org/activemq/browse/AMQ-3018
                    ((ActiveMQBlobMessage) m).deleteFile();
                } catch (Exception e) {
                    logger.error("Unable to delete blob file for message " +m, e);
                }
            }
        }
        return mList;
    }

    
    @Override
    protected Message copy(Session session, Message m) throws JMSException {
        if (m instanceof ActiveMQBlobMessage) {
            ActiveMQBlobMessage b = (ActiveMQBlobMessage)m;
            ActiveMQBlobMessage copy = (ActiveMQBlobMessage) getAMQSession(session).createBlobMessage(b.getURL());
            try {
                copy.setProperties(b.getProperties());
            } catch (IOException e) {
                throw JMSExceptionSupport.create("Unable to copy message " + m, e);
            }
            return copy;
        } else {
            return super.copy(session, m);
        }
    }

    /**
     * Try to use ActiveMQ StatisticsPlugin to get size and if that fails fallback to {@link JMSMailQueue#getSize()}
     * 
     */
    @Override
    public long getSize() throws MailQueueException {
        
        Connection connection = null;
        Session session = null;
        MessageConsumer consumer = null;
        MessageProducer producer = null;
        int size = -1;
        
        try {
            connection = connectionFactory.createConnection();
            connection.start();

            session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
            Queue replyTo = session.createTemporaryQueue();
            consumer = session.createConsumer(replyTo);

            Queue myQueue = session.createQueue(queuename);;
            producer = session.createProducer(null);

            String queueName = "ActiveMQ.Statistics.Destination." + myQueue.getQueueName();
            Queue query = session.createQueue(queueName);

            Message msg = session.createMessage();

            producer.send(myQueue, msg);
            msg.setJMSReplyTo(replyTo);
            producer.send(query, msg);
            MapMessage reply = (MapMessage) consumer.receive();
            if (reply.itemExists("size")) {
                try {
                    // Maybe a bug in activemq as reply.getInt(..) did not work
                    // need to check activemq source code to understand why ..
                    size = Integer.parseInt(reply.getObject("size").toString());
                    return size;
                } catch (NumberFormatException e) {
                    // if we hit this we can't calculate the size so just catch it
                }
            }
              
        } catch (Exception e) {
            try {
                session.rollback();
            } catch (JMSException e1) {
                // ignore on rollback
            }
            throw new MailQueueException("Unable to remove mails" , e);

        } finally {
            if (consumer != null) {

                try {
                    consumer.close();
                } catch (JMSException e1) {
                    // ignore on rollback
                }
            }
            if (producer != null) {

                try {
                    producer.close();
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
        
        // if we came to this point we should just fallback to super method
        return super.getSize();
    }
    
    
}
