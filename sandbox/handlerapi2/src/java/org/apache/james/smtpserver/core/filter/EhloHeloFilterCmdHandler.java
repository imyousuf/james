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



package org.apache.james.smtpserver.core.filter;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.smtpserver.Chain;
import org.apache.james.smtpserver.CommandHandler;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.util.mail.dsn.DSNStatus;

/**
  * Handles EHLO command
  */
public class EhloHeloFilterCmdHandler extends AbstractLogEnabled implements CommandHandler {


    /**
     * processes EHLO command
     *
     * @see org.apache.james.smtpserver.CommandHandler#onCommand(SMTPSession)
     **/
    public void onCommand(SMTPSession session, Chain chain) {
        String response = doEHLO(session);
        
        if (response == null) {
            // call the next handler in chain
            chain.doChain(session);
            
        } else {        
            // store the response
            session.getSMTPResponse().setRawSMTPResponse(response);
        }
    }

    /**
     * Check EHLO is present
     * 
     * @param session SMTP session object
     * @return responseString The response which should be passed to the client
     */
    private String doEHLO(SMTPSession session) {
        String argument = session.getCommandArgument();
        
        // reset the state
        session.resetState();
        
        if (argument == null) {
            String responseString = "501 "+DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.DELIVERY_INVALID_ARG)+" Domain address required: " + session.getCommandName();
            return responseString;
        } else {
            // store provided name
            session.getState().put(SMTPSession.CURRENT_HELO_NAME,argument);
        }
        return null;
    }
    
    /**
     * @see org.apache.james.smtpserver.CommandHandler#getImplCommands()
     */
    public Collection getImplCommands() {
        Collection implCommands = new ArrayList();
        implCommands.add("EHLO");
        implCommands.add("HELO");
        
        return implCommands;
    }

}
