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

public abstract class AbstractTestFetch extends BaseTestSelectedState {

    public AbstractTestFetch(HostSystem system) {
        super(system);
    }

    public void testFetchTextUS() throws Exception {
        scriptTest("FetchText", Locale.US);
    }

    public void testFetchBodyNoSectionUS() throws Exception {
        scriptTest("FetchBodyNoSection", Locale.US);
    }
    
    public void testFetchTextIT() throws Exception {
        scriptTest("FetchText", Locale.ITALY);
    }

    public void testFetchBodyNoSectionIT() throws Exception {
        scriptTest("FetchBodyNoSection", Locale.ITALY);
    }
    
    public void testFetchTextKOREA() throws Exception {
        scriptTest("FetchText", Locale.KOREA);
    }

    public void testFetchBodyNoSectionKOREA() throws Exception {
        scriptTest("FetchBodyNoSection", Locale.KOREA);
    }
}
