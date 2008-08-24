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

package org.apache.james.test.functional.imap;

import java.util.Locale;


abstract public class AbstractAuthenticatedStateTestSuite extends AbstractTestForAuthenticatedState {

    public AbstractAuthenticatedStateTestSuite(HostSystem system) throws Exception
    {
        super(system);
    }

    public void testNoopUS() throws Exception {
        scriptTest("Noop", Locale.US); 
    }
    
    public void testLogoutUS() throws Exception {
        scriptTest("Logout", Locale.US); 
    }
    
    public void testCapabilityUS() throws Exception {
        scriptTest("Capability", Locale.US); 
    }
    
    public void testAppendExamineInboxUS() throws Exception {
        scriptTest("AppendExamineInbox", Locale.US);
    }

    public void testAppendSelectInboxUS() throws Exception {
        scriptTest("AppendSelectInbox", Locale.US);
    }
    
    public void testCreateUS() throws Exception {
        scriptTest("Create", Locale.US); 
    }
    
    public void testExamineEmptyUS() throws Exception {
        scriptTest("ExamineEmpty", Locale.US); 
    }
    
    public void testSelectEmptyUS() throws Exception {
        scriptTest("SelectEmpty", Locale.US);
    }
        
    public void testListNamespaceUS() throws Exception {
        scriptTest("ListNamespace", Locale.US); 
    }
    
    public void testListMailboxesUS() throws Exception {
        scriptTest("ListMailboxes", Locale.US);
    }
    
    public void testStatusUS() throws Exception {
        scriptTest("Status", Locale.US); 
    }
    
    public void testSubscribeUS() throws Exception {
        scriptTest("Subscribe", Locale.US);
    }
    
    public void testDeleteUS() throws Exception {
        scriptTest("Delete", Locale.US); 
    }
    
    public void testAppendUS() throws Exception {
        scriptTest("Append", Locale.US);
    }
    
    public void testAppendExpungeUS() throws Exception {
        scriptTest("AppendExpunge", Locale.US);
    }
    
    public void testSelectAppendUS() throws Exception {
        scriptTest("SelectAppend", Locale.US);
    }
    
    public void testStringArgsUS() throws Exception {
        scriptTest("StringArgs", Locale.US);
    }
    
    public void testValidNonAuthenticatedUS() throws Exception {
        scriptTest("ValidNonAuthenticated", Locale.US);
    }
    

    public void testNoopITALY() throws Exception {
        scriptTest("Noop", Locale.ITALY); 
    }
    
    public void testLogoutITALY() throws Exception {
        scriptTest("Logout", Locale.ITALY); 
    }
    
    public void testCapabilityITALY() throws Exception {
        scriptTest("Capability", Locale.ITALY); 
    }
    
    public void testAppendExamineInboxITALY() throws Exception {
        scriptTest("AppendExamineInbox", Locale.ITALY);
    }

    public void testAppendSelectInboxITALY() throws Exception {
        scriptTest("AppendSelectInbox", Locale.ITALY);
    }
    
    public void testCreateITALY() throws Exception {
        scriptTest("Create", Locale.ITALY); 
    }
    
    public void testExamineEmptyITALY() throws Exception {
        scriptTest("ExamineEmpty", Locale.ITALY); 
    }
    
    public void testSelectEmptyITALY() throws Exception {
        scriptTest("SelectEmpty", Locale.ITALY);
    }
        
    public void testListNamespaceITALY() throws Exception {
        scriptTest("ListNamespace", Locale.ITALY); 
    }
    
    public void testListMailboxesITALY() throws Exception {
        scriptTest("ListMailboxes", Locale.ITALY);
    }
    
    public void testStatusITALY() throws Exception {
        scriptTest("Status", Locale.ITALY); 
    }
    
    public void testSubscribeITALY() throws Exception {
        scriptTest("Subscribe", Locale.ITALY);
    }
    
    public void testDeleteITALY() throws Exception {
        scriptTest("Delete", Locale.ITALY); 
    }
    
    public void testAppendITALY() throws Exception {
        scriptTest("Append", Locale.ITALY);
    }
    
    public void testAppendExpungeITALY() throws Exception {
        scriptTest("AppendExpunge", Locale.ITALY);
    }
    
    public void testSelectAppendITALY() throws Exception {
        scriptTest("SelectAppend", Locale.ITALY);
    }
    
    public void testStringArgsITALY() throws Exception {
        scriptTest("StringArgs", Locale.ITALY);
    }
    
    public void testValidNonAuthenticatedITALY() throws Exception {
        scriptTest("ValidNonAuthenticated", Locale.ITALY);
    }
    

    public void testNoopKOREA() throws Exception {
        scriptTest("Noop", Locale.KOREA); 
    }
    
    public void testLogoutKOREA() throws Exception {
        scriptTest("Logout", Locale.KOREA); 
    }
    
    public void testCapabilityKOREA() throws Exception {
        scriptTest("Capability", Locale.KOREA); 
    }
    
    public void testAppendExamineInboxKOREA() throws Exception {
        scriptTest("AppendExamineInbox", Locale.KOREA);
    }

    public void testAppendSelectInboxKOREA() throws Exception {
        scriptTest("AppendSelectInbox", Locale.KOREA);
    }
    
    public void testCreateKOREA() throws Exception {
        scriptTest("Create", Locale.KOREA); 
    }
    
    public void testExamineEmptyKOREA() throws Exception {
        scriptTest("ExamineEmpty", Locale.KOREA); 
    }
    
    public void testSelectEmptyKOREA() throws Exception {
        scriptTest("SelectEmpty", Locale.KOREA);
    }
        
    public void testListNamespaceKOREA() throws Exception {
        scriptTest("ListNamespace", Locale.KOREA); 
    }
    
    public void testListMailboxesKOREA() throws Exception {
        scriptTest("ListMailboxes", Locale.KOREA);
    }
    
    public void testStatusKOREA() throws Exception {
        scriptTest("Status", Locale.KOREA); 
    }
    
    public void testSubscribeKOREA() throws Exception {
        scriptTest("Subscribe", Locale.KOREA);
    }
    
    public void testDeleteKOREA() throws Exception {
        scriptTest("Delete", Locale.KOREA); 
    }
    
    public void testAppendKOREA() throws Exception {
        scriptTest("Append", Locale.KOREA);
    }
    
    public void testAppendExpungeKOREA() throws Exception {
        scriptTest("AppendExpunge", Locale.KOREA);
    }
    
    public void testSelectAppendKOREA() throws Exception {
        scriptTest("SelectAppend", Locale.KOREA);
    }
    
    public void testStringArgsKOREA() throws Exception {
        scriptTest("StringArgs", Locale.KOREA);
    }
    
    public void testValidNonAuthenticatedKOREA() throws Exception {
        scriptTest("ValidNonAuthenticated", Locale.KOREA);
    }
    
}
