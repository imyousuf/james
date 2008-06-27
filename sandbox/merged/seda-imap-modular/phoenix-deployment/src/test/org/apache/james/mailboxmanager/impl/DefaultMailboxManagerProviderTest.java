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


package org.apache.james.mailboxmanager.impl;

import java.io.IOException;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.james.mailboxmanager.manager.MailboxManagerFactory;
import org.apache.james.mailboxmanager.mock.MockMailboxManagerFactory;
import org.xml.sax.SAXException;

import junit.framework.TestCase;

public class DefaultMailboxManagerProviderTest extends TestCase {

    private DefaultMailboxManagerProvider mailboxManagerProvider;

    public void setUp() {
        mailboxManagerProvider = new DefaultMailboxManagerProvider();
    }

    public void testConfigure() throws ConfigurationException, SAXException,
            IOException {
        Configuration confFile = new DefaultConfigurationBuilder()
                .build(getClass()
                        .getResourceAsStream(
                                "/org/apache/james/mailboxmanager/testdata/DefaultMailboxManagerConf.xml"));
        mailboxManagerProvider.configure(confFile.getChild("mailboxmanager",
                false));
        assertTrue(mailboxManagerProvider.getMailboxManagerFactory() instanceof MailboxManagerFactory);
        MockMailboxManagerFactory factory = (MockMailboxManagerFactory) mailboxManagerProvider
                .getMailboxManagerFactory();
        assertEquals(confFile.getChild("mailboxmanager").getChild("factory"),
                factory.configuration);
    }

}
