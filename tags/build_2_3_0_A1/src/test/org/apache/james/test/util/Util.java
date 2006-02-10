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
package org.apache.james.test.util;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.james.smtpserver.*;

public class Util {

    public static int getRandomNonPrivilegedPort() {
        return ((int)( Math.random() * 3000) + 8000);
    }

    public static Configuration getValuedConfiguration(String name, String value) {
        DefaultConfiguration defaultConfiguration = new DefaultConfiguration(name);
        defaultConfiguration.setValue(value);
        return defaultConfiguration;
    }

    public static DefaultConfiguration createRemoteManagerHandlerChainConfiguration() {
        DefaultConfiguration handlerChainConfig = new DefaultConfiguration("handlerchain");
        handlerChainConfig.addChild(createCommandHandlerConfiguration("HELO", HeloCmdHandler.class));
        handlerChainConfig.addChild(createCommandHandlerConfiguration("EHLO", EhloCmdHandler.class));
        handlerChainConfig.addChild(createCommandHandlerConfiguration("AUTH", AuthCmdHandler.class));
        handlerChainConfig.addChild(createCommandHandlerConfiguration("VRFY", VrfyCmdHandler.class));
        handlerChainConfig.addChild(createCommandHandlerConfiguration("EXPN", ExpnCmdHandler.class));
        handlerChainConfig.addChild(createCommandHandlerConfiguration("MAIL", MailCmdHandler.class));
        handlerChainConfig.addChild(createCommandHandlerConfiguration("RCPT", RcptCmdHandler.class));
        handlerChainConfig.addChild(createCommandHandlerConfiguration("DATA", DataCmdHandler.class));
        handlerChainConfig.addChild(createCommandHandlerConfiguration("RSET", RsetCmdHandler.class));
        handlerChainConfig.addChild(createCommandHandlerConfiguration("HELP", HelpCmdHandler.class));
        handlerChainConfig.addChild(createCommandHandlerConfiguration("QUIT", QuitCmdHandler.class));
        return handlerChainConfig;
    }

    private static DefaultConfiguration createCommandHandlerConfiguration(String command, Class commandClass) {
        DefaultConfiguration cmdHandlerConfig = new DefaultConfiguration("handler");
        cmdHandlerConfig.setAttribute("command", command);
        String classname = commandClass.getName();
        cmdHandlerConfig.setAttribute("class", classname);
        return cmdHandlerConfig;
    }
}
