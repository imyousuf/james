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

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Random;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.ParseException;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.james.test.mock.MockMimeMessage;
import org.apache.james.test.mock.mailet.MockMail;
import org.apache.mailet.MailAddress;

/**
 * some utilities for James unit testing
 */
public class Util {

    private static final int PORT_RANGE_START =  8000; // the lowest possible port number assigned for testing
    private static final int PORT_RANGE_END   = 11000; // the highest possible port number assigned for testing
    private static int PORT_LAST_USED = PORT_RANGE_START;
    private static final Random RANDOM = new Random();
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
        int nextPortCandidate = PORT_LAST_USED;
        while (true) {
            try {
                nextPortCandidate++;
                if (PORT_LAST_USED == nextPortCandidate) throw new RuntimeException("no free port found");
                if (nextPortCandidate > PORT_RANGE_END) nextPortCandidate = PORT_RANGE_START; // start over

                // test, port is available
                ServerSocket ss;
                ss = new ServerSocket(nextPortCandidate);
                ss.setReuseAddress(true);
                ss.close();
                break;
            } catch (IOException e) {
                e.printStackTrace();
                continue; // try next port
            }
        }
        PORT_LAST_USED = nextPortCandidate;
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

    public static MockMail createMockMail2Recipients(MimeMessage m) throws ParseException {
        MockMail mockedMail = new MockMail();
        mockedMail.setName("ID="+RANDOM.nextLong());
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
