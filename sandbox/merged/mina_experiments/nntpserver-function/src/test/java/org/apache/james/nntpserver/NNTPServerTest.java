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
import org.apache.avalon.framework.service.ServiceException;
import org.apache.commons.net.nntp.NNTPClient;
import org.apache.james.api.dnsservice.AbstractDNSServer;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.nntpserver.mock.MockNNTPRepository;
import org.apache.james.nntpserver.repository.NNTPRepository;
import org.apache.james.nntpserver.repository.NNTPRepositoryImpl;
import org.apache.james.services.FileSystem;
import org.apache.james.services.MailServer;
import org.apache.james.socket.JamesConnectionManager;
import org.apache.james.socket.SimpleConnectionManager;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.avalon.MockServiceManager;
import org.apache.james.test.mock.avalon.MockSocketManager;
import org.apache.james.test.mock.avalon.MockThreadManager;
import org.apache.james.test.mock.james.MockMailServer;
import org.apache.james.test.util.Util;
import org.apache.james.userrepository.MockUsersRepository;

public class NNTPServerTest extends TestCase {
	private int m_nntpListenerPort = Util.getNonPrivilegedPort();

	private NNTPServer m_nntpServer;
	private MockServiceManager serviceManager;

	private MockUsersRepository m_usersRepository;
	private NNTPTestConfiguration m_testConfiguration;
	private NNTPRepositoryImpl m_nntpRepos;
	
	private MockMailServer m_mailServer;
	private NNTPClient m_nntpProtocol;

	protected void setUp() throws Exception {
		m_nntpServer = new NNTPServer();
		setUpServiceManager();

		ContainerUtil.enableLogging(m_nntpServer, new MockLogger());
		ContainerUtil.service(m_nntpServer, serviceManager);

		m_testConfiguration = new NNTPTestConfiguration(m_nntpListenerPort);
	}

	private void finishSetUp(NNTPTestConfiguration testConfiguration)
			throws Exception {
		testConfiguration.init();
		ContainerUtil.configure(m_nntpServer, testConfiguration);
		m_nntpServer.initialize();
	}

	private void setUpServiceManager() throws ServiceException {
		serviceManager = new MockServiceManager();
		m_usersRepository = new MockUsersRepository();
		m_mailServer = new MockMailServer(m_usersRepository);
		m_nntpRepos = new NNTPRepositoryImpl();
		m_nntpRepos.setFileSystem(new FileSystem() {
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
			
		});

		SimpleConnectionManager connectionManager = new SimpleConnectionManager();
		ContainerUtil.enableLogging(connectionManager, new MockLogger());
		ContainerUtil.service(connectionManager, serviceManager);
		serviceManager.put(JamesConnectionManager.ROLE, connectionManager);

		serviceManager.put(MailServer.ROLE, m_mailServer);
		serviceManager.put(UsersRepository.ROLE, m_usersRepository);
		serviceManager.put(SocketManager.ROLE, new MockSocketManager(
				m_nntpListenerPort));
		serviceManager.put(ThreadManager.ROLE, new MockThreadManager());
		serviceManager.put(NNTPRepository.ROLE, new MockNNTPRepository());
		serviceManager.put(DNSService.ROLE, setUpDNSServer());
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
		m_nntpServer.dispose();
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
