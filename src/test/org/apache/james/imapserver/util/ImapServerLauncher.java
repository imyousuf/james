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

package org.apache.james.imapserver.util;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.mail.MessagingException;

import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.imapserver.ImapHandler;
import org.apache.james.imapserver.TestConstants;
import org.apache.james.imapserver.mock.MockImapHandlerConfigurationData;
import org.apache.james.imapserver.mock.MockWatchdog;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.services.DNSServer;
import org.apache.james.test.mock.avalon.MockLogger;

public class ImapServerLauncher  implements TestConstants 
{


    private DNSServer dnsServer;

    public void go() throws IOException, MessagingException, MailboxManagerException
    {
        ServerSocket ss = new ServerSocket(HOST_PORT);
        final MockImapHandlerConfigurationData theConfigData=new MockImapHandlerConfigurationData();
        while (true) {
            
            final Socket s=ss.accept();
            new Thread() {
                public void run() {
                    try {
                        ImapHandler imapHandler=new ImapHandler();
                        imapHandler.enableLogging(new MockLogger());
                        imapHandler.setConfigurationData(theConfigData);
                        imapHandler.setDnsServer(getDNSServer());
                        imapHandler.setStreamDumpDir("streamdump");
                        imapHandler.setWatchdog(new MockWatchdog());
                        System.out.println("Handle connection "+s);
                        imapHandler.handleConnection(s);
                        System.out.println("Handle connection finished."+s);
    
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }       
                }
            }.start();
            
            
        }

    }
    
    public DNSServer getDNSServer() throws Exception {
        dnsServer=new org.apache.james.dnsserver.DNSServer();
        ContainerUtil.enableLogging(dnsServer, new MockLogger());
        ContainerUtil.configure(dnsServer, new DefaultConfiguration("dnsserver"));
        ContainerUtil.initialize(dnsServer);
        return dnsServer; 
    }

    public static void main(String[] args)
    {
        try {
            new ImapServerLauncher().go();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
