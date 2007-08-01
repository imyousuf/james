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

public abstract class AbstractTestSelectedState extends BaseTestSelectedState {

    public AbstractTestSelectedState(HostSystem system) {
        super(system);
    }

    public void testCheck() throws Exception {
        scriptTest("Check");
    }
    
    public void testExpunge() throws Exception {
        scriptTest("Expunge");
    }
    
    public void testSearch() throws Exception {
        scriptTest("Search");
    }
    
    public void testFetchSingleMessage() throws Exception {
        scriptTest("FetchSingleMessage");
    }
    
    public void testFetchMultipleMessages() throws Exception {
        scriptTest("FetchMultipleMessages");
    }
    
    public void testFetchPeek() throws Exception {
        scriptTest("FetchPeek");
    }
    
    public void testStore() throws Exception {
        scriptTest("Store");
    }
    
    public void testCopy() throws Exception {
        scriptTest("Copy");
    }
    
    public void testUid() throws Exception {
        scriptTest("Uid");
    }
}
