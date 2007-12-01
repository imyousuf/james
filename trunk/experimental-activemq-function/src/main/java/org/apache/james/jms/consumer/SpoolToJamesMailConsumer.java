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

import javax.mail.MessagingException;

import org.apache.commons.logging.Log;
import org.apache.james.api.jms.MailConsumer;
import org.apache.james.services.MailServer;
import org.apache.mailet.Mail;

/**
 * Consumes a mail message by sending it to the main
 * JAMES spool for processing.
 */
public class SpoolToJamesMailConsumer implements MailConsumer {

	private final MailServer mailServer;
	private final Log log;
	
	public SpoolToJamesMailConsumer(final MailServer mailServer, final Log log) {
		super();
		this.mailServer = mailServer;
		this.log = log;
	}

	public void consume(Mail mail) {
		if (log.isDebugEnabled()) {
			log.debug("Consuming " + mail.getName());
		}
		log.trace(mail);
		
		try {
			mailServer.sendMail(mail);
		} catch (MessagingException e) {
			reportIssue(e, mail);
		}
	}

	public void reportIssue(Exception e, Object message) {
		log.warn(e.getMessage());
		if (log.isDebugEnabled()) {
			log.debug("Failed to process: " + message, e);
			log.trace(message);
		}
	}

	/**
	 * Renders suitably for logging.
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	public String toString()
	{
	    final String TAB = " ";
	    
	    final String retValue = "SpoolToJamesMailConsumer ( "
	        + "mailServer = " + this.mailServer + TAB
	        + " )";
	
	    return retValue;
	}
	
	
}
