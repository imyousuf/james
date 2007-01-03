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

import org.apache.james.smtpserver.HandlersPackage;

import java.util.LinkedList;
import java.util.List;

/**
 * This class represent the base command handlers which are shipped with james.
 */
public class CoreCmdHandlerLoader implements HandlersPackage {

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
    private final Object WELCOMEMESSAGEHANDLER = WelcomeMessageHandler.class.getName();
    private final Object ADDDEFAULTATTRIBUTESHANDLER = AddDefaultAttributesMessageHook.class.getName();
    private final Object SENDMAILHANDLER = SendMailHandler.class.getName();
    private final Object USERSREPOSITORYAUTHHANDLER = UsersRepositoryAuthHook.class.getName();
    private final Object POSTMASTERABUSEHOOK = PostmasterAbuseRcptHook.class.getName();
    private final Object AUTHREQUIREDTORELAY = AuthRequiredToRelayRcptHook.class.getName();
    private final Object SENDERAUTHIDENTITYVERIFICATION = SenderAuthIdentifyVerificationRcptHook.class.getName();
   
    /**
     * @see org.apache.james.smtpserver.HandlersPackage#getHandlers()
     */
    public List getHandlers() {
        List commands = new LinkedList();
        
        // Insert the basecommands in the Map
        commands.add(WELCOMEMESSAGEHANDLER);
        commands.add(ADDDEFAULTATTRIBUTESHANDLER);
        commands.add(SENDMAILHANDLER);
        commands.add(AUTHCMDHANDLER);
        commands.add(DATACMDHANDLER);
        commands.add(EHLOCMDHANDLER);
        commands.add(EXPNCMDHANDLER);
        commands.add(HELOCMDHANDLER);
        commands.add(HELPCMDHANDLER);
        commands.add(MAILCMDHANDLER);
        commands.add(NOOPCMDHANDLER);
        commands.add(QUITCMDHANDLER);
        commands.add(RCPTCMDHANDLER);
        commands.add(RSETCMDHANDLER);
        commands.add(VRFYCMDHANDLER);
        commands.add(USERSREPOSITORYAUTHHANDLER);
        commands.add(AUTHREQUIREDTORELAY);
        commands.add(SENDERAUTHIDENTITYVERIFICATION);
        commands.add(POSTMASTERABUSEHOOK);
        
        return commands;
    }
}
