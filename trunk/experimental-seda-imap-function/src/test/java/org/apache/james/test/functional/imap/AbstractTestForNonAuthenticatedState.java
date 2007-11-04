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

public abstract class AbstractTestForNonAuthenticatedState extends
        BaseTestNonAuthenticatedState {

    public AbstractTestForNonAuthenticatedState(HostSystem system) {
        super(system);
    }

    public void testNoopUS() throws Exception {
        scriptTest("Noop", Locale.US);
    }
    
    public void testLogoutUS() throws Exception {
        scriptTest("Logout", Locale.US);
    }
    
    public void testCapabilityUS() throws Exception {
        scriptTest("Capability", Locale.US);
    }
    
    public void testLoginUS() throws Exception {
        scriptTest("Login", Locale.US);
    }
    
    public void testValidAuthenticatedUS() throws Exception {
        scriptTest("ValidAuthenticated", Locale.US);
    }
    
    public void testValidSelectedUS() throws Exception {
        scriptTest("ValidSelected", Locale.US);
    }
    
    public void testAuthenticateUS() throws Exception {
        scriptTest("Authenticate", Locale.US);
    }
    

    public void testNoopITALY() throws Exception {
        scriptTest("Noop", Locale.ITALY);
    }
    
    public void testLogoutITALY() throws Exception {
        scriptTest("Logout", Locale.ITALY);
    }
    
    public void testCapabilityITALY() throws Exception {
        scriptTest("Capability", Locale.ITALY);
    }
    
    public void testLoginITALY() throws Exception {
        scriptTest("Login", Locale.ITALY);
    }
    
    public void testValidAuthenticatedITALY() throws Exception {
        scriptTest("ValidAuthenticated", Locale.ITALY);
    }
    
    public void testValidSelectedITALY() throws Exception {
        scriptTest("ValidSelected", Locale.ITALY);
    }
    
    public void testAuthenticateITALY() throws Exception {
        scriptTest("Authenticate", Locale.ITALY);
    }
    
    public void testNoopKOREA() throws Exception {
        scriptTest("Noop", Locale.KOREA);
    }
    
    public void testLogoutKOREA() throws Exception {
        scriptTest("Logout", Locale.KOREA);
    }
    
    public void testCapabilityKOREA() throws Exception {
        scriptTest("Capability", Locale.KOREA);
    }
    
    public void testLoginKOREA() throws Exception {
        scriptTest("Login", Locale.KOREA);
    }
    
    public void testValidAuthenticatedKOREA() throws Exception {
        scriptTest("ValidAuthenticated", Locale.KOREA);
    }
    
    public void testValidSelectedKOREA() throws Exception {
        scriptTest("ValidSelected", Locale.KOREA);
    }
    
    public void testAuthenticateKOREA() throws Exception {
        scriptTest("Authenticate", Locale.KOREA);
    }
}
