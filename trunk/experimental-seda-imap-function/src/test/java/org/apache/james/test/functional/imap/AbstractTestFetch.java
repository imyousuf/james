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

    public void testFetchRFC822US() throws Exception {
        scriptTest("FetchRFC822", Locale.US);
    }

    public void testFetchRFC822TextUS() throws Exception {
        scriptTest("FetchRFC822Text", Locale.US);
    }

    public void testFetchRFC822HeaderUS() throws Exception {
        scriptTest("FetchRFC822Header", Locale.US);
    }

    public void testFetchRFC822KOREA() throws Exception {
        scriptTest("FetchRFC822", Locale.KOREA);
    }

    public void testFetchRFC822TextKOREA() throws Exception {
        scriptTest("FetchRFC822Text", Locale.KOREA);
    }

    public void testFetchRFC822HeaderKOREA() throws Exception {
        scriptTest("FetchRFC822Header", Locale.KOREA);
    }

    public void testFetchRFC822ITALY() throws Exception {
        scriptTest("FetchRFC822", Locale.ITALY);
    }

    public void testFetchRFC822TextITALY() throws Exception {
        scriptTest("FetchRFC822Text", Locale.ITALY);
    }

    public void testFetchRFC822HeaderITALY() throws Exception {
        scriptTest("FetchRFC822Header", Locale.ITALY);
    }
    
  public void testFetchInternalDateUS() throws Exception {
      scriptTest("FetchInternalDate", Locale.US);
  }  
  
  public void testFetchInternalDateITALY() throws Exception {
      scriptTest("FetchInternalDate", Locale.ITALY);
  } 
  
  public void testFetchInternalDateKOREA() throws Exception {
      scriptTest("FetchInternalDate", Locale.KOREA);
  } 
}
