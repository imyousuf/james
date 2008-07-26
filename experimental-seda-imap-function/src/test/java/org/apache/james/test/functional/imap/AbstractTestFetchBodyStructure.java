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

public abstract class AbstractTestFetchBodyStructure extends AbstractTestSelectedStateBase {

    public AbstractTestFetchBodyStructure(HostSystem system) {
        super(system);
    }

    public void testFetchFetchSimpleBodyStructureUS() throws Exception {
        scriptTest("FetchSimpleBodyStructure", Locale.US);
    }

    public void testFetchFetchSimpleBodyStructureKOREA() throws Exception {
        scriptTest("FetchSimpleBodyStructure", Locale.KOREA);
    }

    public void testFetchFetchSimpleBodyStructureITALY() throws Exception {
        scriptTest("FetchSimpleBodyStructure", Locale.ITALY);
    }

    public void testFetchFetchMultipartBodyStructureUS() throws Exception {
        scriptTest("FetchMultipartBodyStructure", Locale.US);
    }

    public void testFetchFetchMultipartBodyStructureKOREA() throws Exception {
        scriptTest("FetchMultipartBodyStructure", Locale.KOREA);
    }

    public void testFetchFetchMultipartBodyStructureITALY() throws Exception {
        scriptTest("FetchMultipartBodyStructure", Locale.ITALY);
    }
}
