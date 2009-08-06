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

package org.apache.james.jms.consumer;

import org.apache.james.services.MailServer;
import org.apache.mailet.Mail;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

public class SpoolToJamesMailConsumerTest extends MockObjectTestCase {

	SpoolToJamesMailConsumer consumer;
	Mock mailServer;
	Mail mail;
	
	protected void setUp() throws Exception {
		super.setUp();
		mail = (Mail)(mock(Mail.class).proxy());
		mailServer = mock(MailServer.class);
		consumer = new SpoolToJamesMailConsumer((MailServer)(mailServer.proxy()),new MockLog());
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testShouldSendMailToJames() throws Exception {
		mailServer.expects(once()).method("sendMail").with(same(mail));
		consumer.consume(mail);
	}
}
