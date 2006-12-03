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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.mailboxmanager.mailstore.MailStoreMailboxManagerFactory;
import org.apache.james.mailboxmanager.manager.MailboxManagerFactory;
import org.apache.james.mailboxmanager.mock.MockMailboxManagerFactory;
import org.apache.james.mailboxmanager.torque.TorqueMailboxManagerFactory;
import org.apache.james.services.FileSystem;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.james.MockFileSystem;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.xml.sax.SAXException;

public class VirtualMailboxManagerFactoryTest extends MockObjectTestCase {

    VirtualMailboxManagerFactory virtualMailboxManagerFactory;

    public void setUp() {
        virtualMailboxManagerFactory = new VirtualMailboxManagerFactory();
    }

    public void testReadConf() throws ConfigurationException, SAXException,
            IOException {
        Configuration confFile = new DefaultConfigurationBuilder()
                .buildFromFile("src/conf/james-config.xml");
        Configuration conf = confFile.getChild("mailboxmanager", false).getChild("factory",false);
        VirtualMailboxManagerFactory vmm = new VirtualMailboxManagerFactory();
        vmm.enableLogging(new MockLogger());
        vmm.configure(conf);
    }

    public void testVirtualRepositoryMix() throws Exception {
        Configuration confFile = new DefaultConfigurationBuilder()
                .build(getClass()
                        .getResourceAsStream(
                                "/org/apache/james/mailboxmanager/testdata/VirtualRepositoryMix.xml"));
        Configuration conf = confFile.getChild("mailboxmanager", false).getChild("factory",false);



        VirtualMailboxManagerFactory virtualMailboxManagerFactory = new VirtualMailboxManagerFactory();
        virtualMailboxManagerFactory.enableLogging(new MockLogger());
        virtualMailboxManagerFactory.configure(conf);
        virtualMailboxManagerFactory.service(getMockService());
//        virtualMailboxManagerFactory.initialize();

        String[] expected = { "#system", "#user1", "#user2", "#user3", "#mail",
                "#user" };
        List resultList = new ArrayList(virtualMailboxManagerFactory
                .getMountMap().keySet());
        List expectedList = Arrays.asList(expected);
        assertEquals(expectedList, resultList);

        MailboxManagerFactory[] factories = new MailboxManagerFactory[expected.length];
        for (int i = 0; i < factories.length; i++) {
            factories[i] = (MailboxManagerFactory) virtualMailboxManagerFactory
                    .getMountMap().get(expected[i]);
        }

        assertSame(factories[1], factories[5]);
        assertSame(factories[2], factories[3]);
        assertNotSame(factories[1], factories[2]);
        assertNotSame(factories[1], factories[0]);
        assertNotSame(factories[0], factories[4]);

        MockMailboxManagerFactory mockFactory1 = (MockMailboxManagerFactory) factories[1];
        MockMailboxManagerFactory mockFactory2 = (MockMailboxManagerFactory) factories[2];

        Configuration[] mounts = conf.getChild(
                "mounts", false).getChildren("mount");

        assertEquals(mounts[0].getChild("target", false),
                mockFactory1.configuration);
        assertEquals(mounts[1].getChild("target", false),
                mockFactory2.configuration);

//        assertEquals(1, mockFactory1.init);
//        assertEquals(1, mockFactory2.init);

        assertTrue(factories[0] instanceof MailStoreMailboxManagerFactory);
        assertTrue(factories[4] instanceof TorqueMailboxManagerFactory);

    }
    
    
    public void testVirtualRepositoryMixWithInit() throws Exception {
        Configuration confFile = new DefaultConfigurationBuilder()
                .build(getClass()
                        .getResourceAsStream(
                                "/org/apache/james/mailboxmanager/testdata/VirtualRepositoryMix.xml"));
        Configuration conf = confFile.getChild("mailboxmanager-without-torque", false).getChild("factory",false);


        VirtualMailboxManagerFactory virtualMailboxManagerFactory = new VirtualMailboxManagerFactory();
        virtualMailboxManagerFactory.enableLogging(new MockLogger());
        virtualMailboxManagerFactory.configure(conf);
        virtualMailboxManagerFactory.service(getMockService());
        virtualMailboxManagerFactory.initialize();

        String[] expected = { "#system", "#user1", "#user2", "#user3", "#mail",
                "#user" };
        List resultList = new ArrayList(virtualMailboxManagerFactory
                .getMountMap().keySet());
        List expectedList = Arrays.asList(expected);
        assertEquals(expectedList, resultList);

        MailboxManagerFactory[] factories = new MailboxManagerFactory[expected.length];
        for (int i = 0; i < factories.length; i++) {
            factories[i] = (MailboxManagerFactory) virtualMailboxManagerFactory
                    .getMountMap().get(expected[i]);
        }

        assertSame(factories[1], factories[5]);
        assertSame(factories[2], factories[3]);
        assertNotSame(factories[1], factories[2]);
        assertNotSame(factories[1], factories[0]);
        assertNotSame(factories[0], factories[4]);

        MockMailboxManagerFactory mockFactory1 = (MockMailboxManagerFactory) factories[1];
        MockMailboxManagerFactory mockFactory2 = (MockMailboxManagerFactory) factories[2];
        MockMailboxManagerFactory mockFactory4 = (MockMailboxManagerFactory) factories[4];

        Configuration[] mounts = conf.getChild(
                "mounts", false).getChildren("mount");

        assertEquals(mounts[0].getChild("target", false),
                mockFactory1.configuration);
        assertEquals(mounts[1].getChild("target", false),
                mockFactory2.configuration);
        assertEquals(mounts[3].getChild("target", false),
                mockFactory4.configuration);

        assertEquals(1, mockFactory1.init);
        assertEquals(1, mockFactory2.init);
        assertEquals(1, mockFactory4.init);

        assertTrue(factories[0] instanceof MailStoreMailboxManagerFactory);
        

    }
    

    public void testMountMap() {
        Map mountMap = virtualMailboxManagerFactory.getMountMap();
        String[] expected = { "xaaa", "xaab", "zaa", "b", "c" };
        mountMap.put("c", "x");
        mountMap.put("xaaa", "x");
        mountMap.put("xaab", "x");
        mountMap.put("zaa", "x");
        mountMap.put("b", "x");
        List resultList = new ArrayList(mountMap.keySet());
        List expectedList = Arrays.asList(expected);
        assertEquals(expectedList, resultList);
    }

    protected static Set toSet(Object[] o) {
        return new HashSet(Arrays.asList(o));
    }
    
    protected ServiceManager getMockService() {
        Mock mockService = mock(ServiceManager.class);
        mockService.expects(atMostOnce()).method("lookup").with(eq(FileSystem.ROLE))
                .will(returnValue(new MockFileSystem()));
        mockService.expects(atMostOnce()).method("lookup").with(eq(Store.ROLE)).will(
                returnValue(null));
        return (ServiceManager) mockService.proxy();
    }

}
