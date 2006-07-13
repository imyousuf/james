/***********************************************************************
 * Copyright (c) 2006 The Apache Software Foundation.                  *
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

package org.apache.james.smtpserver.core;

import java.util.HashMap;
import java.util.Map;

import org.apache.james.smtpserver.CommandsHandler;


/**
 * This class represent the base command handlers which are shipped with james.
 */
public class CoreCmdHandler implements CommandsHandler {

    private final Object AUTHCMDHANDLER = "org.apache.james.smtpserver.AuthCmdHandler";
    private final Object DATACMDHANDLER = "org.apache.james.smtpserver.DataCmdHandler";
    private final Object EHLOCMDHANDLER = "org.apache.james.smtpserver.EhloCmdHandler";
    private final Object EXPNCMDHANDLER = "org.apache.james.smtpserver.ExpnCmdHandler";
    private final Object HELOCMDHANDLER = "org.apache.james.smtpserver.HeloCmdHandler";
    private final Object HELPCMDHANDLER = "org.apache.james.smtpserver.HelpCmdHandler";
    private final Object MAILCMDHANDLER = "org.apache.james.smtpserver.MailCmdHandler";
    private final Object NOOPCMDHANDLER = "org.apache.james.smtpserver.NoopCmdHandler";
    private final Object QUITCMDHANDLER = "org.apache.james.smtpserver.QuitCmdHandler";
    private final Object RCPTCMDHANDLER = "org.apache.james.smtpserver.RcptCmdHandler";
    private final Object RSETCMDHANDLER = "org.apache.james.smtpserver.RsetCmdHandler";
    private final Object VRFYCMDHANDLER = "org.apache.james.smtpserver.VrfyCmdHandler";
   
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
