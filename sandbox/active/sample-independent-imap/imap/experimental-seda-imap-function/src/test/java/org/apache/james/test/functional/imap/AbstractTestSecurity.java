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

public abstract class AbstractTestSecurity extends AbstractSimpleScriptedTestProtocol {

    public AbstractTestSecurity(HostSystem system) {
        super(system);
    }

    public void testLoginThreeStrikesUS() throws Exception {
        scriptTest("LoginThreeStrikes", Locale.US);
    }
    
    public void testLoginThreeStrikesKOREA() throws Exception {
        scriptTest("LoginThreeStrikes", Locale.KOREA);
    }
    
    public void testLoginThreeStrikesITALY() throws Exception {
        scriptTest("LoginThreeStrikes", Locale.ITALY);
    }
    
    public void testBadTagUS() throws Exception {
        scriptTest("BadTag", Locale.US);
    }
    
    public void testBadTagKOREA() throws Exception {
        scriptTest("BadTag", Locale.KOREA);
    }
    
    public void testBadTagITALY() throws Exception {
        scriptTest("BadTag", Locale.ITALY);
    }
    
    public void testNoTagUS() throws Exception {
        scriptTest("NoTag", Locale.US);
    }
    
    public void testNoTagKOREA() throws Exception {
        scriptTest("NoTag", Locale.KOREA);
    }
    
    public void testNoTagITALY() throws Exception {
        scriptTest("NoTag", Locale.ITALY);
    }
    
    public void testIllegalTagUS() throws Exception {
        scriptTest("IllegalTag", Locale.US);
    }
    
    public void testIllegalTagKOREA() throws Exception {
        scriptTest("IllegalTag", Locale.KOREA);
    }
    
    public void testIllegalTagITALY() throws Exception {
        scriptTest("IllegalTag", Locale.ITALY);
    }
    
    public void testJustTagUS() throws Exception {
        scriptTest("JustTag", Locale.US);
    }
    
    public void testJustTagKOREA() throws Exception {
        scriptTest("JustTag", Locale.KOREA);
    }
    
    public void testJustTagITALY() throws Exception {
        scriptTest("JustTag", Locale.ITALY);
    }
    
    public void testNoCommandUS() throws Exception {
        scriptTest("NoCommand", Locale.US);
    }
    
    public void testNoCommandKOREA() throws Exception {
        scriptTest("NoCommand", Locale.KOREA);
    }
    
    public void testNoCommandITALY() throws Exception {
        scriptTest("NoCommand", Locale.ITALY);
    }
    
    public void testBogusCommandUS() throws Exception {
        scriptTest("BogusCommand", Locale.US);
    }
    
    public void testBogusCommandKOREA() throws Exception {
        scriptTest("BogusCommand", Locale.KOREA);
    }
    
    public void testNoBogusITALY() throws Exception {
        scriptTest("BogusCommand", Locale.ITALY);
    }
}
