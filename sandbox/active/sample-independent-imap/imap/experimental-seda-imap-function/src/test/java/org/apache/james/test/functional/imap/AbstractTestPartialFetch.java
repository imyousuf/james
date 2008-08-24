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

public abstract class AbstractTestPartialFetch extends AbstractTestSelectedStateBase {

    public AbstractTestPartialFetch(HostSystem system) {
        super(system);
    }
    
    public void testBodyPartialFetchUS() throws Exception {
        scriptTest("BodyPartialFetch", Locale.US);
    }
    
    public void testBodyPartialFetchIT() throws Exception {
        scriptTest("BodyPartialFetch", Locale.ITALY);
    }
    
    public void testBodyPartialFetchKO() throws Exception {
        scriptTest("BodyPartialFetch", Locale.KOREA);
    }
    
    public void testTextPartialFetchUS() throws Exception {
        scriptTest("TextPartialFetch", Locale.US);
    }
    
    public void testTextPartialFetchKO() throws Exception {
        scriptTest("TextPartialFetch", Locale.US);
    }
    
    public void testTextPartialFetchIT() throws Exception {
        scriptTest("TextPartialFetch", Locale.US);
    }
    
    public void testMimePartialFetchUS() throws Exception {
        scriptTest("MimePartialFetch", Locale.US);
    }
    
    public void testMimePartialFetchIT() throws Exception {
        scriptTest("MimePartialFetch", Locale.ITALY);
    }
    
    public void testMimePartialFetchKO() throws Exception {
        scriptTest("MimePartialFetch", Locale.KOREA);
    }
    
    public void testHeaderPartialFetchUS() throws Exception {
        scriptTest("HeaderPartialFetch", Locale.US);
    }

    public void testHeaderPartialFetchIT() throws Exception {
        scriptTest("HeaderPartialFetch", Locale.ITALY);
    }

    public void testHeaderPartialFetchKO() throws Exception {
        scriptTest("HeaderPartialFetch", Locale.KOREA);
    }
}
