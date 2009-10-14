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

package org.apache.james.jms.activemq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.commons.logging.Log;
import org.apache.james.api.jms.MailBuilder;
import org.apache.james.api.jms.MailConsumer;
import org.apache.james.jms.MailMessageListener;

/**
 * <p>Manages the connection to the local ActiveMQ broker.
 * </p><p>
 * <strong>Note:</strong> <code>BrokerManager</code> is intended
 * to allow concurrent access. {@link #start} and {@link stop}
 * are synchronized on the consumer register. 
 * </p>
 */
public class BrokerManager {

	private final BrokerService broker;
	private final Collection registrations;
	private final Log log;
	
	private ActiveMQConnectionFactory factory;
	private Connection connection;
	private boolean started;
	
	public BrokerManager(final BrokerService broker, final Log log) {
		this.broker = broker;
		this.registrations = new ArrayList();
		this.log = log;
	}
	
	public void start() throws JMSException {
		// Prevent concurrent start, stop, registration
		synchronized (registrations) {
			if (!started) {
				try {
					broker.start();
				} catch (Exception e) {
					throw new ActiveMQException(e);
				}
				factory = new ActiveMQConnectionFactory("vm://localhost");
				connection = factory.createConnection();
				
				for (final Iterator it=registrations.iterator();it.hasNext();) {
					final ConsumerRegistration registration = (ConsumerRegistration) it.next();
					try {
						registration.register(this);
						it.remove();
					} catch (JMSException e) {
						if (log.isErrorEnabled()) {
							log.error("Failed to add consumer to " + registration.destination 
									+ ": " + e.getMessage());
						}
						if (log.isDebugEnabled()) {
							log.debug("Failed to register " + registration, e);
						}
					}
				}
				
				started = true;
			}
		}
	}
	
	public void stop() throws JMSException {
		// Prevent concurrent start, stop, registration
		synchronized (registrations) {
			if (started) {
				connection.stop();
				try {
					broker.stop();
				} catch (Exception e) {
					throw new ActiveMQException(e);
				}
				started = false;
			}
		}
	}
	
	/**
	 * Sets the consumer as the listener for the queue with the given name.
	 * If the broker has not been started, the consumer will be registered
	 * and added on start.
	 * @param consumer <code>MailConsumer</code>, not null
	 * @param builder <code>MailBuilder</code>, not null
	 * @param destination name of the destination queue, not null
	 * @throws JMSException 
	 */
	public void consumeQueue(final MailConsumer consumer, final MailBuilder builder, 
			String destination) throws JMSException {
		if (started) {
			doListen(consumer, builder, destination);
		} else {
			register(consumer, builder, destination, false);
		}
	}

	private void doListen(final MailConsumer consumer, final MailBuilder builder, String destination) throws JMSException {
		final MailMessageListener listener = new MailMessageListener(consumer, builder);
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Queue queue = session.createQueue(destination);
		MessageConsumer messageConsumer = session.createConsumer(queue);
		messageConsumer.setMessageListener(listener);
		connection.start();
		if (log.isTraceEnabled()) {
			log.trace("Attached " + consumer + " to " + destination);
		}
	}
	
	/**
	 * Subscribes the consumer to the topic with the given name.
	 * If the broker has not been started, the consumer will be registered
	 * and subscribed on start.
	 * @param consumer <code>MailConsumer</code>, not null
	 * @param builder <code>MailBuilder</code>, not null
	 * @param destination name of the destination queue, not null
	 * @throws JMSException 
	 */
	public void subscribeToTopic(final MailConsumer consumer, final MailBuilder builder, 
			String destination) throws JMSException {
		if (started) {
			doSubscribe(consumer, builder, destination);
		} else {
			register(consumer, builder, destination, true);
		}
	}

	private void doSubscribe(final MailConsumer consumer, final MailBuilder builder, String destination) throws JMSException {
		final MailMessageListener listener = new MailMessageListener(consumer, builder);
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Topic topic = session.createTopic(destination);
		MessageConsumer messageConsumer = session.createConsumer(topic);
		messageConsumer.setMessageListener(listener);
		connection.start();
		if (log.isTraceEnabled()) {
			log.trace("Subscribed " + consumer + " to " + destination);
		}
	}

	private void register(final MailConsumer consumer, final MailBuilder builder, 
			final String destination, final boolean topic) throws JMSException {
		final ConsumerRegistration registration = new ConsumerRegistration(consumer, builder, destination, topic);
		// Prevent concurrent start, stop, registration
		synchronized (registrations) {
			// After gaining the lock for registrations, check whether start
			// or stop has now completed
			if (started) {
				// broker now started so add consumer now
				registration.register(this);
			} else {
				// broker isn't started so add to registrations
				if (log.isDebugEnabled()) {
					log.debug("Registered: " + registration);
				}
				registrations.add(registration);
			}
		}
	}
	
	/**
	 * Holds a pending registration.
	 */
	private static final class ConsumerRegistration {
		public final MailConsumer consumer;
		public final MailBuilder builder;
		public final String destination;
		public final boolean topic;
		
		public ConsumerRegistration(final MailConsumer consumer, final MailBuilder builder, final String destination, final boolean topic) {
			super();
			this.consumer = consumer;
			this.builder = builder;
			this.destination = destination;
			this.topic = topic;
		}
		
		public void register(final BrokerManager brokerManager) throws JMSException {
			if (topic) {
				brokerManager.doSubscribe(consumer, builder, destination);
			} else {
				brokerManager.doListen(consumer, builder, destination);
			}
		}

		/**
		 * Renders this object suitably for logging.
		 *
		 * @return a <code>String</code> representation 
		 * of this object.
		 */
		public String toString()
		{
		    final String TAB = "  ";
		    
		    String retValue = "ConsumerRegistration ( "
		        + super.toString() + TAB
		        + "consumer = " + this.consumer + TAB
		        + "builder = " + this.builder + TAB
		        + "destination = " + this.destination + TAB
		        + "topic = " + this.topic + TAB
		        + " )";
		
		    return retValue;
		}
	}
}
