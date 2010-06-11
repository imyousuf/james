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


package org.apache.james.transport.matchers;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.ParseException;

import junit.framework.TestCase;

import org.apache.james.Constants;
import org.apache.james.services.AbstractDNSServer;
import org.apache.james.services.DNSServer;
import org.apache.james.test.mock.avalon.MockServiceManager;
import org.apache.james.test.mock.mailet.MockMailContext;
import org.apache.james.test.mock.mailet.MockMatcherConfig;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

public abstract class AbstractRemoteAddrInNetworkTest extends TestCase {

    protected Mail mockedMail;

    protected AbstractNetworkMatcher matcher;

    private String remoteAddr;

    private DNSServer dnsServer;

    private MockServiceManager m_serviceManager;

    public AbstractRemoteAddrInNetworkTest(String arg0)
            throws UnsupportedEncodingException {
        super(arg0);
    }

    protected void setRemoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    protected void setupMockedMail() {
        mockedMail = new Mail() {

            private static final long serialVersionUID = 1L;

            public String getName() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void setName(String newName) {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public MimeMessage getMessage() throws MessagingException {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public Collection getRecipients() {
                ArrayList r = new ArrayList();
                try {
                    r = new ArrayList(Arrays
                            .asList(new MailAddress[] { new MailAddress(
                                    "test@james.apache.org") }));
                } catch (ParseException e) {
                }
                return r;
            }

            public void setRecipients(Collection recipients) {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public MailAddress getSender() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String getState() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String getRemoteHost() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String getRemoteAddr() {
                return remoteAddr;
            }

            public String getErrorMessage() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void setErrorMessage(String msg) {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void setMessage(MimeMessage message) {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void setState(String state) {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public Serializable getAttribute(String name) {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public Iterator getAttributeNames() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public boolean hasAttributes() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public Serializable removeAttribute(String name) {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void removeAllAttributes() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public Serializable setAttribute(String name, Serializable object) {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public long getMessageSize() throws MessagingException {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public Date getLastUpdated() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void setLastUpdated(Date lastUpdated) {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void dispose() {

                throw new UnsupportedOperationException(
                "Unimplemented mock service");
                
            }

            public void setRemoteAddr(String hostAddress) {

                throw new UnsupportedOperationException(
                "Unimplemented mock service");
                
            }

            public void setRemoteHost(String hostName) {

                throw new UnsupportedOperationException(
                "Unimplemented mock service");
                
            }

            public void setSender(MailAddress reversePath) {

                throw new UnsupportedOperationException(
                "Unimplemented mock service");
            }

        };

    }

    protected void setupDNSServer() {
        dnsServer = new AbstractDNSServer() {
            public InetAddress getByName(String host)
                    throws UnknownHostException {
                if ("192.168.200.0".equals(host) || "255.255.255.0".equals(host) || "192.168.200.1".equals(host) || "192.168.0.1".equals(host) || "192.168.1.1".equals(host)) {
                    // called with an IP it only check formal validity
                    return InetAddress.getByName(host);
                }
                throw new UnsupportedOperationException("getByName("+host+") unimplemented in AbstractRemoteAddrInNetworkTest");
            }

        };
    }

    protected void setupMatcher() throws MessagingException {
        m_serviceManager = new MockServiceManager();
        m_serviceManager.put(DNSServer.ROLE, dnsServer);

        MockMailContext mmc = new MockMailContext();
        mmc.setAttribute(Constants.AVALON_COMPONENT_MANAGER, m_serviceManager);
        matcher = createMatcher();
        MockMatcherConfig mci = new MockMatcherConfig(getConfigOption()
                + getAllowedNetworks(), mmc);
        matcher.init(mci);
    }

    protected void setupAll() throws MessagingException {
        setupDNSServer();
        setupMockedMail();
        setupMatcher();
    }

    protected abstract String getConfigOption();

    protected abstract String getAllowedNetworks();

    protected abstract AbstractNetworkMatcher createMatcher();
}
