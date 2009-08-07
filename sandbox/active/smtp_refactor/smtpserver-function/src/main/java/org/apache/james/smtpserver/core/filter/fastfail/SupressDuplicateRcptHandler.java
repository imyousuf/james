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




package org.apache.james.smtpserver.core.filter.fastfail;

import java.util.Collection;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.dsn.DSNStatus;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.smtpserver.core.PostRcptListener;
import org.apache.mailet.MailAddress;

/**
 * 
 * This handler can be used to just ignore duplicated recipients. 
 */
public class SupressDuplicateRcptHandler extends AbstractLogEnabled implements PostRcptListener {

   
	/**
	 * (non-Javadoc)
	 * 
	 * @see org.apache.james.smtpserver.core.PostRcptListener#onRcpt(org.apache.james.smtpserver.SMTPSession,
	 *      org.apache.mailet.MailAddress)
	 */
	public String onRcpt(SMTPSession session, MailAddress rcpt) {
		Collection rcptList = (Collection) session.getState().get(
				SMTPSession.RCPT_LIST);

		// Check if the recipient is allready in the rcpt list
		if (rcptList != null && rcptList.contains(rcpt)) {
			StringBuffer responseBuffer = new StringBuffer();

			responseBuffer.append("250 " + DSNStatus.getStatus(DSNStatus.SUCCESS, DSNStatus.ADDRESS_VALID) + " Recipient <")
					.append(rcpt.toString()).append("> OK");

			getLogger().debug("Duplicate recipient not add to recipient list: "+ rcpt.toString());
			return responseBuffer.toString();
		}
		return null;
	}
}
