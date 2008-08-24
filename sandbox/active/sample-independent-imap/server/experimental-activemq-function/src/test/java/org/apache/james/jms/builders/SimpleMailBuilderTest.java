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

package org.apache.james.jms.builders;

import java.util.Collection;

import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.apache.james.jms.builder.SimpleMailBuilder;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

public class SimpleMailBuilderTest extends MockObjectTestCase {

	private static final String KEY = "A Key";
	
	private static final String BODY =	
		"Hello Joe, could we change that to 4:00pm tomorrow?\r\n" +
		"\r\n";
	
	private static final String MAIL = 
		"Date: Mon, 7 Feb 1994 21:52:25 -0800 (PST)\r\n" +
		"From: Fred Foobar <foobar@Blurdybloop.COM>\r\n" +
		"Subject: afternoon meeting 2\r\n" +
		"To: mooch@owatagu.siam.edu\r\n" +
		"Message-Id: <B27397-0100000@Blurdybloop.COM>\r\n" +
		"MIME-Version: 1.0\r\n" +
		"Content-Type: TEXT/PLAIN; CHARSET=US-ASCII\r\n" +
		"\r\n" + BODY;
	
	SimpleMailBuilder builder;
	Mock generator;
	
	protected void setUp() throws Exception {
		super.setUp();
		generator = mock(SimpleMailBuilder.IdGenerator.class);
		builder = new SimpleMailBuilder((SimpleMailBuilder.IdGenerator) generator.proxy());
		generator.expects(once()).method("getId").will(returnValue(KEY));
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testBuildSimpleMail() throws Exception {
		Mail mail = builder.build(MAIL);
		assertNotNull("Do not return null", mail);
		assertEquals(KEY, mail.getName());
		MailAddress sender = mail.getSender();
		assertNotNull("Use 'from' header", sender);
		assertEquals("foobar", sender.getUser());
		assertEquals("Blurdybloop.COM", sender.getHost());
		Collection recipients = mail.getRecipients();
		assertNotNull(recipients);
		assertEquals("Use 'to' header", 1, recipients.size());
		MimeMessage message = mail.getMessage();
		assertEquals(BODY, IOUtils.toString(message.getInputStream(), "US-ASCII"));
	}
}
