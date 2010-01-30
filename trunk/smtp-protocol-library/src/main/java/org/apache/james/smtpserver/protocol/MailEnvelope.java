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


package org.apache.james.smtpserver.protocol;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.mailet.MailAddress;

/**
 * The MailEnvelope of a SMTP-Transaction
 * 
 *
 */
public interface MailEnvelope {

    /**
     * Return the size of the message. If the message is "empty" it will return -1
     * 
     * @return size
     */
	public int getSize();

	/**
	 * Return the recipients which where supplied in the RCPT TO: command
	 * 
	 * @return recipients
	 */
	public List<MailAddress> getRecipients();

	/**
	 * Return the sender of the mail which was supplied int the MAIL FROM: command. If its a "null" sender,
	 * null will get returned
	 * 
	 * @return sender
	 */
	public MailAddress getSender();


	/**
	 * Return the OutputStream of the message
	 * 
	 * @return out
	 * @throws Exception
	 */
	public OutputStream getMessageOutputStream() throws Exception;

	/**
	 * Return the InputStream of the message
	 * 
	 * @return in
	 * @throws Exception
	 */
	public InputStream getMessageInputStream() throws Exception;
}
