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

package org.apache.james.mailboxmanager.torque;

import junit.framework.TestCase;

public class MessageUtilsNormalisedWriteTo extends TestCase {

    StringBuffer buffer;
    
    protected void setUp() throws Exception {
        super.setUp();
        buffer = new StringBuffer();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testEmpty() throws Exception {
        MessageUtils.normalisedWriteTo("".getBytes(), buffer);
        assertEquals("Check processing of empty array", "", buffer.toString());
    }
    
    public void testNormal() throws Exception {
        MessageUtils.normalisedWriteTo("One\r\nTwo\r\nThree\r\n".getBytes(), buffer);
        assertEquals("Check processing of normal data", "One\r\nTwo\r\nThree\r\n", buffer.toString());
    }
    
    public void testMissing() throws Exception {
        MessageUtils.normalisedWriteTo("One\rTwo\nThree\r\n".getBytes(), buffer);        
        assertEquals("Check processing simple data containing unnormal lines", "One\r\nTwo\r\nThree\r\n", 
                buffer.toString());
    }
    
    public void testCRAtEnd() throws Exception {
        MessageUtils.normalisedWriteTo("One\r\nTwo\r\nThree\r".getBytes(), buffer);        
        assertEquals("CR at end", "One\r\nTwo\r\nThree\r\n", 
                buffer.toString());
    }
    
    
    public void testLFAtEnd() throws Exception {
        MessageUtils.normalisedWriteTo("One\r\nTwo\r\nThree\n".getBytes(), buffer);        
        assertEquals("LF at end", "One\r\nTwo\r\nThree\r\n", 
                buffer.toString());
    }
    
    
    public void testCRAtStart() throws Exception {
        MessageUtils.normalisedWriteTo("\rOne\r\nTwo\r\nThree\r".getBytes(), buffer);        
        assertEquals("CR at start", "\r\nOne\r\nTwo\r\nThree\r\n", 
                buffer.toString());
    }
    
    
    public void testLFAtStart() throws Exception {
        MessageUtils.normalisedWriteTo("\nOne\r\nTwo\r\nThree".getBytes(), buffer);        
        assertEquals("CR at start", "\r\nOne\r\nTwo\r\nThree", 
                buffer.toString());
    }
    
    public void testSwitchOrder() throws Exception {
        MessageUtils.normalisedWriteTo("\n\rOne\n\rTwo\n\rThree\n\r".getBytes(), buffer);        
        assertEquals("Check processing simple data containing unnormal lines", "\r\n\r\nOne\r\n\r\nTwo\r\n\r\nThree\r\n\r\n", 
                buffer.toString());
    }
}