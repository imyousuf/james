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
import org.apache.james.smtpserver.core.esmtp.AuthCmdHandler;
import org.apache.james.smtpserver.core.esmtp.EhloCmdHandler;
import org.apache.james.smtpserver.core.esmtp.MailSizeEsmtpExtension;

import java.util.LinkedList;
import java.util.List;

/**
 * This class represent the base command handlers which are shipped with james.
 */
public class CoreCmdHandlerLoader implements HandlersPackage {

    private final String COMMANDDISPATCHER = SMTPCommandDispatcherLineHandler.class.getName();
    private final String AUTHCMDHANDLER = AuthCmdHandler.class.getName();
    private final String DATACMDHANDLER = DataCmdHandler.class.getName();
    private final String EHLOCMDHANDLER = EhloCmdHandler.class.getName();
    private final String EXPNCMDHANDLER = ExpnCmdHandler.class.getName();
    private final String HELOCMDHANDLER = HeloCmdHandler.class.getName();
    private final String HELPCMDHANDLER = HelpCmdHandler.class.getName();
    private final String MAILCMDHANDLER = MailCmdHandler.class.getName();
    private final String NOOPCMDHANDLER = NoopCmdHandler.class.getName();
    private final String QUITCMDHANDLER = QuitCmdHandler.class.getName();
    private final String RCPTCMDHANDLER = RcptCmdHandler.class.getName();
    private final String RSETCMDHANDLER = RsetCmdHandler.class.getName();
    private final String VRFYCMDHANDLER = VrfyCmdHandler.class.getName();
    private final String MAILSIZEHOOK = MailSizeEsmtpExtension.class.getName();
    private final String WELCOMEMESSAGEHANDLER = WelcomeMessageHandler.class.getName();
    private final String USERSREPOSITORYAUTHHANDLER = UsersRepositoryAuthHook.class.getName();
    private final String POSTMASTERABUSEHOOK = PostmasterAbuseRcptHook.class.getName();
    private final String AUTHREQUIREDTORELAY = AuthRequiredToRelayRcptHook.class.getName();
    private final String SENDERAUTHIDENTITYVERIFICATION = SenderAuthIdentifyVerificationRcptHook.class.getName();
    private final String DATALINEMESSAGEHOOKHANDLER = DataLineMessageHookHandler.class.getName();
   
    /**
     * @see org.apache.james.smtpserver.HandlersPackage#getHandlers()
     */
    public List<String> getHandlers() {
        List<String> commands = new LinkedList<String>();
        
        // Insert the base commands in the Map
        commands.add(WELCOMEMESSAGEHANDLER);
        commands.add(COMMANDDISPATCHER);
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
        commands.add(MAILSIZEHOOK);
        commands.add(USERSREPOSITORYAUTHHANDLER);
        commands.add(AUTHREQUIREDTORELAY);
        commands.add(SENDERAUTHIDENTITYVERIFICATION);
        commands.add(POSTMASTERABUSEHOOK);
        commands.add(DATALINEMESSAGEHOOKHANDLER);
        
        return commands;
    }
}
