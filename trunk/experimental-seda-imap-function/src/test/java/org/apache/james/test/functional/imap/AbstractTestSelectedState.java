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

public abstract class AbstractTestSelectedState extends BaseTestSelectedState {

    public AbstractTestSelectedState(HostSystem system) {
        super(system);
    }

    public void testCheckUS() throws Exception {
        scriptTest("Check", Locale.US);
    }
    
    public void testExpungeUS() throws Exception {
        scriptTest("Expunge", Locale.US);
    }
    
    public void testSearchUS() throws Exception {
        scriptTest("Search", Locale.US);
    }
    
    public void testFetchSingleMessageUS() throws Exception {
        scriptTest("FetchSingleMessage", Locale.US);
    }
    
    public void testFetchMultipleMessagesUS() throws Exception {
        scriptTest("FetchMultipleMessages", Locale.US);
    }
    
    public void testFetchPeekUS() throws Exception {
        scriptTest("FetchPeek", Locale.US);
    }
    
    public void testStoreUS() throws Exception {
        scriptTest("Store", Locale.US);
    }
    
    public void testCopyUS() throws Exception {
        scriptTest("Copy", Locale.US);
    }
    
    public void testUidUS() throws Exception {
        scriptTest("Uid", Locale.US);
    }
    

    public void testCheckITALY() throws Exception {
        scriptTest("Check", Locale.ITALY);
    }
    
    public void testExpungeITALY() throws Exception {
        scriptTest("Expunge", Locale.ITALY);
    }
    
    public void testSearchITALY() throws Exception {
        scriptTest("Search", Locale.ITALY);
    }
    
    public void testFetchSingleMessageITALY() throws Exception {
        scriptTest("FetchSingleMessage", Locale.ITALY);
    }
    
    public void testFetchMultipleMessagesITALY() throws Exception {
        scriptTest("FetchMultipleMessages", Locale.ITALY);
    }
    
    public void testFetchPeekITALY() throws Exception {
        scriptTest("FetchPeek", Locale.ITALY);
    }
    
    public void testStoreITALY() throws Exception {
        scriptTest("Store", Locale.ITALY);
    }
    
    public void testCopyITALY() throws Exception {
        scriptTest("Copy", Locale.ITALY);
    }
    
    public void testUidITALY() throws Exception {
        scriptTest("Uid", Locale.ITALY);
    }
    

    public void testCheckKOREA() throws Exception {
        scriptTest("Check", Locale.KOREA);
    }
    
    public void testExpungeKOREA() throws Exception {
        scriptTest("Expunge", Locale.KOREA);
    }
    
    public void testSearchKOREA() throws Exception {
        scriptTest("Search", Locale.KOREA);
    }
    
    public void testFetchSingleMessageKOREA() throws Exception {
        scriptTest("FetchSingleMessage", Locale.KOREA);
    }
    
    public void testFetchMultipleMessagesKOREA() throws Exception {
        scriptTest("FetchMultipleMessages", Locale.KOREA);
    }
    
    public void testFetchPeekKOREA() throws Exception {
        scriptTest("FetchPeek", Locale.KOREA);
    }
    
    public void testStoreKOREA() throws Exception {
        scriptTest("Store", Locale.KOREA);
    }
    
    public void testCopyKOREA() throws Exception {
        scriptTest("Copy", Locale.KOREA);
    }
    
    public void testUidKOREA() throws Exception {
        scriptTest("Uid", Locale.KOREA);
    }
}
