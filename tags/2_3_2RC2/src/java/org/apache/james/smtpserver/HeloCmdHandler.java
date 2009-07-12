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

package org.apache.james.smtpserver;


import java.net.UnknownHostException;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;


/**
  * Handles HELO command
  */
public class HeloCmdHandler extends AbstractLogEnabled implements CommandHandler,Configurable {

    /**
     * The name of the command handled by the command handler
     */
    private final static String COMMAND_NAME = "HELO";

    /**
     * set checkValidHelo to false as default value
     */
    private boolean checkResolvableHelo = false;
    
    private boolean checkAuthNetworks = false;
    
    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration handlerConfiguration) throws ConfigurationException {
        Configuration configuration = handlerConfiguration.getChild("checkResolvableHelo",false);
        if(configuration != null) {
            checkResolvableHelo = configuration.getValueAsBoolean();
        }
        
        Configuration configRelay = handlerConfiguration.getChild("checkAuthNetworks",false);
        if(configRelay != null) {
            checkAuthNetworks = configRelay.getValueAsBoolean();
        }
        
    }
       
    /*
     * process HELO command
     *
     * @see org.apache.james.smtpserver.CommandHandler#onCommand(SMTPSession)
    **/
    public void onCommand(SMTPSession session) {
        doHELO(session, session.getCommandArgument());
    }

    /**
     * Handler method called upon receipt of a HELO command.
     * Responds with a greeting and informs the client whether
     * client authentication is required.
     *
     * @param session SMTP session object
     * @param argument the argument passed in with the command by the SMTP client
     */
    private void doHELO(SMTPSession session, String argument) {
        String responseString = null;
        boolean badHelo = false;
                
        
        // check for resolvable helo if its set in config
        if (checkResolvableHelo) {
            
            /**
             * don't check if the ip address is allowed to relay. Only check if it is set in the config. ed.
             */
            if (!session.isRelayingAllowed() || checkAuthNetworks) {

                // try to resolv the provided helo. If it can not resolved do not accept it.
                try {
                    org.apache.james.dnsserver.DNSServer.getByName(argument);
                } catch (UnknownHostException e) {
                    badHelo = true;
                    responseString = "501 Provided HELO " + argument + " can not resolved";
                    session.writeResponse(responseString);
                    getLogger().info(responseString);
                } 

            }
        }
        
        if (argument == null) {
            responseString = "501 Domain address required: " + COMMAND_NAME;
            session.writeResponse(responseString);
            getLogger().info(responseString);
        } else if (!badHelo) {
            session.resetState();
            session.getState().put(SMTPSession.CURRENT_HELO_MODE, COMMAND_NAME);
            session.getResponseBuffer().append("250 ")
                          .append(session.getConfigurationData().getHelloName())
                          .append(" Hello ")
                          .append(argument)
                          .append(" (")
                          .append(session.getRemoteHost())
                          .append(" [")
                          .append(session.getRemoteIPAddress())
                          .append("])");
            responseString = session.clearResponseBuffer();
            session.writeResponse(responseString);
        }
    }
}
