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

package org.apache.james.mailboxmanager.util;

import junit.framework.TestCase;

public class UidToKeyConverterTest extends TestCase {
    
    UidToKeyConverterImpl uidToKeyConverterImpl;
    
    long uidValidity=123;
    
    public void setUp(){
        uidToKeyConverterImpl=new UidToKeyConverterImpl();
        uidToKeyConverterImpl.setUidValidity(uidValidity);
    }

    
    public void testUidToKey() {
        assertEquals("JAMES-UID-KEY-(345;123)",uidToKeyConverterImpl.toKey(345));
    }
    
    public void testKeyToUidValid() {
        assertEquals(new Long(3454),uidToKeyConverterImpl.toUid("JAMES-UID-KEY-(3454;123)"));
        uidToKeyConverterImpl.setUidValidity(1);
        assertEquals(new Long(2),uidToKeyConverterImpl.toUid("JAMES-UID-KEY-(2;1)"));
    }
    
    public void testKeyToUidWrongUidValidity() {
        assertNull(uidToKeyConverterImpl.toUid("JAMES-UID-KEY-(3454;1234)"));
    }
    
    public void testKeyToUidWrongFormat() {
        assertNull(uidToKeyConverterImpl.toUid(null));
        assertNull(uidToKeyConverterImpl.toUid(""));
        assertNull(uidToKeyConverterImpl.toUid("JMES-UID-KEY-("));
        assertNull(uidToKeyConverterImpl.toUid("JAMES-UID-KEY-(;123)"));
        assertNull(uidToKeyConverterImpl.toUid("JAMES-UID-KEY-(3454;)"));
        assertNull(uidToKeyConverterImpl.toUid("JAMES-UID-KEY-(3454;123"));
        assertNull(uidToKeyConverterImpl.toUid("JAMES-UID-KEY-(345d4;123)"));
        assertNull(uidToKeyConverterImpl.toUid("JAMES-UID-KEY-(34;54;123)"));
        
    }
    
    
}
