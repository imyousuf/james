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

package org.apache.james.pop3server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.james.pop3server.core.ExtraDotInputStream;

import junit.framework.TestCase;

public class ExtraDotInputStreamTest extends TestCase {

    public void testExtraDot() throws IOException {
        String data = "This\r\n.\r\nThis.\r\n";
        String expectedOutput = "This\r\n..\r\nThis.\r\n";
        ExtraDotInputStream in = new ExtraDotInputStream(new ByteArrayInputStream(data.getBytes()));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int i = -1;
        while ((i = in.read()) != -1) {
            out.write(i);
        }
        in.close();
        out.close();

        String output = new String(out.toByteArray());
        assertEquals(expectedOutput, output);

    }

    public void testNoDotCLRF() throws IOException {
        String data = "ABCD\r\n";
        ExtraDotInputStream in = new ExtraDotInputStream(new ByteArrayInputStream(data.getBytes()));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int i = -1;
        while ((i = in.read()) != -1) {
            out.write(i);
        }
        in.close();
        out.close();

        String output = new String(out.toByteArray());
        assertEquals(data, output);
    }

    public void testNoDot() throws IOException {
        String data = "ABCD";
        ExtraDotInputStream in = new ExtraDotInputStream(new ByteArrayInputStream(data.getBytes()));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int i = -1;
        while ((i = in.read()) != -1) {
            out.write(i);
        }
        in.close();
        out.close();

        String output = new String(out.toByteArray());
        assertEquals(data, output);
    }

    // Proof of BUG JAMES-1152
    public void testNoDotHeaderBody() throws IOException {
        String data = "Subject: test\r\n\r\nABCD\r\n";
        ExtraDotInputStream in = new ExtraDotInputStream(new ByteArrayInputStream(data.getBytes()));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int i = -1;
        while ((i = in.read()) != -1) {
            out.write(i);
        }
        in.close();
        out.close();

        String output = new String(out.toByteArray());
        assertEquals(data, output);
    }
}
