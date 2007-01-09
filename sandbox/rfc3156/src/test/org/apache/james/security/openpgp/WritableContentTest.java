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

package org.apache.james.security.openpgp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.mail.internet.MimeBodyPart;


import junit.framework.TestCase;

public class WritableContentTest extends TestCase {
    
    private static final String CONTENT = 
            "Content-Type: text/plain; charset=us-ascii\r\n" +
            "Far from the madding Crowd's ignoble Strife,\r\n" +
            "Their sober Wishes never learn'd to stray;\r\n" +
            "Along the cool sequester'd Vale of Life\r\n" +
            "They ket the noiseless Tenor of their Way.\r\n" +
            "Thomas Grey, 1716-1771\r\n\r\n"; // Note: Adopting OpenGPG convention 
                                              // ensuring JavaMail does not insert
                                              // extra CRLF
    private static final byte[] CONTENT_BYTES = OpenPGPUtils.toBytes(CONTENT);
    
    OpenPGPMIMEGenerator generator;
    MockOpenPGPSignatureGenerator signatureGenerator;
    
    protected void setUp() throws Exception {
        super.setUp();
        signatureGenerator = new MockOpenPGPSignatureGenerator(OpenPGPSignatureType.PGP_MD5);
        generator = new OpenPGPMIMEGenerator(signatureGenerator);
    }

    public void testWriteContent() throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream(CONTENT_BYTES);
        MimeBodyPart content = new MimeBodyPart(in);
        generator.generateSignedMessage(content);
        Writable writable = signatureGenerator.content;
        assertNotNull("Writable content should be set on generator", writable);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writable.write(out);
        byte[] output = out.toByteArray();
        String contentAsString = OpenPGPUtils.toString(output);
        assertEquals("Content set by generator", CONTENT, contentAsString);
    }
}
