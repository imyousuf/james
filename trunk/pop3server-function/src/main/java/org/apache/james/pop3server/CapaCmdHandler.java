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


package org.apache.james.pop3server;

import java.util.ArrayList;
import java.util.List;

public class CapaCmdHandler implements CommandHandler{
	public final static String COMMAND_NAME = "CAPA";
	private List<CapaCapability> caps = new ArrayList<CapaCapability>();
    
	/**
     * @see org.apache.james.pop3server.CommandHandler#getCommands()
     */
	public List<String> getCommands() {
		List<String> commands = new ArrayList<String>();
		commands.add(COMMAND_NAME);
		return commands;
	}

	/**
	 * @see org.apache.james.pop3server.CommandHandler#onCommand(org.apache.james.pop3server.POP3Session)
	 */
	public void onCommand(POP3Session session) {
		session.writeResponse(POP3Handler.OK_RESPONSE+ " Capability list follows");	
		
		for (int i = 0; i < caps.size(); i++) {
			List<String> cList = caps.get(i).getImplementedCapabilities(session);
			for (int a = 0; a < cList.size(); a++) {
				session.writeResponse(cList.get(a));
			}
		}
		session.writeResponse(".");
	}
	
	/**
	 * Wire the handler
	 * 
	 * @param capHandler
	 */
	public void wireHandler(CapaCapability capHandler) {
		caps.add(capHandler);
	}

}
