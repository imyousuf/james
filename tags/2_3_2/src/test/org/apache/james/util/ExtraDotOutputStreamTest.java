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
package org.apache.james.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import junit.framework.TestCase;

/**
 * Tests for the ExtraDotOutputStream
 */
public class ExtraDotOutputStreamTest extends TestCase {

    public void testMain() throws IOException {
        String data = ".This is a test\r\nof the thing.\r\nWe should not have much trouble.\r\n.doubled?\r\nor not?\n.doubled\nor not?\r\n\r\n\n\n\r\r\r\n";
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        OutputStream os = new ExtraDotOutputStream(bOut);
        os.write(data.getBytes());
        os.flush();
        String expected = "..This is a test\r\nof the thing.\r\nWe should not have much trouble.\r\n..doubled?\r\nor not?\r\n..doubled\r\nor not?\r\n\r\n\r\n\r\n\r\n\r\n\r\n";
        assertEquals(expected,bOut.toString());
    }

    /*
     * Test method for 'org.apache.james.util.ExtraDotOutputStream.checkCRLFTerminator()'
     */
    public void testCheckCRLFTerminator() throws IOException {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        ExtraDotOutputStream os = new ExtraDotOutputStream(bOut);
        // if the stream is empty then we should not add the CRLF.
        os.checkCRLFTerminator();
        os.flush();
        assertEquals("",bOut.toString());
        os.write("Test".getBytes());
        os.flush();
        assertEquals("Test",bOut.toString());
        os.checkCRLFTerminator();
        os.flush();
        assertEquals("Test\r\n",bOut.toString());
        // if the stream ends with \r we should simply add the \n
        os.write("A line with incomplete ending\r".getBytes());
        os.flush();
        assertEquals("Test\r\nA line with incomplete ending\r",bOut.toString());
        os.checkCRLFTerminator();
        os.flush();
        assertEquals("Test\r\nA line with incomplete ending\r\n",bOut.toString());
        // already correctly terminated, should leave the output untouched
        os.checkCRLFTerminator();
        os.flush();
        assertEquals("Test\r\nA line with incomplete ending\r\n",bOut.toString());
    }

}
