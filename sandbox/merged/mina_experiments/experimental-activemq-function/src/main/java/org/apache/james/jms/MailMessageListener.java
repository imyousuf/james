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

package org.apache.james.jms;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.mail.MessagingException;

import org.apache.james.api.jms.MailBuilder;
import org.apache.james.api.jms.MailConsumer;
import org.apache.mailet.Mail;

/**
 * <p>Listeners for mail.
 * Supported message content is built into a {@link Mail} 
 * by the {@link MailBuilder} stategy. The <code>Mail</code>
 * is then passed to the {@link MailConsumer} for further
 * processing.
 * </p><p>
 * Responsible for extracting content from known message
 * types.
 * </p>
 */
public class MailMessageListener implements MessageListener {

	private final MailConsumer consumer;
	private final MailBuilder builder;
	
	public MailMessageListener(final MailConsumer consumer, final MailBuilder builder) {
		this.consumer = consumer;
		this.builder = builder;
	}
	
	/**
	 * Processes a message.
	 * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
	 */
	public void onMessage(final Message message) {
		try {
			if (message instanceof TextMessage) {
				final TextMessage textMessage = (TextMessage)message;
				final String text = textMessage.getText();
				final Mail mail = builder.build(text);
				consumer.consume(mail);
			} 
		} catch (JMSException e) {
			consumer.reportIssue(e, message);
		} catch (MessagingException e) {
			consumer.reportIssue(e, message);
		}
	}
}
