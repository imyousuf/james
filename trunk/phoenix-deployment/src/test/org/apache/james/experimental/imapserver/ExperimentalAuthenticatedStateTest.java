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

import java.util.Locale;

import org.apache.james.test.functional.imap.AbstractAuthenticatedStateTestSuite;

public class ExperimentalAuthenticatedStateTest extends AbstractAuthenticatedStateTestSuite {
    public ExperimentalAuthenticatedStateTest() throws Exception {
        super(HostSystemFactory.createStandardImap());
    }
    
    public void testSubscribeUS() throws Exception {
        // TODO: user is automatically subscribed to INBOX. 
        // Check whether this is correct behaviour
    }
    
    public void testSelectEmptyUS() throws Exception {
//      TODO: \recent flag is returned by FLAGS - specification seems ambiguous on this
    }
    
    public void testAppendExamineInboxUS() throws Exception {
//      TODO: \recent flag is returned by FLAGS - specification seems ambiguous on this
    }
    
    public void testAppendSelectInboxUS() throws Exception {
//      TODO: \recent flag is returned by FLAGS - specification seems ambiguous on this
    }
    
    public void testListMailboxesUS() throws Exception {
// TODO: fix bug - complete hierarchy returned
    }

    public void testSelectAppendUS() throws Exception {
//      TODO: \recent flag is returned by FLAGS - specification seems ambiguous on this
    }
    
    public void testAppendExpungeUS() throws Exception {
//      TODO: \recent flag is returned by FLAGS - specification seems ambiguous on this
    }
    
    public void testListNamespaceUS() throws Exception {
        // TODO: root mailbox should be marked as Noselect
    }
    
    public void testSubscribeITALY() throws Exception {
//      TODO: \recent flag is returned by FLAGS - specification seems ambiguoITALY on this
    }
    
    public void testSelectEmptyITALY() throws Exception {
//      TODO: \recent flag is returned by FLAGS - specification seems ambiguoITALY on this
    }
    
    public void testAppendExamineInboxITALY() throws Exception {
//      TODO: \recent flag is returned by FLAGS - specification seems ambiguoITALY on this
    }
    
    public void testAppendSelectInboxITALY() throws Exception {
//      TODO: \recent flag is returned by FLAGS - specification seems ambiguoITALY on this
    }
    
    public void testListMailboxesITALY() throws Exception {
// TODO: fix bug - complete hierarchy returned
    }

    public void testSelectAppendITALY() throws Exception {
//      TODO: \recent flag is returned by FLAGS - specification seems ambiguoITALY on this
    }
    
    public void testAppendExpungeITALY() throws Exception {
//      TODO: \recent flag is returned by FLAGS - specification seems ambiguoITALY on this
    }
    
    public void testListNamespaceITALY() throws Exception {
        // TODO: root mailbox should be marked as Noselect
    }
    
    
    public void testSubscribeKOREA() throws Exception {
//      TODO: \recent flag is returned by FLAGS - specification seems ambiguoKOREA on this
    }
    
    public void testSelectEmptyKOREA() throws Exception {
//      TODO: \recent flag is returned by FLAGS - specification seems ambiguoKOREA on this
    }
    
    public void testAppendExamineInboxKOREA() throws Exception {
//      TODO: \recent flag is returned by FLAGS - specification seems ambiguoKOREA on this
    }
    
    public void testAppendSelectInboxKOREA() throws Exception {
//      TODO: \recent flag is returned by FLAGS - specification seems ambiguoKOREA on this
    }
    
    public void testListMailboxesKOREA() throws Exception {
// TODO: fix bug - complete hierarchy returned
    }

    public void testSelectAppendKOREA() throws Exception {
//      TODO: \recent flag is returned by FLAGS - specification seems ambiguoKOREA on this
    }
    
    public void testAppendExpungeKOREA() throws Exception {
//      TODO: \recent flag is returned by FLAGS - specification seems ambiguoKOREA on this
    }
    
    public void testListNamespaceKOREA() throws Exception {
        // TODO: root mailbox should be marked as Noselect
    }
}
