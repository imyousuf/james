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

package org.apache.james.imapserver.codec.decode;

import javax.mail.Flags;

import junit.framework.TestCase;

public class DecoderUtilsTest extends TestCase {
    
    private static final String EXTENSION_FLAG = "\\Extension";
    private static final String A_CUSTOM_FLAG = "Another";
    private static final String FLAG_MESSAGE = "RFC3501 specifies that \\Recent flag cannot be set by the client but accept liberally for better compatibility.";

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testSetRecentFlag() throws Exception {
        Flags flags = new Flags();
        DecoderUtils.setFlag("\\Recent", flags);
        assertFalse(FLAG_MESSAGE, flags.contains("\\Recent"));
        assertFalse(FLAG_MESSAGE, flags.contains(Flags.Flag.RECENT));
    }

    public void testSetOtherFlag() throws Exception {
        Flags flags = new Flags();
        DecoderUtils.setFlag(A_CUSTOM_FLAG, flags);
        assertTrue("Unknown flags should be added", flags.contains(A_CUSTOM_FLAG));
    }
    
    public void testExtensionFlag() throws Exception {
        Flags flags = new Flags();
        DecoderUtils.setFlag(EXTENSION_FLAG, flags);
        assertTrue("Extension flags should be added", flags.contains(EXTENSION_FLAG));
    }
}
