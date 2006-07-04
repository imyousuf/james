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

package org.apache.james.smtpserver.basefilter;

import java.util.ArrayList;
import java.util.List;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.smtpserver.CommandHandler;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.util.mail.dsn.DSNStatus;


/**
  * handles DATA command
 */
public class DataBaseFilterCmdHandler
    extends AbstractLogEnabled
    implements CommandHandler {
   
    /**
     * process DATA command
     *
     * @see org.apache.james.smtpserver.CommandHandler#onCommand(SMTPSession)
     */
    public void onCommand(SMTPSession session) {
        doDATA(session, session.getCommandArgument());
    }


    /**
     * @param session SMTP session object
     * @param argument the argument passed in with the command by the SMTP client
     */
    private void doDATA(SMTPSession session, String argument) {
        String responseString = null;
        if ((argument != null) && (argument.length() > 0)) {
            responseString = "500 "+DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.DELIVERY_INVALID_ARG)+" Unexpected argument provided with DATA command";
            session.writeResponse(responseString);
            
            //TODO: Check if this should been!
            // After this filter match we should not call any other handler!
            //session.getState().put(SMTPSession.STOP_HANDLER_PROCESSING, "true");
        }
        if (!session.getState().containsKey(SMTPSession.SENDER)) {
            responseString = "503 "+DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.DELIVERY_OTHER)+" No sender specified";
            session.writeResponse(responseString);
            
            // After this filter match we should not call any other handler!
            session.getState().put(SMTPSession.STOP_HANDLER_PROCESSING, "true");
            
        } else if (!session.getState().containsKey(SMTPSession.RCPT_LIST)) {
            responseString = "503 "+DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.DELIVERY_OTHER)+" No recipients specified";
            session.writeResponse(responseString);
            
            // After this filter match we should not call any other handler!
            session.getState().put(SMTPSession.STOP_HANDLER_PROCESSING, "true");
        }
    }
    
    /**
     * @see org.apache.james.smtpserver.CommandHandler#getImplCommands()
     */
    public List getImplCommands() {
        ArrayList implCommands = new ArrayList();
        implCommands.add("DATA");
        
        return implCommands;
    }
}
