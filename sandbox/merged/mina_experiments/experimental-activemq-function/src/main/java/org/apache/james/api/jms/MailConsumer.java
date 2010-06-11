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

package org.apache.james.api.jms;

import org.apache.mailet.Mail;

/**
 * Consumes mail messages.
 */
public interface MailConsumer {
	
	/**
	 * Consumes the given mail message.
	 * @param message <code>Mail</code>, not null
	 */
	public void consume(Mail mail);
	
	/**
	 * Reports an exception.
	 * 
	 * @param e Exception, not null
	 * @param message being processed
	 */
	public void reportIssue(Exception e, Object message);
}
