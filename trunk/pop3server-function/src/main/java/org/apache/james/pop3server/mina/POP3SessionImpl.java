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

package org.apache.james.pop3server.mina;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.commons.logging.Log;
import org.apache.james.pop3server.POP3HandlerConfigurationData;
import org.apache.james.pop3server.POP3Session;
import org.apache.james.services.MailRepository;
import org.apache.james.socket.mina.AbstractMINASession;
import org.apache.mailet.Mail;
import org.apache.mina.core.session.IoSession;

public class POP3SessionImpl extends AbstractMINASession implements POP3Session{

    public final static String POP3SESSION = "POP3SESSION";

	private POP3HandlerConfigurationData configData;

	private Map<String, Object> state = new HashMap<String, Object>();

	private List<Mail> userMailbox;

	private MailRepository userInbox;

	private int handlerState;

	private List<Mail> backupUserMailbox;
	
	public POP3SessionImpl(POP3HandlerConfigurationData configData, Log logger, IoSession session, SSLContext context) {
		super(logger, session, context);
		
		this.configData = configData;
	}

	public POP3SessionImpl(POP3HandlerConfigurationData configData, Log logger, IoSession session) {
		this(configData, logger, session, null);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.apache.james.pop3server.POP3Session#getBackupUserMailbox()
	 */
	public List<Mail> getBackupUserMailbox() {
		return backupUserMailbox;
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.james.pop3server.POP3Session#getConfigurationData()
	 */
	public POP3HandlerConfigurationData getConfigurationData() {
		return configData;
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.james.pop3server.POP3Session#getHandlerState()
	 */
	public int getHandlerState() {
		return handlerState;
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.james.api.protocol.TLSSupportedSession#getState()
	 */
	public Map<String, Object> getState() {
		return state;
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.james.pop3server.POP3Session#getUserInbox()
	 */
	public MailRepository getUserInbox() {
		return userInbox;
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.james.pop3server.POP3Session#getUserMailbox()
	 */
	public List<Mail> getUserMailbox() {
		return userMailbox;
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.james.pop3server.POP3Session#setBackupUserMailbox(java.util.List)
	 */
	public void setBackupUserMailbox(List<Mail> backupUserMailbox) {
		this.backupUserMailbox = backupUserMailbox;
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.james.pop3server.POP3Session#setHandlerState(int)
	 */
	public void setHandlerState(int handlerState) {
		this.handlerState = handlerState;
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.james.pop3server.POP3Session#setUserInbox(org.apache.james.services.MailRepository)
	 */
	public void setUserInbox(MailRepository userInbox) {
		this.userInbox = userInbox;
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.james.pop3server.POP3Session#setUserMailbox(java.util.List)
	 */
	public void setUserMailbox(List<Mail> userMailbox) {
		this.userMailbox = userMailbox;
	}


	/*
	 * (non-Javadoc)
	 * @see org.apache.james.api.protocol.TLSSupportedSession#resetState()
	 */
	public void resetState() {
		state.clear();
		
		setHandlerState(AUTHENTICATION_READY);
	}

}
