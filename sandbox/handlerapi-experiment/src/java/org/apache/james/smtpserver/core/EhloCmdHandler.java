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
import java.util.List;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.smtpserver.CommandHandler;
import org.apache.james.smtpserver.ExtensibleHandler;
import org.apache.james.smtpserver.SMTPResponse;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.util.mail.SMTPRetCode;

/**
 * Handles EHLO command
 */
public class EhloCmdHandler extends AbstractLogEnabled implements
        CommandHandler, ExtensibleHandler {

    /**
     * The name of the command handled by the command handler
     */
    private final static String COMMAND_NAME = "EHLO";
    private List ehloExtensions;

    /**
     * processes EHLO command
     *
     * @see org.apache.james.smtpserver.CommandHandler#onCommand(org.apache.james.smtpserver.SMTPSession, java.lang.String, java.lang.String) 
     **/
    public SMTPResponse onCommand(SMTPSession session, String command, String arguments) {
        return doEHLO(session, arguments);
    }

    /**
     * Handler method called upon receipt of a EHLO command.
     * Responds with a greeting and informs the client whether
     * client authentication is required.
     *
     * @param session SMTP session object
     * @param argument the argument passed in with the command by the SMTP client
     */
    private SMTPResponse doEHLO(SMTPSession session, String argument) {
        SMTPResponse resp = new SMTPResponse();
        resp.setRetCode(SMTPRetCode.MAIL_OK);
        
        session.getConnectionState().put(SMTPSession.CURRENT_HELO_MODE, COMMAND_NAME);

        resp.appendLine(new StringBuffer(session.getConfigurationData()
                .getHelloName()).append(" Hello ").append(argument)
                .append(" (").append(session.getRemoteHost()).append(" [")
                .append(session.getRemoteIPAddress()).append("])"));

        // Extension defined in RFC 1870
        long maxMessageSize = session.getConfigurationData()
                .getMaxMessageSize();
        if (maxMessageSize > 0) {
            resp.appendLine("SIZE " + maxMessageSize);
        }

        processExtensions(session, resp);
        
        resp.appendLine("PIPELINING");
        resp.appendLine("ENHANCEDSTATUSCODES");
        // see http://issues.apache.org/jira/browse/JAMES-419 
        resp.appendLine("8BITMIME");
        return resp;

    }
    
    /**
     * @see org.apache.james.smtpserver.CommandHandler#getImplCommands()
     */
    public Collection getImplCommands() {
        Collection implCommands = new ArrayList();
        implCommands.add(COMMAND_NAME);
        
        return implCommands;
    }

    /**
     * @see org.apache.james.smtpserver.ExtensibleHandler#getMarkerInterface()
     */
    public Class getMarkerInterface() {
        return EhloExtension.class;
    }


    /**
     * @see org.apache.james.smtpserver.ExtensibleHandler#wireExtensions(java.util.List)
     */
    public void wireExtensions(List extension) {
        this.ehloExtensions = extension;
    }

    /**
     * @param session
     */
    private void processExtensions(SMTPSession session, SMTPResponse resp) {
        if (ehloExtensions != null) {
            int count = ehloExtensions.size();
            for(int i =0; i < count; i++) {
                List lines = ((EhloExtension)ehloExtensions.get(i)).getImplementedEsmtpFeatures(session);
                if (lines != null) {
                    for (int j = 0; j < lines.size(); j++) {
                        resp.appendLine((String) lines.get(j));
                    }
                }
            }
        }
    }


}
