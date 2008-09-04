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




package org.apache.james.smtpserver.junkscore;

import org.apache.james.smtpserver.junkscore.JunkScoreConfigUtil;

import junit.framework.TestCase;

public class JunkScoreConfigUtilTest extends TestCase {

    private final static String INVALID_CONFIG1 = "junkscore: invalid";
    private final static String INVALID_CONFIG2 = "junk: 21";
    private final static String VALID_CONFIG = "junkscore: 21";
    
    public void testgetJunkScoreConfig() {
        boolean exception1 = false;
        boolean exception2 = false;
    
        try {
            JunkScoreConfigUtil.getJunkScore(INVALID_CONFIG1);
        } catch (IllegalArgumentException e) {
            exception1 = true;
        }
    
        assertTrue("Exception thrown", exception1);
    
        try {
            JunkScoreConfigUtil.getJunkScore(INVALID_CONFIG2);
        } catch (IllegalArgumentException e) {
            exception2 = true;
        }
    
        assertTrue("Exception thrown", exception2);
        
        assertEquals("JunkScore extracted", JunkScoreConfigUtil.getJunkScore(VALID_CONFIG),21.0,0d);
    
    }
    
    public void testIsValidJunkScoreConfig() {
        assertFalse("Invalid Config", JunkScoreConfigUtil.isValidJunkScoreConfig(INVALID_CONFIG1));
        assertFalse("Invalid Config", JunkScoreConfigUtil.isValidJunkScoreConfig(INVALID_CONFIG2));
        assertTrue("Valid Config", JunkScoreConfigUtil.isValidJunkScoreConfig(VALID_CONFIG));
    }
}
