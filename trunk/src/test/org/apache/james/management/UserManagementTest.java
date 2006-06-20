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
import org.apache.james.services.UsersStore;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.avalon.MockServiceManager;
import org.apache.james.test.mock.james.MockUsersStore;
import org.apache.james.userrepository.MockUsersRepository;

import java.util.Arrays;
import java.util.List;

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
        serviceManager.put(UsersStore.ROLE, new MockUsersStore(m_mockUsersRepository));
        return serviceManager;
    }

    public void testUserCount() throws UserManagementException {
        assertEquals("no user yet", 0, m_userManagement.countUsers(null));
        m_mockUsersRepository.addUser("testCount1", "testCount");
        assertEquals("1 user", 1, m_userManagement.countUsers(null));
        m_mockUsersRepository.addUser("testCount2", "testCount");
        assertEquals("2 users", 2, m_userManagement.countUsers(null));
        m_mockUsersRepository.removeUser("testCount1");
        assertEquals("1 user", 1, m_userManagement.countUsers(null));
    }

    public void testDefaultRepositoryIsLocalUsers() throws UserManagementException {
        m_userManagement.addUser("testCount1", "testCount", null);
        m_userManagement.addUser("testCount2", "testCount", "LocalUsers");

        assertEquals("2 users", 2, m_userManagement.countUsers(null));
        assertEquals("2 users", 2, m_userManagement.countUsers("LocalUsers"));
    }

    public void testNonExistingRepository() throws UserManagementException {
        try {
            m_userManagement.addUser("testCount1", "testCount", "NonExisting");
            fail("retrieved non-existing repository");
        } catch (UserManagementException e) {
            // success
        }
    }

    public void testAddUserAndVerify() throws UserManagementException {
        assertTrue("user added", m_mockUsersRepository.addUser("testCount1", "testCount"));
        assertFalse("user not there", m_userManagement.verifyExists("testNotAdded", null));
        assertTrue("user is there", m_userManagement.verifyExists("testCount1", null));
        m_mockUsersRepository.removeUser("testCount1");
        assertFalse("user not there", m_userManagement.verifyExists("testCount1", null));
    }

    public void testDelUser() throws UserManagementException {
        assertTrue("user added", m_mockUsersRepository.addUser("testDel", "test"));
        assertFalse("user not there", m_userManagement.verifyExists("testNotDeletable", null));
        assertTrue("user is there", m_userManagement.verifyExists("testDel", null));
        m_mockUsersRepository.removeUser("testDel");
        assertFalse("user no longer there", m_userManagement.verifyExists("testDel", null));
    }

    public void testListUsers() throws UserManagementException {

        String[] usersArray = new String[] {"ccc", "aaa", "dddd", "bbbbb"};
        List users = Arrays.asList(usersArray);

        for (int i = 0; i < users.size(); i++) {
            String user = (String) users.get(i);
            assertTrue("user added", m_mockUsersRepository.addUser(user, "test"));
        }

        String[] userNames = m_userManagement.listAllUsers(null);
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
            m_userManagement.setAlias("testNonExist1", "testNonExist2", null);
            fail("user unknown to server");
        } catch (UserManagementException e) {
            // success
        }

        assertTrue("user added", m_userManagement.addUser("testAlias1", "test", null));

        assertNull("no alias set", m_userManagement.getAlias("testAlias1", null));

        try {
            m_userManagement.setAlias("testAlias1", "testNonExist2", null);
            fail("alias unknown to server");
        } catch (UserManagementException e) {
            // success
        }

        try {
            m_userManagement.setAlias("testNonExist1", "testAlias", null);
            fail("user unknown to server");
        } catch (UserManagementException e) {
            // success
        }

        assertTrue("user added", m_userManagement.addUser("testAlias2", "test", null));

        // regular alias
        assertTrue("alias for testAlias1 set to:testAlias2", m_userManagement.setAlias("testAlias1", "testAlias2", null));

        //TODO: is this correct? even primitive circular aliasing allowed!
        assertTrue("alias for testAlias2 set to:testAlias1", m_userManagement.setAlias("testAlias2", "testAlias1", null));

        // did first one persist?
        assertEquals("Current alias for testAlias1 is: testAlias2", "testAlias2", m_userManagement.getAlias("testAlias1", null));

        //TODO: is this correct? setting self as alias!
        assertTrue("alias for testAlias1 set to:testAlias1", m_userManagement.setAlias("testAlias1", "testAlias1", null));

        assertTrue("user added", m_userManagement.addUser("testAlias3", "test", null));

        // re-set, simply overwrites
        assertTrue("alias for testAlias1 set to:testAlias3", m_userManagement.setAlias("testAlias1", "testAlias3", null));

        // check overwrite
        assertEquals("Current alias for testAlias1 is: testAlias3", "testAlias3", m_userManagement.getAlias("testAlias1", null));

        // retreat
        assertTrue("alias for testAlias1 unset", m_userManagement.unsetAlias("testAlias1", null));

        // check removed alias
        //sendCommand("showalias testAlias1");
        assertNull("User testAlias1 does not currently have an alias", m_userManagement.getAlias("testAlias1", null));

    }

    public void testForward() throws UserManagementException {
        m_mockUsersRepository.setForceUseJamesUser();

        // do some tests when parameter users don't exist
        try {
            m_userManagement.setForwardAddress("testNonExist1", "testForward1@locahost", null);
            fail("user unknown to server");
        } catch (UserManagementException e) {
            // success
        }

        assertTrue("user added", m_userManagement.addUser("testForwardUser", "test", null));

        assertNull("no forward set", m_userManagement.getForwardAddress("testForwardUser", null));

        assertTrue(m_userManagement.setForwardAddress("testForwardUser", "testForward1@locahost", null));

        // did it persist?
        String forwardAddress = m_userManagement.getForwardAddress("testForwardUser", null);
        assertEquals("forward for testForwardUser is: testForward1@locahost", "testForward1@locahost", forwardAddress);

        // re-set, simply overwrites
        assertTrue(m_userManagement.setForwardAddress("testForwardUser", "testForward2@locahost", null));

        // check overwrite
        forwardAddress = m_userManagement.getForwardAddress("testForwardUser", null);
        assertEquals("forward for testForwardUser is: testForward2@locahost", "testForward2@locahost", forwardAddress);

        // retreat
        assertTrue("Forward for testForwardUser unset", m_userManagement.unsetForwardAddress("testForwardUser", null));

        // check removed forward
        assertNull("no more forward set", m_userManagement.getForwardAddress("testForwardUser", null));

    }

    public void testSetPassword() throws UserManagementException {

        assertTrue("user added", m_userManagement.addUser("testPwdUser", "pwd1", null));

        assertTrue("initial password", m_mockUsersRepository.test("testPwdUser", "pwd1"));

        // set empty pwd
        assertTrue("changed to empty password", m_userManagement.setPassword("testPwdUser", "", null));
        assertTrue("password changed to empty", m_mockUsersRepository.test("testPwdUser", ""));

        // change pwd
        assertTrue("changed password", m_userManagement.setPassword("testPwdUser", "pwd2", null));
        assertTrue("password not changed to pwd2", m_mockUsersRepository.test("testPwdUser", "pwd2"));

        // assure case sensitivity
        assertTrue("changed password", m_userManagement.setPassword("testPwdUser", "pWD2", null));
        assertFalse("password no longer pwd2", m_mockUsersRepository.test("testPwdUser", "pwd2"));
        assertTrue("password changed to pWD2", m_mockUsersRepository.test("testPwdUser", "pWD2"));

    }
    
    public void testListRepositories() throws UserManagementException {
        List userRepositoryNames = m_userManagement.getUserRepositoryNames();
        assertTrue("default is there", userRepositoryNames.contains("LocalUsers"));
    }
}
