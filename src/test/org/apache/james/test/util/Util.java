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
import org.apache.james.smtpserver.core.AuthCmdHandler;
import org.apache.james.smtpserver.core.DataCmdHandler;
import org.apache.james.smtpserver.core.EhloCmdHandler;
import org.apache.james.smtpserver.core.ExpnCmdHandler;
import org.apache.james.smtpserver.core.HeloCmdHandler;
import org.apache.james.smtpserver.core.HelpCmdHandler;
import org.apache.james.smtpserver.core.MailCmdHandler;
import org.apache.james.smtpserver.core.QuitCmdHandler;
import org.apache.james.smtpserver.core.RcptCmdHandler;
import org.apache.james.smtpserver.core.RsetCmdHandler;
import org.apache.james.smtpserver.core.SendMailHandler;
import org.apache.james.smtpserver.core.VrfyCmdHandler;
import org.apache.james.test.mock.javaxmail.MockMimeMessage;
import org.apache.james.test.mock.mailet.MockMail;
import org.apache.mailet.MailAddress;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.ParseException;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;

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

    public static MockMail createMockMail2Recipients(MimeMessage m) throws ParseException {
        MockMail mockedMail = new MockMail();
        mockedMail.setMessage(m);
        mockedMail.setRecipients(Arrays.asList(new MailAddress[] {
                new MailAddress("test@james.apache.org"),
                new MailAddress("test2@james.apache.org") }));
        return mockedMail;
    }

    public static MockMimeMessage createMimeMessage() throws MessagingException {
        return createMimeMessage(null, null);
    }
    
    public static MockMimeMessage createMimeMessageWithSubject(String subject) throws MessagingException {
        return createMimeMessage(null, null, subject, 0);
    }
    
    public static MockMimeMessage createMimeMessage(String subject, int number) throws MessagingException {
        return createMimeMessage(null, null, subject, number);
    }
    
    public static MockMimeMessage createMimeMessage(String headerName, String headerValue) throws MessagingException {
        return createMimeMessage(headerName, headerValue, "testmail", 0);
    }
    
    public static MockMimeMessage createMimeMessage(String headerName, String headerValue, String subject, int number) throws MessagingException {
        String sender = "test@james.apache.org";
        String rcpt = "test2@james.apache.org";

        MockMimeMessage mockedMimeMessage = new MockMimeMessage(number);
        mockedMimeMessage.setFrom(new InternetAddress(sender));
        mockedMimeMessage.setRecipients(MimeMessage.RecipientType.TO, rcpt);
        if (headerName != null) mockedMimeMessage.setHeader(headerName, headerValue);
        if (subject != null) mockedMimeMessage.setSubject(subject);
        mockedMimeMessage.setText("testtext");
        mockedMimeMessage.saveChanges();
        return mockedMimeMessage;
    }
}
