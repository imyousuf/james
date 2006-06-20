/***********************************************************************
 * Copyright (c) 2006 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/


package org.apache.james.management;

import junit.framework.TestCase;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.services.UsersRepository;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.avalon.MockServiceManager;
import org.apache.james.userrepository.MockUsersRepository;

import java.io.IOException;
import java.util.List;
import java.util.Arrays;

/**
 * Tests the UserManagement
 */
public class UserManagementTest extends TestCase {

    private MockUsersRepository m_mockUsersRepository;
    private UserManagement m_userManagement;

    protected void setUp() throws Exception {
        m_userManagement = new UserManagement();
        ContainerUtil.enableLogging(m_userManagement, new MockLogger());
        ContainerUtil.service(m_userManagement, setUpServiceManager());
    }

    private MockServiceManager setUpServiceManager() {
        MockServiceManager serviceManager = new MockServiceManager();
        m_mockUsersRepository = new MockUsersRepository();
        serviceManager.put(UsersRepository.ROLE, m_mockUsersRepository);
        return serviceManager;
    }

    public void testUserCount() throws IOException {
        assertEquals("no user yet", 0, m_userManagement.countUsers());
        m_mockUsersRepository.addUser("testCount1", "testCount");
        assertEquals("1 user", 1, m_userManagement.countUsers());
        m_mockUsersRepository.addUser("testCount2", "testCount");
        assertEquals("2 users", 2, m_userManagement.countUsers());
        m_mockUsersRepository.removeUser("testCount1");
        assertEquals("1 user", 1, m_userManagement.countUsers());
    }

    public void testAddUserAndVerify() throws IOException {
        assertTrue("user added", m_mockUsersRepository.addUser("testCount1", "testCount"));
        assertFalse("user not there", m_userManagement.verifyExists("testNotAdded"));
        assertTrue("user is there", m_userManagement.verifyExists("testCount1"));
        m_mockUsersRepository.removeUser("testCount1");
        assertFalse("user not there", m_userManagement.verifyExists("testCount1"));
    }

    public void testDelUser() throws IOException {
        assertTrue("user added", m_mockUsersRepository.addUser("testDel", "test"));
        assertFalse("user not there", m_userManagement.verifyExists("testNotDeletable"));
        assertTrue("user is there", m_userManagement.verifyExists("testDel"));
        m_mockUsersRepository.removeUser("testDel");
        assertFalse("user no longer there", m_userManagement.verifyExists("testDel"));
    }

    public void testListUsers() throws IOException {

        String[] usersArray = new String[] {"ccc", "aaa", "dddd", "bbbbb"};
        List users = Arrays.asList(usersArray);

        for (int i = 0; i < users.size(); i++) {
            String user = (String) users.get(i);
            assertTrue("user added", m_mockUsersRepository.addUser(user, "test"));
        }

        String[] userNames = m_userManagement.listAllUsers();
        assertEquals("user count", users.size(), userNames.length);

        for (int i = 0; i < userNames.length; i++) {
            String user = userNames[i];
            if (!users.contains(user)) fail("user not listed");
        }
    }

    public void testAlias() throws UserManagementException {
        m_mockUsersRepository.setForceUseJamesUser();

        // do some tests when parameter users don't exist
        try {
            m_userManagement.setAlias("testNonExist1", "testNonExist2");
            fail("user unknown to server");
        } catch (UserManagementException e) {
            // success
        }

        assertTrue("user added", m_userManagement.addUser("testAlias1", "test"));

        assertNull("no alias set", m_userManagement.getAlias("testAlias1"));

        try {
            m_userManagement.setAlias("testAlias1", "testNonExist2");
            fail("alias unknown to server");
        } catch (UserManagementException e) {
            // success
        }

        try {
            m_userManagement.setAlias("testNonExist1", "testAlias");
            fail("user unknown to server");
        } catch (UserManagementException e) {
            // success
        }

        assertTrue("user added", m_userManagement.addUser("testAlias2", "test"));

        // regular alias
        assertTrue("alias for testAlias1 set to:testAlias2", m_userManagement.setAlias("testAlias1", "testAlias2"));

        //TODO: is this correct? even primitive circular aliasing allowed!
        assertTrue("alias for testAlias2 set to:testAlias1", m_userManagement.setAlias("testAlias2", "testAlias1"));

        // did first one persist?
        assertEquals("Current alias for testAlias1 is: testAlias2", "testAlias2", m_userManagement.getAlias("testAlias1"));

        //TODO: is this correct? setting self as alias!
        assertTrue("alias for testAlias1 set to:testAlias1", m_userManagement.setAlias("testAlias1", "testAlias1"));

        assertTrue("user added", m_userManagement.addUser("testAlias3", "test"));

        // re-set, simply overwrites
        assertTrue("alias for testAlias1 set to:testAlias3", m_userManagement.setAlias("testAlias1", "testAlias3"));

        // check overwrite
        assertEquals("Current alias for testAlias1 is: testAlias3", "testAlias3", m_userManagement.getAlias("testAlias1"));

        // retreat
        assertTrue("alias for testAlias1 unset", m_userManagement.unsetAlias("testAlias1"));

        // check removed alias
        //sendCommand("showalias testAlias1");
        assertNull("User testAlias1 does not currently have an alias", m_userManagement.getAlias("testAlias1"));

    }

    public void testForward() throws UserManagementException {
        m_mockUsersRepository.setForceUseJamesUser();

        // do some tests when parameter users don't exist
        try {
            m_userManagement.setForwardAddress("testNonExist1", "testForward1@locahost");
            fail("user unknown to server");
        } catch (UserManagementException e) {
            // success
        }

        assertTrue("user added", m_userManagement.addUser("testForwardUser", "test"));

        assertNull("no forward set", m_userManagement.getForwardAddress("testForwardUser"));

        assertTrue(m_userManagement.setForwardAddress("testForwardUser", "testForward1@locahost"));

        // did it persist?
        String forwardAddress = m_userManagement.getForwardAddress("testForwardUser");
        assertEquals("forward for testForwardUser is: testForward1@locahost", "testForward1@locahost", forwardAddress);

        // re-set, simply overwrites
        assertTrue(m_userManagement.setForwardAddress("testForwardUser", "testForward2@locahost"));

        // check overwrite
        forwardAddress = m_userManagement.getForwardAddress("testForwardUser");
        assertEquals("forward for testForwardUser is: testForward2@locahost", "testForward2@locahost", forwardAddress);

        // retreat
        assertTrue("Forward for testForwardUser unset", m_userManagement.unsetForwardAddress("testForwardUser"));

        // check removed forward
        assertNull("no more forward set", m_userManagement.getForwardAddress("testForwardUser"));

    }

    public void testSetPassword() throws IOException, UserManagementException {

        assertTrue("user added", m_userManagement.addUser("testPwdUser", "pwd1"));

        assertTrue("initial password", m_mockUsersRepository.test("testPwdUser", "pwd1"));

        // set empty pwd
        assertTrue("changed to empty password", m_userManagement.setPassword("testPwdUser", ""));
        assertTrue("password changed to empty", m_mockUsersRepository.test("testPwdUser", ""));

        // change pwd
        assertTrue("changed password", m_userManagement.setPassword("testPwdUser", "pwd2"));
        assertTrue("password not changed to pwd2", m_mockUsersRepository.test("testPwdUser", "pwd2"));

        // assure case sensitivity
        assertTrue("changed password", m_userManagement.setPassword("testPwdUser", "pWD2"));
        assertFalse("password no longer pwd2", m_mockUsersRepository.test("testPwdUser", "pwd2"));
        assertTrue("password changed to pWD2", m_mockUsersRepository.test("testPwdUser", "pWD2"));

    }
}
