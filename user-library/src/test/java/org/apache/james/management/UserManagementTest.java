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



package org.apache.james.management;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.apache.james.user.api.UserManagementException;
import org.apache.james.user.lib.UserManagement;
import org.apache.james.userrepository.MockUsersRepository;
import org.apache.james.usersstore.MockUsersStore;

/**
 * Tests the UserManagement
 */
public class UserManagementTest extends TestCase {

    private MockUsersRepository m_mockUsersRepository;
    private UserManagement m_userManagement;

    protected void setUp() throws Exception {
        m_mockUsersRepository = new MockUsersRepository();

        m_userManagement = new UserManagement();      
        m_userManagement.setUsersRepository(m_mockUsersRepository);
        m_userManagement.setUsersStore(new MockUsersStore(m_mockUsersRepository));
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
        List<String> users = Arrays.asList(usersArray);

        for (int i = 0; i < users.size(); i++) {
            String user = users.get(i);
            assertTrue("user added", m_mockUsersRepository.addUser(user, "test"));
        }

        String[] userNames = m_userManagement.listAllUsers(null);
        assertEquals("user count", users.size(), userNames.length);

        for (int i = 0; i < userNames.length; i++) {
            String user = userNames[i];
            if (!users.contains(user)) fail("user not listed");
        }
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
        List<String> userRepositoryNames = m_userManagement.getUserRepositoryNames();
        assertTrue("default is there", userRepositoryNames.contains("LocalUsers"));
    }
}
