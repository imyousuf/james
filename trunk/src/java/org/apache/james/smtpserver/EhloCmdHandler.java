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

package org.apache.james.smtpserver;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.util.mail.dsn.DSNStatus;

import java.net.UnknownHostException;
import java.util.ArrayList;

/**
  * Handles EHLO command
  */
public class EhloCmdHandler extends AbstractLogEnabled implements CommandHandler,Configurable {

    /**
     * The name of the command handled by the command handler
     */
    private final static String COMMAND_NAME = "EHLO";

    /**
     * set checkValidHelo to false as default value
     */
    private boolean checkValidEhlo = false;
    
    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration handlerConfiguration) throws ConfigurationException {
        Configuration configuration = handlerConfiguration.getChild("checkValidEhlo",false);
        if(configuration != null) {
           checkValidEhlo = configuration.getValueAsBoolean();
        }
    }

    /*
     * processes EHLO command
     *
     * @see org.apache.james.smtpserver.CommandHandler#onCommand(SMTPSession)
    **/
    public void onCommand(SMTPSession session) {
        doEHLO(session, session.getCommandArgument());
    }

    /**
     * Handler method called upon receipt of a EHLO command.
     * Responds with a greeting and informs the client whether
     * client authentication is required.
     *
     * @param session SMTP session object
     * @param argument the argument passed in with the command by the SMTP client
     */
    private void doEHLO(SMTPSession session, String argument) {
        String responseString = null;
        StringBuffer responseBuffer = session.getResponseBuffer();
        boolean badEhlo = false;
        
        // check for helo if its set in config
        if (checkValidEhlo == true) {
             
            // try to resolv the provided helo. If it can not resolved do not accept it.
            try {
                org.apache.james.dnsserver.DNSServer.getByName(argument);
            } catch (UnknownHostException e) {
                badEhlo = true;
                responseString = "501 "+DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.DELIVERY_INVALID_ARG)+" Provided EHLO " + argument + " can not resolved";
                session.writeResponse(responseString);
                getLogger().info(responseString);
            }
        }
        
        if (argument == null) {
            responseString = "501 "+DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.DELIVERY_INVALID_ARG)+" Domain address required: " + COMMAND_NAME;
            session.writeResponse(responseString);
        } else if (badEhlo == false){
            session.resetState();
            session.getState().put(SMTPSession.CURRENT_HELO_MODE, COMMAND_NAME);

            ArrayList esmtpextensions = new ArrayList();

            esmtpextensions.add(new StringBuffer(session.getConfigurationData().getHelloName())
                .append(" Hello ")
                .append(argument)
                .append(" (")
                .append(session.getRemoteHost())
                .append(" [")
                .append(session.getRemoteIPAddress())
                .append("])").toString());

            // Extension defined in RFC 1870
            long maxMessageSize = session.getConfigurationData().getMaxMessageSize();
            if (maxMessageSize > 0) {
                esmtpextensions.add("SIZE " + maxMessageSize);
            }

            if (session.isAuthRequired()) {
                esmtpextensions.add("AUTH LOGIN PLAIN");
                esmtpextensions.add("AUTH=LOGIN PLAIN");
            }

            esmtpextensions.add("PIPELINING");
            esmtpextensions.add("ENHANCEDSTATUSCODES");
            // see http://issues.apache.org/jira/browse/JAMES-419 
            //esmtpextensions.add("8BITMIME");


            // Iterator i = esmtpextensions.iterator();
            for (int i = 0; i < esmtpextensions.size(); i++) {
                if (i == esmtpextensions.size() - 1) {
                    responseBuffer.append("250 ");
                    responseBuffer.append((String) esmtpextensions.get(i));
                    session.writeResponse(session.clearResponseBuffer());
                } else {
                    responseBuffer.append("250-");
                    responseBuffer.append((String) esmtpextensions.get(i));
                    session.writeResponse(session.clearResponseBuffer());
                }
            }
        }
    }

}
