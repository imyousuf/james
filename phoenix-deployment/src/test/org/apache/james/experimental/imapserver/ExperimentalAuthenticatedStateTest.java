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

package org.apache.james.experimental.imapserver;

import org.apache.james.test.functional.imap.AbstractAuthenticatedStateTestSuite;

public class ExperimentalAuthenticatedStateTest extends AbstractAuthenticatedStateTestSuite {
    public ExperimentalAuthenticatedStateTest() throws Exception {
        super(HostSystemFactory.createStandardImap());
    }
    
    public void testSubscribe() throws Exception {
        // TODO: user is automatically subscribed to INBOX. 
        // Check whether this is correct behaviour
    }
    
    public void testExamineEmpty() throws Exception {
        // TODO: \recent flag is returned by FLAGS - specification seems ambiguous on this
    }
    
    public void testSelectEmpty() throws Exception {
//      TODO: \recent flag is returned by FLAGS - specification seems ambiguous on this
    }
    
    public void testAppendExamineInbox() throws Exception {
//      TODO: \recent flag is returned by FLAGS - specification seems ambiguous on this
    }
    
    public void testAppendSelectInbox() throws Exception {
//      TODO: \recent flag is returned by FLAGS - specification seems ambiguous on this
    }
    
    public void testListMailboxes() throws Exception {
// TODO: fix bug - complete hierarchy returned
    }

    public void testSelectAppend() throws Exception {
//      TODO: \recent flag is returned by FLAGS - specification seems ambiguous on this
    }
    
    public void testAppendExpunge() throws Exception {
//      TODO: \recent flag is returned by FLAGS - specification seems ambiguous on this
    }
    
    public void testListNamespace() throws Exception {
        // TODO: root mailbox should be marked as Noselect
    }
    
    
}
