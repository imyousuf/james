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



package org.apache.james.smtpserver.core;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.smtpserver.CommandHandler;
import org.apache.james.smtpserver.SMTPResponse;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.util.mail.SMTPRetCode;
import org.apache.james.util.mail.dsn.DSNStatus;
import org.apache.mailet.MailAddress;

/**
  * Handles RCPT command
  */
public class RcptCmdHandler extends AbstractLogEnabled implements
        CommandHandler {

    /**
     * handles RCPT command
     *
     * @see org.apache.james.smtpserver.CommandHandler#onCommand(org.apache.james.smtpserver.SMTPSession, java.lang.String, java.lang.String) 
    **/
    public SMTPResponse onCommand(SMTPSession session, String command, String parameters) {
        return doRCPT(session, parameters);
    }


    /**
     * Handler method called upon receipt of a RCPT command.
     * Reads recipient.  Does some connection validation.
     *
     *
     * @param session SMTP session object
     * @param argument the argument passed in with the command by the SMTP client
     */
    private SMTPResponse doRCPT(SMTPSession session, String argument) {
        Collection rcptColl = (Collection) session.getState().get(
                SMTPSession.RCPT_LIST);
        if (rcptColl == null) {
            rcptColl = new ArrayList();
        }
        MailAddress recipientAddress = (MailAddress) session.getState().get(
                SMTPSession.CURRENT_RECIPIENT);
        rcptColl.add(recipientAddress);
        session.getState().put(SMTPSession.RCPT_LIST, rcptColl);
        StringBuffer response = new StringBuffer();
        response.append(DSNStatus.getStatus(DSNStatus.SUCCESS,DSNStatus.ADDRESS_VALID))
                .append(" Recipient <")
                .append(recipientAddress)
                .append("> OK");
        return new SMTPResponse(SMTPRetCode.MAIL_OK, response);
             
    }
    
    /**
     * @see org.apache.james.smtpserver.CommandHandler#getImplCommands()
     */
    public Collection getImplCommands() {
        Collection implCommands = new ArrayList();
        implCommands.add("RCPT");
        
        return implCommands;
    }

}
