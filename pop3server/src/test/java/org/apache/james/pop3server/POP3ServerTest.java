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

package org.apache.james.pop3server;

import org.apache.commons.logging.impl.SimpleLog;
import org.apache.james.pop3server.netty.POP3Server;

public class POP3ServerTest extends AbstractAsyncPOP3ServerTest{

    private POP3Server m_pop3Server;

    @Override
    protected void initPOP3Server(POP3TestConfiguration testConfiguration) throws Exception {
        m_pop3Server.configure(testConfiguration);
        m_pop3Server.init();
    }

    @Override
    protected void setUpPOP3Server() throws Exception {
        
        m_pop3Server = new POP3Server();
        m_pop3Server.setDNSService(dnsservice);
        m_pop3Server.setFileSystem(fSystem);
        m_pop3Server.setProtocolHandlerChain(chain);
       
        
        SimpleLog log = new SimpleLog("Mock");
        log.setLevel(SimpleLog.LOG_LEVEL_DEBUG);
        m_pop3Server.setLog(log);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        m_pop3Server.destroy();

    }

}
