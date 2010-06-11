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
import javax.jms.TextMessage;
import javax.mail.MessagingException;

import org.apache.james.api.jms.MailBuilder;
import org.apache.james.api.jms.MailConsumer;
import org.apache.james.jms.MailMessageListener;
import org.apache.mailet.Mail;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;


public class MailMessageListenerTest extends MockObjectTestCase {

	private static final String MAIL = 
			"Date: Mon, 7 Feb 1994 21:52:25 -0800 (PST)\r\n" +
			"From: Fred Foobar <foobar@Blurdybloop.COM>\r\n" +
			"Subject: afternoon meeting 2\r\n" +
			"To: mooch@owatagu.siam.edu\r\n" +
			"Message-Id: <B27397-0100000@Blurdybloop.COM>\r\n" +
			"MIME-Version: 1.0\r\n" +
			"Content-Type: TEXT/PLAIN; CHARSET=US-ASCII\r\n" +
			"\r\n" +
			"Hello Joe, could we change that to 4:00pm tomorrow?\r\n" +
			"\r\n";
	
	Mock builder;
	TextMessage message;
	Mock mockMessage;
	Mock consumer;
	MailMessageListener listener;
	
	protected void setUp() throws Exception {
		super.setUp();
		builder = mock(MailBuilder.class);
		mockMessage = mock(TextMessage.class);
		message = (TextMessage) mockMessage.proxy();
		consumer = mock(MailConsumer.class);
		listener = new MailMessageListener((MailConsumer) consumer.proxy(), (MailBuilder) builder.proxy());
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testShouldReportJMSException() throws Exception {
		Exception e = new JMSException("Test");
		consumer.expects(once()).method("reportIssue").with(same(e), same(message));
		mockMessage.expects(once()).method("getText").will(throwException(e));
		listener.onMessage(message);
	}
	
	public void testShouldReportMessagingException() throws Exception {
		Exception e = new MessagingException("Test");
		consumer.expects(once()).method("reportIssue").with(same(e), same(message));
		mockMessage.expects(once()).method("getText").will(returnValue("Text"));
		builder.expects(once()).method("build").will(throwException(e));
		listener.onMessage(message);
	}
	
	public void testShouldConvertTextMessageToMimeMessage() throws Exception {
		Mail mail = (Mail) mock(Mail.class).proxy();
		consumer.expects(once()).method("consume").with(same(mail));
		mockMessage.expects(once()).method("getText").will(returnValue(MAIL));
		builder.expects(once()).method("build").with(eq(MAIL)).will(returnValue(mail));
		listener.onMessage(message);
	}
}
