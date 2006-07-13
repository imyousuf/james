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
 * This class represent the base filter command handlers which are shipped with james.
 */
public class CoreFilterCmdHandler implements CommandsHandler {

    private final Object DATABASEFILTERCMDHANDLER = "org.apache.james.smtpserver.basefilter.DataBaseFilterCmdHandler";
    private final Object EHLOBASEFILTERCMDHANDLER = "org.apache.james.smtpserver.basefilter.EhloBaseFilterCmdHandler";
    private final Object HELOBASEFILTERCMDHANDLER = "org.apache.james.smtpserver.basefilter.HeloBaseFilterCmdHandler";
    private final Object MAILBASEFILTERCMDHANDLER = "org.apache.james.smtpserver.basefilter.MailBaseFilterCmdHandler";
    private final Object RCPTBASEFILTERCMDHANDLER = "org.apache.james.smtpserver.basefilter.RcptBaseFilterCmdHandler";
   
    /**
     * @see org.apache.james.smtpserver.CommandsHandler#getCommands()
     */
    public Map getCommands() {
        Map commands = new HashMap();
        
        // Insert the basecommands in the Map
        commands.put("DATA", DATABASEFILTERCMDHANDLER);
        commands.put("EHLO", EHLOBASEFILTERCMDHANDLER);
        commands.put("HELO", HELOBASEFILTERCMDHANDLER);
        commands.put("MAIL", MAILBASEFILTERCMDHANDLER);
        commands.put("RCPT", RCPTBASEFILTERCMDHANDLER);
        
        return commands;
    }
}
