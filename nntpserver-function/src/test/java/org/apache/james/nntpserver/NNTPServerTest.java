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

package org.apache.james.nntpserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import junit.framework.TestCase;

import org.apache.avalon.cornerstone.services.sockets.SocketManager;
import org.apache.avalon.cornerstone.services.threads.ThreadManager;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.commons.logging.impl.SimpleLog;
import org.apache.commons.net.nntp.NNTPClient;
import org.apache.james.api.dnsservice.AbstractDNSServer;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.api.kernel.mock.FakeLoader;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.nntpserver.mock.MockNNTPRepository;
import org.apache.james.nntpserver.repository.NNTPRepository;
import org.apache.james.nntpserver.repository.NNTPRepositoryImpl;
import org.apache.james.services.FileSystem;
import org.apache.james.services.MailServer;
import org.apache.james.socket.AvalonProtocolServer;
import org.apache.james.socket.JamesConnectionManager;
import org.apache.james.socket.SimpleConnectionManager;
import org.apache.james.test.mock.avalon.MockSocketManager;
import org.apache.james.test.mock.avalon.MockThreadManager;
import org.apache.james.test.mock.james.MockFileSystem;
import org.apache.james.test.mock.james.MockMailServer;
import org.apache.james.test.util.Util;
import org.apache.james.userrepository.MockUsersRepository;

public class NNTPServerTest extends TestCase {
	protected int m_nntpListenerPort = Util.getNonPrivilegedPort();

	private NNTPServerProtocolHandlerFactory m_nntpServer;
	private AvalonProtocolServer protoserver;
	protected FakeLoader serviceManager;

	private MockUsersRepository m_usersRepository;
	protected NNTPTestConfiguration m_testConfiguration;
	private NNTPRepositoryImpl m_nntpRepos;
	
	protected MockMailServer m_mailServer;
	protected NNTPClient m_nntpProtocol;

    private SimpleConnectionManager connectionManager;

    private MockSocketManager socketManager;

    private MockThreadManager threadManager;

    private MockNNTPRepository nntpRepos;

    private DNSService dnsService;

    protected void setUp() throws Exception {
        setUpServiceManager();

        m_nntpServer = new NNTPServerProtocolHandlerFactory();
        m_nntpServer.setLog(new SimpleLog("MockLog"));
        m_nntpServer.setDNSService(dnsService);
        m_nntpServer.setUserRepository(m_usersRepository);
        m_nntpServer.setMailServer(m_mailServer);
        m_nntpServer.setNNTPRepository(nntpRepos);


        //ContainerUtil.service(m_nntpServer, serviceManager);

        //ContainerUtil.enableLogging(m_nntpServer, new MockLogger());
        protoserver = new AvalonProtocolServer();
        protoserver.setConnectionManager(connectionManager);
        protoserver.setFileSystem(new MockFileSystem());
        protoserver.setProtocolHandlerFactory(m_nntpServer);
        protoserver.setSocketManager(socketManager);
        protoserver.setThreadManager(threadManager);
        protoserver.setDNSService(dnsService);
        protoserver.setLog(new SimpleLog("MockLog"));

        m_testConfiguration = new NNTPTestConfiguration(m_nntpListenerPort);
    }

    protected void finishSetUp(NNTPTestConfiguration testConfiguration)
			throws Exception {
		testConfiguration.init();
		m_nntpServer.configure(testConfiguration);
		m_nntpServer.init();
		
		protoserver.configure(testConfiguration);
		protoserver.init();
	}

    protected void setUpServiceManager() throws Exception {
		serviceManager = new FakeLoader();
		m_usersRepository = new MockUsersRepository();
		m_mailServer = new MockMailServer(m_usersRepository);
		m_nntpRepos = new NNTPRepositoryImpl();
		FileSystem fs = new FileSystem() {
            private File base = new File(System.getProperty("java.io.tmpdir"));
            public File getBasedir() throws FileNotFoundException {
                return base;
            }

            public File getFile(String fileURL) throws FileNotFoundException {
                return null;
            }

            public InputStream getResource(String url) throws IOException {
                return null;
            }
            
        };
        serviceManager.put(FileSystem.ROLE, fs);
        
		m_nntpRepos.setFileSystem(fs);



		serviceManager.put(MailServer.ROLE, m_mailServer);
		serviceManager.put(UsersRepository.ROLE, m_usersRepository);
		
		socketManager = new MockSocketManager(m_nntpListenerPort);
		serviceManager.put(SocketManager.ROLE,socketManager);
		
		threadManager = new MockThreadManager();
		serviceManager.put(ThreadManager.ROLE, threadManager);
	      
		connectionManager = new SimpleConnectionManager();
	    connectionManager.setThreadManager(threadManager);
	    connectionManager.setLog(new SimpleLog("CM"));
	    connectionManager.configure(new DefaultConfigurationBuilder());
	                
	    serviceManager.put(JamesConnectionManager.ROLE, connectionManager);
	        
		nntpRepos = new MockNNTPRepository();
		serviceManager.put(NNTPRepository.ROLE, nntpRepos);
		
		dnsService = setUpDNSServer();
		serviceManager.put(DNSService.ROLE, dnsService);
	}

	private DNSService setUpDNSServer() {
		DNSService dns = new AbstractDNSServer() {
			public String getHostName(InetAddress addr) {
				return "localhost";
			}

			public InetAddress getLocalHost() throws UnknownHostException {
				return InetAddress.getLocalHost();
			}

		};
		return dns;
	}

	protected void tearDown() throws Exception {
		if (m_nntpProtocol != null) {
			m_nntpProtocol.sendCommand("quit");
			m_nntpProtocol.disconnect();
		}		
		ContainerUtil.dispose(m_mailServer);

		super.tearDown();
	}

	public void testLoginAuthRequired() throws Exception {
		m_testConfiguration.setUseAuthRequired();
		finishSetUp(m_testConfiguration);
		m_nntpProtocol = new NNTPClient();
		m_usersRepository.addUser("valid", "user");

		m_nntpProtocol.connect("127.0.0.1", m_nntpListenerPort);
		assertFalse("Reject invalid user", m_nntpProtocol.authenticate(
				"invalid", "user"));

		assertTrue("Login successful", m_nntpProtocol.authenticate("valid",
				"user"));
	}

}
