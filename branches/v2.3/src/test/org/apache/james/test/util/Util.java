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
package org.apache.james.test.util;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.james.smtpserver.*;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * some utilities for James unit testing
 */
public class Util {

    private static final int PORT_RANGE_START =  8000; // the lowest possible port number assigned for testing
    private static final int PORT_RANGE_END   = 11000; // the highest possible port number assigned for testing
    private static int PORT_LAST_USED = PORT_RANGE_START;

    /**
     * assigns a port from the range of test ports
     * @return port number
     */
    public static int getNonPrivilegedPort() {
        return getNextNonPrivilegedPort(); // uses sequential assignment of ports
    }

    /**
     * assigns a random port from the range of test ports
     * @return port number
     */
    protected static int getRandomNonPrivilegedPortInt() {
        return ((int)( Math.random() * (PORT_RANGE_END - PORT_RANGE_START) + PORT_RANGE_START));
    }

    /**
     * assigns ports sequentially from the range of test ports
     * @return port number
     */
    protected synchronized static int getNextNonPrivilegedPort() {
        // Hack to increase probability that the port is bindable
        while (true) {
            try {
        PORT_LAST_USED++;
        if (PORT_LAST_USED > PORT_RANGE_END) PORT_LAST_USED = PORT_RANGE_START;
                ServerSocket ss;
                ss = new ServerSocket(PORT_LAST_USED);
                ss.setReuseAddress(true);
                ss.close();
                break;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return PORT_LAST_USED;
    }

    public static Configuration getValuedConfiguration(String name, String value) {
        DefaultConfiguration defaultConfiguration = new DefaultConfiguration(name);
        defaultConfiguration.setValue(value);
        return defaultConfiguration;
    }

    public static DefaultConfiguration createRemoteManagerHandlerChainConfiguration() {
        DefaultConfiguration handlerChainConfig = new DefaultConfiguration("test");
        return handlerChainConfig;
    }
    public static DefaultConfiguration createSMTPHandlerChainConfiguration() {
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
        // mail sender
        handlerChainConfig.addChild(createCommandHandlerConfiguration(null, SendMailHandler.class));
        return handlerChainConfig;
    }

    private static DefaultConfiguration createCommandHandlerConfiguration(String command, Class commandClass) {
        DefaultConfiguration cmdHandlerConfig = new DefaultConfiguration("handler");
        if (command != null) {
            cmdHandlerConfig.setAttribute("command", command);
        }
        String classname = commandClass.getName();
        cmdHandlerConfig.setAttribute("class", classname);
        return cmdHandlerConfig;
    }

}
