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
import org.apache.james.pop3server.mina.AsyncPOP3Server;
import org.apache.james.util.ConfigurationAdapter;

public class AsyncPOP3ServerTest extends POP3ServerTest {

    private AsyncPOP3Server m_pop3Server;

    protected void setUp() throws Exception {
        setUpServiceManager();

        m_pop3Server = new AsyncPOP3Server();
        m_pop3Server.setDNSService(dnsservice);
        m_pop3Server.setFileSystem(fSystem);
        m_pop3Server.setLoader(serviceManager);
        SimpleLog log = new SimpleLog("Mock");
        log.setLevel(SimpleLog.LOG_LEVEL_DEBUG);
        m_pop3Server.setLog(log);
        m_pop3Server.setMailServer(m_mailServer);
        m_testConfiguration = new POP3TestConfiguration(m_pop3ListenerPort);
    }

    protected void finishSetUp(POP3TestConfiguration testConfiguration) throws Exception {
        testConfiguration.init();
        m_pop3Server.configure(new ConfigurationAdapter(testConfiguration));
        m_pop3Server.init();
    }

    public void testNotAsciiCharsInPassword() throws Exception {
        // TODO: This currently fails with Async implementation because
        //       it use Charset US-ASCII to decode / Encode the protocol
        //       from the RFC I'm currently not understand if NON-ASCII chars
        //       are allowed at all. So this needs to be checked
    }
}
