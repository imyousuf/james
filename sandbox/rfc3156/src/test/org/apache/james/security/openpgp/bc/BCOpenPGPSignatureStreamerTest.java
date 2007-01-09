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

package org.apache.james.security.openpgp.bc;

import java.io.ByteArrayOutputStream;
import java.io.File;

import junit.framework.TestCase;

import org.bouncycastle.openpgp.PGPSignature;

/**
 * There are issues testing OpenPGP cryptographic. Various
 * additional components are included together with the
 * document in the hash. This includes a timestamp for the
 * signature. This means that simple string comparisions
 * cannot be used.
 *
 */
public class BCOpenPGPSignatureStreamerTest extends TestCase {

    
    BCOpenPGPSignatureStreamer streamer;

    protected void setUp() throws Exception {
        super.setUp();
        
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testWriteOpenPGPContent() throws Exception {
        final String document = "src/test/org/apache/james/security/openpgp/bc/message.txt";
        PGPSignature signature = BCTestHelper.createSignature(document);
        streamer = new BCOpenPGPSignatureStreamer(signature);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        streamer.writeOpenPGPContent(out);
        String asc = out.toString("ASCII");
        
        boolean success = BCTestHelper.isValidSignature(asc, new File(document));
        assertTrue("Expected signature to be verified by GNU PG", success);
    }

}
