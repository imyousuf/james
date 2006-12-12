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

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.mailet.Mail;

import javax.mail.MessagingException;

import java.util.ArrayList;
import java.util.Iterator;


/**
  * Handles RSET command
  */
public class RsetCmdHandler extends AbstractLogEnabled implements CommandHandler {

    /**
     * @see org.apache.james.pop3server.CommandHandler#onCommand(POP3Session)
     */
    public void onCommand(POP3Session session) {
        doRSET(session,session.getCommandArgument());
    }

    /**
     * Handler method called upon receipt of a RSET command.
     * Calls stat() to reset the mailbox.
     *
     * @param argument the first argument parsed by the parseCommand method
     */
    private void doRSET(POP3Session session,String argument) {
        String responseString = null;
        if (session.getHandlerState() == POP3Handler.TRANSACTION) {
            stat(session, getLogger());
            responseString = POP3Handler.OK_RESPONSE;
        } else {
            responseString = POP3Handler.ERR_RESPONSE;
        }
        session.writeResponse(responseString);
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
    public static void stat(POP3Session session, Logger logger) {
        ArrayList userMailbox = new ArrayList();
        userMailbox.add(POP3Handler.DELETED);
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
            logger.error("Unable to STAT mail box ", e);
        }
        finally {
            session.setUserMailbox(userMailbox);
            session.setBackupUserMailbox((ArrayList) userMailbox.clone());
        }
    }

}
