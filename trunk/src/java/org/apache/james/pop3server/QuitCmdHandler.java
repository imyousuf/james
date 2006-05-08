/***********************************************************************
 * Copyright (c) 1999-2006 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.pop3server;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.commons.collections.ListUtils;

import java.util.List;

/**
  * Handles QUIT command
  */
public class QuitCmdHandler extends AbstractLogEnabled implements CommandHandler {

    /**
     * @see org.apache.james.pop3server.CommandHandler#onCommand(POP3Session)
     */
    public void onCommand(POP3Session session) {
        doQUIT(session,session.getCommandArgument());
    }

    /**
     * Handler method called upon receipt of a QUIT command.
     * This method handles cleanup of the POP3Handler state.
     *
     * @param command the command parsed by the parseCommand method
     * @param argument the first argument parsed by the parseCommand method
     */
    private void doQUIT(POP3Session session,String argument) {
        String responseString = null;
        if (session.getHandlerState() == POP3Handler.AUTHENTICATION_READY ||  session.getHandlerState() == POP3Handler.AUTHENTICATION_USERSET) {
            responseString = POP3Handler.OK_RESPONSE + " Apache James POP3 Server signing off.";
            session.writeResponse(responseString);
            return;
        }
        List toBeRemoved =  ListUtils.subtract(session.getBackupUserMailbox(), session.getUserMailbox());
        try {
            session.getUserInbox().remove(toBeRemoved);
            // for (Iterator it = toBeRemoved.iterator(); it.hasNext(); ) {
            //    Mail mc = (Mail) it.next();
            //    userInbox.remove(mc.getName());
            //}
            responseString = POP3Handler.OK_RESPONSE + " Apache James POP3 Server signing off.";
            session.writeResponse(responseString);
        } catch (Exception ex) {
            responseString = POP3Handler.ERR_RESPONSE + " Some deleted messages were not removed";
            session.writeResponse(responseString);
            getLogger().error("Some deleted messages were not removed: " + ex.getMessage());
        }
        session.endSession();
    }


}
