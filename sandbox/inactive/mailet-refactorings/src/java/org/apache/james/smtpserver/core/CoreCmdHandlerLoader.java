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

import java.util.HashMap;
import java.util.Map;

import org.apache.james.smtpserver.CommandsHandler;

/**
 * This class represent the base command handlers which are shipped with james.
 */
public class CoreCmdHandlerLoader implements CommandsHandler {

    private final Object AUTHCMDHANDLER = AuthCmdHandler.class.getName();
    private final Object DATACMDHANDLER = DataCmdHandler.class.getName();
    private final Object EHLOCMDHANDLER = EhloCmdHandler.class.getName();
    private final Object EXPNCMDHANDLER = ExpnCmdHandler.class.getName();
    private final Object HELOCMDHANDLER = HeloCmdHandler.class.getName();
    private final Object HELPCMDHANDLER = HelpCmdHandler.class.getName();
    private final Object MAILCMDHANDLER = MailCmdHandler.class.getName();
    private final Object NOOPCMDHANDLER = NoopCmdHandler.class.getName();
    private final Object QUITCMDHANDLER = QuitCmdHandler.class.getName();
    private final Object RCPTCMDHANDLER = RcptCmdHandler.class.getName();
    private final Object RSETCMDHANDLER = RsetCmdHandler.class.getName();
    private final Object VRFYCMDHANDLER = VrfyCmdHandler.class.getName();
   
    /**
     * @see org.apache.james.smtpserver.CommandsHandler#getCommands()
     */
    public Map getCommands() {
        Map commands = new HashMap();
        
        // Insert the basecommands in the Map
        commands.put("AUTH", AUTHCMDHANDLER);
        commands.put("DATA", DATACMDHANDLER);
        commands.put("EHLO", EHLOCMDHANDLER);
        commands.put("EXPN", EXPNCMDHANDLER);
        commands.put("HELO", HELOCMDHANDLER);
        commands.put("HELP", HELPCMDHANDLER);
        commands.put("MAIL", MAILCMDHANDLER);
        commands.put("NOOP", NOOPCMDHANDLER);
        commands.put("QUIT", QUITCMDHANDLER);
        commands.put("RCPT", RCPTCMDHANDLER);
        commands.put("RSET", RSETCMDHANDLER);
        commands.put("VRFY", VRFYCMDHANDLER);
        
        return commands;
    }
}
