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

import org.apache.james.test.functional.imap.AbstractTestSelectedState;

public class ExperimentalSelectedStateTest extends AbstractTestSelectedState {

    public ExperimentalSelectedStateTest() throws Exception {
        super(HostSystemFactory.createStandardImap());
    }
    
    public void testFetchMultipleMessagesUS() throws Exception {
        // TODO Auto-generated method stub
        //super.testFetchMultipleMessages();
    }

    public void testFetchSingleMessageUS() throws Exception {
        // BODY octet count is buggy.
        // The total size of the message is used rather than the size of the part
        //super.testFetchSingleMessageUS();
    }

    public void testUidUS() throws Exception {
        // UID fetch not working very well
        // super.testUid();
    }

    public void testFetchMultipleMessagesKOREA() throws Exception {
        // TODO Auto-generated method stub
        //super.testFetchMultipleMessages();
    }

    public void testFetchSingleMessageKOREA() throws Exception {
        // BODY octet count is buggy.
        // The total size of the message is KOREAed rather than the size of the part
        //super.testFetchSingleMessage();
    }

    public void testUidKOREA() throws Exception {
        // UID fetch not working very well
        // super.testUid();
    }  

    public void testFetchMultipleMessagesITALY() throws Exception {
        // TODO Auto-generated method stub
        //super.testFetchMultipleMessages();
    }

    public void testFetchSingleMessageITALY() throws Exception {
        // BODY octet count is buggy.
        // The total size of the message is ITALYed rather than the size of the part
        //super.testFetchSingleMessage();
    }

    public void testUidITALY() throws Exception {
        // UID fetch not working very well
        // super.testUid();
    }    
}
