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

import org.apache.james.test.functional.imap.AbstractTestSelectedInbox;

public class ExperimentalSelectedInboxTest extends AbstractTestSelectedInbox {

    public ExperimentalSelectedInboxTest() throws Exception {
        super(HostSystemFactory.createStandardImap());
    }
    
    public void testListNamespaceUS() throws Exception {
        //// scriptTest("ListNamespace", Locale.US);
    }
    
    public void testSubscribeUS() throws Exception {
        //// scriptTest("Subscribe", Locale.US);
    }
     
    public void testListNamespaceITALY() throws Exception {
        // scriptTest("ListNamespace", Locale.ITALY);
    }  
    
    public void testSubscribeITALY() throws Exception {
        // scriptTest("Subscribe", Locale.ITALY);
    }
   
    public void testListNamespaceKOREA() throws Exception {
        // scriptTest("ListNamespace", Locale.KOREA);
    }
    
    public void testSubscribeKOREA() throws Exception {
        // scriptTest("Subscribe", Locale.KOREA);
    }
}