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



package org.apache.james.services;

import junit.framework.TestCase;

/**
 * tests all implementations for interface MailServer
 */
abstract public class MailServerTestAllImplementations extends TestCase {
    
    protected static final String EXISTING_USER_NAME = "testExistingUserName";

    abstract public MailServer createMailServer() throws Exception;
    abstract public boolean allowsPasswordlessUser();

    /**
     * while addUser() is part of MailServer interface, a user cannot be tested for afterwards
     * at the same time, James allows to do exactly this via isLocalUser(), other implementations
     * might vary. 
     */
    abstract public boolean canTestUserExists();
    abstract public boolean isUserExisting(MailServer mailServerImpl, String username);
    
    public void testId() throws Exception {
        MailServer mailServer = createMailServer();
        
        String id = mailServer.getId();
        assertNotNull("mail id not null", id);
        assertFalse("mail id not empty", "".equals(id));
    }
    
    public void testIdIncrement() throws Exception {
        MailServer mailServer = createMailServer();
        
        String id1 = mailServer.getId();
        String id2 = mailServer.getId();
        assertFalse("next id is different", id1.equals(id2));
    }
    
  
}
