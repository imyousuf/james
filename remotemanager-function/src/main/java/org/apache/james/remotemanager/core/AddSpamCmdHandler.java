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

package org.apache.james.remotemanager.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Resource;

import org.apache.james.management.BayesianAnalyzerManagementException;
import org.apache.james.management.BayesianAnalyzerManagementService;
import org.apache.james.remotemanager.CommandHandler;
import org.apache.james.remotemanager.CommandHelp;
import org.apache.james.remotemanager.RemoteManagerResponse;
import org.apache.james.remotemanager.RemoteManagerSession;

/**
 * Handler called upon receipt of a ADDSPAM command
 *
 */
public class AddSpamCmdHandler implements CommandHandler{
    private final static String COMMAND_NAME = "ADDSPAM";
    private CommandHelp help = new CommandHelp("addspam dir/mbox [directory/mbox]","feed the BayesianAnalysisFeeder with the content of the directory or mbox file as SPAM");
    
    private BayesianAnalyzerManagementService bayesianAnalyzerManagement;

    /**
     * Set the BayesianAnalyzerManagementService
     * 
     * @param bayesianAnalyzerManagement the BayesianAnalyzerManagementService
     */
    @Resource(name="bayesiananalyzermanagement")
    public final void setBayesianAnalyzerManagement(BayesianAnalyzerManagementService bayesianAnalyzerManagement) {
        this.bayesianAnalyzerManagement = bayesianAnalyzerManagement;
    }
    
    /**
     * @see org.apache.james.remotemanager.CommandHandler#getHelp()
     */
    public CommandHelp getHelp() {
        return help;
    }

    /**
     * @see org.apache.james.remotemanager.CommandHandler#onCommand(org.apache.james.remotemanager.RemoteManagerSession, java.lang.String, java.lang.String)
     */
    public RemoteManagerResponse onCommand(RemoteManagerSession session, String command, String parameters) {
        RemoteManagerResponse response;
        String [] args = null;
        int count = 0;
        
        if (parameters != null) {
            args = parameters.split(" "); 
        }
        // check if the command was called correct
        if (parameters == null || parameters.trim().equals("") || (args != null && args.length != 2)) {
            response = new RemoteManagerResponse("Usage: " + getHelp().getSyntax());
            return response;
        }

        try {
            
            // stop watchdog cause feeding can take some time
            session.getWatchdog().stop();
            
            if (args[0].equalsIgnoreCase("DIR")) {
                count = bayesianAnalyzerManagement.addSpamFromDir(args[1]);
            } else if (args[0].equalsIgnoreCase("MBOX")) {
                count = bayesianAnalyzerManagement.addSpamFromMbox(args[1]);
            } else {
                response = new RemoteManagerResponse("Usage: " + getHelp().getSyntax());
                return response;
            }
            response = new RemoteManagerResponse("Feed the BayesianAnalysis with " + count + " SPAM");
            
        } catch (BayesianAnalyzerManagementException e) {
            session.getLogger().error("Error on feeding BayesianAnalysis: " + e);
            response = new RemoteManagerResponse("Error on feeding BayesianAnalysis: " + e);
        } finally {
            session.getWatchdog().start();
        }
    
        return response;
    }


    /**
     * @see org.apache.james.socket.CommonCommandHandler#getImplCommands()
     */
    public Collection<String> getImplCommands() {
        List<String> commands = new ArrayList<String>();
        commands.add(COMMAND_NAME);
        return commands;
    }

}
