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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.james.mailboxmanager.ListResult;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.impl.VirtualMailboxManagerFactory.LengthComparator;
import org.apache.james.mailboxmanager.manager.MailboxManager;
import org.apache.james.mailboxmanager.manager.MailboxManagerFactory;
import org.apache.james.mailboxmanager.mock.MockUser;
import org.apache.james.services.User;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.jmock.core.Constraint;
import org.jmock.core.constraint.IsEqual;
import org.jmock.core.constraint.IsSame;

public class VirtualMailboxManagerTest extends MockObjectTestCase {

    protected User user = new MockUser();

    protected VirtualMailboxManager virtualMailboxManager;

    public void setUp() {
        virtualMailboxManager = new VirtualMailboxManager();
        virtualMailboxManager.setUser(user);
    }

    public void testGetMailboxManager() throws MailboxManagerException {
        String[] points = { "#mail", "#mail.t1.t2", "#mail.t1", "#user",
                "#user.t1.t2.t3" };
        MailboxManager[] manager = new MailboxManager[] {
                (MailboxManager) mock(MailboxManager.class).proxy(),
                (MailboxManager) mock(MailboxManager.class).proxy(),
                (MailboxManager) mock(MailboxManager.class).proxy(),
                (MailboxManager) mock(MailboxManager.class).proxy(),
                (MailboxManager) mock(MailboxManager.class).proxy() };
        Mock[] mailboxManagerFactoryMocks = createMailboxManagerFactoryMocks(
                manager, 2);
        MailboxManagerFactory[] mailboxManagerFactories = proxyFactoryMocks(mailboxManagerFactoryMocks);

        addMountPoints(points, mailboxManagerFactories);
        assertSame(manager[0], virtualMailboxManager
                .getMailboxManager("#mail.t3"));
        assertSame(manager[0], virtualMailboxManager.getMailboxManager("#mail"));
        assertSame(manager[1], virtualMailboxManager
                .getMailboxManager("#mail.t1.t2"));
        assertSame(manager[1], virtualMailboxManager
                .getMailboxManager("#mail.t1.t2.t4"));
        assertSame(manager[2], virtualMailboxManager
                .getMailboxManager("#mail.t1"));
        assertSame(manager[2], virtualMailboxManager
                .getMailboxManager("#mail.t1.t3"));
        assertSame(manager[3], virtualMailboxManager.getMailboxManager("#user"));
        assertSame(manager[3], virtualMailboxManager
                .getMailboxManager("#user.t2"));
        assertSame(manager[4], virtualMailboxManager
                .getMailboxManager("#user.t1.t2.t3"));
        assertSame(manager[4], virtualMailboxManager
                .getMailboxManager("#user.t1.t2.t3.t4"));
        try {
            virtualMailboxManager.getMailboxManager("#other");
            fail("should throw exception");
        } catch (MailboxManagerException e) {
        }
        

    }

    public void testList() throws MailboxManagerException {
        String[] expected = { "#mail.t1.t2", "#mail.t1.t3", "#mail",
                "#mail.group.t5", "#mail.group.6", "#system.t1" };
        String[] points = { "#mail", "#mail.group", "#system" };
        Mock[] mailboxManagerMocks = { mock(MailboxManager.class),
                mock(MailboxManager.class), mock(MailboxManager.class) };
        MailboxManager[] mailboxManager = proxyMocks(mailboxManagerMocks);
        Mock[] mailboxManagerFactoryMocks = createMailboxManagerFactoryMocks(
                mailboxManager, 1);
        MailboxManagerFactory[] mailboxManagerFactories = proxyFactoryMocks(mailboxManagerFactoryMocks);

        Constraint[] args = { new IsEqual(""), new IsEqual("%"),
                new IsEqual(Boolean.FALSE) };

        mailboxManagerMocks[0].expects(once()).method("list").with(args).will(
                returnValue(generateListResults(new String[] { expected[0],
                        expected[1], expected[2] })));

        mailboxManagerMocks[1].expects(once()).method("list").with(args).will(
                returnValue(generateListResults(new String[] { expected[3],
                        expected[4] })));

        mailboxManagerMocks[2].expects(once()).method("list").with(args).will(
                returnValue(generateListResults(new String[] { expected[5] })));

        addMountPoints(points, mailboxManagerFactories);

        ListResult[] result = virtualMailboxManager.list("", "%", false);
        assertEquals(expected.length, result.length);
        assertEquals(new HashSet(Arrays.asList(expected)), toNamesSet(result));
        System.out.println(toNamesSet(result));

    }
    
    public void testSubscribe() throws MailboxManagerException {
        String[] points = { "#mail" , "#mail.group", "#system"}; // , 
        Mock[] mailboxManagerMocks = createMailboxManagerMocks(points.length);
        MailboxManager[] mailboxManager = proxyMocks(mailboxManagerMocks);
        Mock[] mailboxManagerFactoryMocks = createMailboxManagerFactoryMocks(
                mailboxManager, 1);
        MailboxManagerFactory[] mailboxManagerFactories = proxyFactoryMocks(mailboxManagerFactoryMocks);
        addMountPoints(points, mailboxManagerFactories);
        
        String[] subscribe= {"#mail.user1.Trash","#mail.group.test","#system.go"};
        for (int i = 0; i < subscribe.length; i++) {
            Constraint[] args;
            args=new Constraint[] {eq(subscribe[i]),eq(true)}; 
            mailboxManagerMocks[i].expects(once()).method("setSubscription").with(args).isVoid();
            
            virtualMailboxManager.setSubscription(subscribe[i],true);            
        }


    }
    
    protected Mock[] createMailboxManagerMocks(int count) {
        Mock[] mocks = new Mock[count];
        for (int i = 0; i < mocks.length; i++) {
            mocks[i]=mock(MailboxManager.class);
        }
        return mocks;
    }

    protected Mock[] createMailboxManagerFactoryMocks(MailboxManager[] manager,
            int expected) {
        Mock[] mocks = new Mock[manager.length];
        for (int i = 0; i < mocks.length; i++) {
            mocks[i] = mock(MailboxManagerFactory.class);
            mocks[i].expects(exactly(expected)).method(
                    "getMailboxManagerInstance").with(new IsSame(user)).will(
                    returnValue(manager[i]));
        }
        return mocks;
    }

    protected static MailboxManager[] proxyMocks(Mock[] mocks) {
        MailboxManager[] managers = new MailboxManager[mocks.length];
        for (int i = 0; i < mocks.length; i++) {
            managers[i] = (MailboxManager) mocks[i].proxy();
        }
        return managers;
    }

    protected static MailboxManagerFactory[] proxyFactoryMocks(Mock[] mocks) {
        MailboxManagerFactory[] factories = new MailboxManagerFactory[mocks.length];
        for (int i = 0; i < mocks.length; i++) {
            factories[i] = (MailboxManagerFactory) mocks[i].proxy();
        }
        return factories;
    }

    protected void addMountPoints(String[] points,
            MailboxManagerFactory[] mailboxManagerFactories) {
        Map mountMap = new TreeMap(new LengthComparator());
        for (int i = 0; i < mailboxManagerFactories.length; i++) {

            mountMap.put(points[i], mailboxManagerFactories[i]);
        }
        virtualMailboxManager.setMountMap(mountMap);
    }

    protected static Set toNamesSet(ListResult[] listResult) {
        Set nameSet = new HashSet();
        for (int i = 0; i < listResult.length; i++) {
            nameSet.add(listResult[i].getName());
        }
        return nameSet;
    }

    protected static ListResult[] generateListResults(String[] names) {
        ListResult[] result = new ListResult[names.length];
        for (int i = 0; i < names.length; i++) {
            result[i] = new ListResultImpl(names[i], ".");
        }
        return result;
    }

}
