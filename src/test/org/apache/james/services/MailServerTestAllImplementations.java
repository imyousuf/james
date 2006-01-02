/***********************************************************************
 * Copyright (c) 2000-2005 The Apache Software Foundation.             *
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


package org.apache.james.services;

import junit.framework.TestCase;

/**
 * tests all implementations for interface MailServer
 */
abstract public class MailServerTestAllImplementations extends TestCase {

    abstract public MailServer createMailServer();
    abstract public boolean allowsPasswordlessUser();

    /**
     * while addUser() is part of MailServer interface, a user cannot be tested for afterwards
     * at the same time, James allows to do exactly this via isLocalUser(), other implementations
     * might vary. 
     */
    abstract public boolean canTestUserExists();
    abstract public boolean isUserExisting(MailServer mailServerImpl, String username);
    
    public void testId() {
        MailServer mailServer = createMailServer();
        
        String id = mailServer.getId();
        assertNotNull("mail id not null", id);
        assertFalse("mail id not empty", "".equals(id));
    }
    
    public void testIdIncrement() {
        MailServer mailServer = createMailServer();
        
        String id1 = mailServer.getId();
        String id2 = mailServer.getId();
        assertFalse("next id is different", id1.equals(id2));
    }
    
    public void testAddUser() {
        
        // addUser acts on field localUsers for class org.apache.james.James 
        // thus, it is unrelated to getUserInbox() for the only known implementation of MailServer
        // TODO clarify this 
        
        MailServer mailServer = createMailServer();

        String userName = "testUserName";
        MailRepository userInbox = null;

        if (canTestUserExists())
        {
            assertFalse("this is a fresh user", isUserExisting(mailServer, userName));
        }
        
        boolean allowsPasswordlessUser = allowsPasswordlessUser();
        try {
            boolean success = mailServer.addUser(userName, null);
            if (!allowsPasswordlessUser) fail("null pwd was accepted unexpectedly");
            if (!success) fail("null pwd was not accepted unexpectedly"); 
        } catch (Exception e) {
            if (allowsPasswordlessUser) fail("null pwd not accepted unexpectedly (with exception)");
        }

        userName = userName + "_next"; 
        String password = "password";
        
        boolean success = mailServer.addUser(userName, password);
        if (!success) fail("user has not been added"); 
        
        if (canTestUserExists())
        {
            assertTrue("user is present now", isUserExisting(mailServer, userName));
        }
        
        boolean successAgain = mailServer.addUser(userName, password);
        if (successAgain) fail("user has been added two times"); 
        
    }

    public void testGetNonexisitingUserInbox() {

        //
        // TODO fix test (or James) -- test WILL FAIL
        // 
        
        MailServer mailServer = createMailServer();

        String userName = "testNonexisitingUserName";
        MailRepository userInbox = null;
        
        try {
            userInbox = mailServer.getUserInbox(userName);
            assertEquals("test user does not exist", null, userInbox);
            fail("found inbox which should be unexistent");
        } catch (NullPointerException e) {
            // this is what org.apache.james.James returns  
            // is this behavior compatible with other implementations?
            // shouldn't James behave more gracefully?
        }
    }
    
    public void testGetExisitingUserInbox() {
        
        //
        // TODO fix test (or James) -- test WILL FAIL
        // 
        
        MailServer mailServer = createMailServer();

        String userName = "testUserName";
        MailRepository userInbox = null;
        
        // getUserInbox acts on field mailboxes for class org.apache.james.James
        // thus, it is unrelated to addUser() for the only known implementation of MailServer
        // TODO clarify this 
        mailServer.addUser(userName, "password"); // !! is not retrievable via getUserInbox !!
        
        
        try {
            userInbox = mailServer.getUserInbox(userName);
            assertEquals("test user does not exist", null, userInbox);
            fail("found inbox which should be unexistent");
        } catch (NullPointerException e) {
            // this is what org.apache.james.James returns  
            // is this behavior compatible with other implementations?
            // shouldn't James behave more gracefully?
        }
    }
}
