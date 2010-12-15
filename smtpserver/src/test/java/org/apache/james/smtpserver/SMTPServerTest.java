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

import org.apache.commons.logging.impl.SimpleLog;
import org.apache.commons.net.smtp.SMTPClient;
import org.apache.james.protocols.impl.AbstractChannelPipelineFactory;
import org.apache.james.smtpserver.netty.SMTPServer;

public class SMTPServerTest extends AbstractSMTPServerTest{

    private SMTPServer m_smtpServer;

    @Override
    protected void initSMTPServer(SMTPTestConfiguration testConfiguration) throws Exception {
        m_smtpServer.configure(testConfiguration);      
        m_smtpServer.init();
      
    }

    @Override
    protected void setUpSMTPServer() throws Exception {
        SimpleLog log = new SimpleLog("SMTP");
        log.setLevel(SimpleLog.LOG_LEVEL_ALL);
        m_smtpServer = new SMTPServer();
        m_smtpServer.setDNSService(m_dnsServer);
        m_smtpServer.setFileSystem(fileSystem);      
        
        m_smtpServer.setProtocolHandlerChain(chain);
        
        m_smtpServer.setLog(log);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        m_smtpServer.destroy();

    }

    public void testMaxLineLength() throws Exception {
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol = new SMTPClient();
        smtpProtocol.connect("127.0.0.1", m_smtpListenerPort);
        
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < AbstractChannelPipelineFactory.MAX_LINE_LENGTH; i++) {
            sb.append("A");
        }
        smtpProtocol.sendCommand("EHLO " + sb.toString());
        System.out.println(smtpProtocol.getReplyString());
        assertEquals("Line length exceed", 500, smtpProtocol.getReplyCode());

        smtpProtocol.sendCommand("EHLO test");
        assertEquals("Line length ok", 250, smtpProtocol.getReplyCode());


        smtpProtocol.quit();
        smtpProtocol.disconnect();
    }
    
    public void testConnectionLimit() throws Exception {
        m_testConfiguration.setConnectionLimit(2);
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol = new SMTPClient();
        smtpProtocol.connect("127.0.0.1", m_smtpListenerPort);
        SMTPClient smtpProtocol2 = new SMTPClient();
        smtpProtocol2.connect("127.0.0.1", m_smtpListenerPort);
        
        SMTPClient smtpProtocol3 = new SMTPClient();

        try {
            smtpProtocol3.connect("127.0.0.1", m_smtpListenerPort);
            Thread.sleep(3000);
            fail("Shold disconnect connection 3");
        } catch (Exception e) {
            
        }
        
        smtpProtocol.quit();
        smtpProtocol.disconnect();
        smtpProtocol2.quit();
        smtpProtocol2.disconnect();
        
        smtpProtocol3.connect("127.0.0.1", m_smtpListenerPort);
        Thread.sleep(3000);

       
    }

}
