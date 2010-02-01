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



package org.apache.james.pop3server.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.mail.MessagingException;

import org.apache.james.pop3server.POP3Response;
import org.apache.james.pop3server.POP3Session;
import org.apache.james.protocols.api.CommandHandler;
import org.apache.james.protocols.api.Request;
import org.apache.james.protocols.api.Response;
import org.apache.mailet.Mail;


/**
  * Handles RSET command
  */
public class RsetCmdHandler implements CommandHandler<POP3Session> {
	private final static String COMMAND_NAME = "RSET";

	/**
     * Handler method called upon receipt of a RSET command.
     * Calls stat() to reset the mailbox.
     *
	 */
    public Response onCommand(POP3Session session, Request request) {
        POP3Response response = null;
        if (session.getHandlerState() == POP3Session.TRANSACTION) {
            stat(session);
            response = new POP3Response(POP3Response.OK_RESPONSE);
        } else {
            response = new POP3Response(POP3Response.ERR_RESPONSE);
        }
        return response;    
    }

   

    /**
     * Implements a "stat".  If the handler is currently in
     * a transaction state, this amounts to a rollback of the
     * mailbox contents to the beginning of the transaction.
     * This method is also called when first entering the
     * transaction state to initialize the handler copies of the
     * user inbox.
     *
     */
    @SuppressWarnings("unchecked")
    protected void stat(POP3Session session) {
        ArrayList<Mail> userMailbox = new ArrayList<Mail>();
        Mail dm = (Mail) session.getState().get(POP3Session.DELETED);

        userMailbox.add(dm);
        try {
            for (Iterator it = session.getUserInbox().list(); it.hasNext(); ) {
                String key = (String) it.next();
                Mail mc = session.getUserInbox().retrieve(key);
                // Retrieve can return null if the mail is no longer in the store.
                // In this case we simply continue to the next key
                if (mc == null) {
                    continue;
                }
                userMailbox.add(mc);
            }
        } catch(MessagingException e) {
            // In the event of an exception being thrown there may or may not be anything in userMailbox
            session.getLogger().error("Unable to STAT mail box ", e);
        }
        finally {
            session.setUserMailbox(userMailbox);
            session.setBackupUserMailbox((List<Mail>) userMailbox.clone());
        }
    }

    /**
     * @see org.apache.james.api.protocol.CommonCommandHandler#getImplCommands()
     */
    public Collection<String> getImplCommands() {
        List<String> commands = new ArrayList<String>();
        commands.add(COMMAND_NAME);
        return commands;
    }
    
}
