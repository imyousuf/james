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

import org.apache.james.security.openpgp.OpenPGPSignatureType;
import org.apache.james.security.openpgp.OpenPGPStreamer;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPUtil;

import junit.framework.TestCase;

public class BCOpenPGPSignatureFactoryTest extends TestCase {

    OpenPGPSignatureType signatureType = OpenPGPSignatureType.PGP_SHA1;
    BCOpenPGPSignatureFactory factory;
    
    protected void setUp() throws Exception {
        super.setUp();
        factory = new BCOpenPGPSignatureFactory(
                BCTestHelper.loadStandardPGPSecretKey(), signatureType, 
                BCTestHelper.password(), false);    
        // Note since data is NOT CANONICAL must use binary algorithm
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCreateSignatureStreamer() throws Exception {
        final String document = "src/test/org/apache/james/security/openpgp/bc/message.txt";     
        OpenPGPStreamer streamer = factory.createSignatureStreamer(new WritableFileContent(document));
        assertNotNull("Factory should either return a streamer or throw an exception", 
                streamer);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        streamer.writeOpenPGPContent(out);
        String asc = out.toString("ASCII");
        
        boolean success = BCTestHelper.isValidSignature(asc, new File(document));
        assertTrue("Expected signature to be verified by GNU PG", success);
    }

    public void testGetSignatureType() {
        assertEquals("Signature is set at construction", signatureType, 
                factory.getSignatureType());
    }

    public void testSHA1HashAlgorithm() throws Exception {
        factory = new BCOpenPGPSignatureFactory(
                BCTestHelper.loadStandardPGPSecretKey(), OpenPGPSignatureType.PGP_SHA1, 
                BCTestHelper.password(), false);
        assertEquals("BC hash algorithm is derived from signature type", 
                PGPUtil.SHA1, factory.getBCKeyAlgorithm());
    }
    
    public void testRipeHashAlgorithm() throws Exception {
        factory = new BCOpenPGPSignatureFactory(
                BCTestHelper.loadStandardPGPSecretKey(), OpenPGPSignatureType.PGP_RIPE_MD_160, 
                BCTestHelper.password(), false);
        assertEquals("BC hash algorithm is derived from signature type", 
                PGPUtil.RIPEMD160, factory.getBCKeyAlgorithm());
    }
    
    public void testHavelHashAlgorithm() throws Exception {
        factory = new BCOpenPGPSignatureFactory(
                BCTestHelper.loadStandardPGPSecretKey(), OpenPGPSignatureType.PGP_HAVEL_5_160, 
                BCTestHelper.password(), false);
        assertEquals("BC hash algorithm is derived from signature type", 
                PGPUtil.HAVAL_5_160, factory.getBCKeyAlgorithm());
    }
    
    public void testTiger192HashAlgorithm() throws Exception {
        factory = new BCOpenPGPSignatureFactory(
                BCTestHelper.loadStandardPGPSecretKey(), OpenPGPSignatureType.PGP_TIGER_192, 
                BCTestHelper.password(), false);
        assertEquals("BC hash algorithm is derived from signature type", 
                PGPUtil.TIGER_192, factory.getBCKeyAlgorithm());
    }
    
    public void testMD2HashAlgorithm() throws Exception {
        factory = new BCOpenPGPSignatureFactory(
                BCTestHelper.loadStandardPGPSecretKey(), OpenPGPSignatureType.PGP_MD2, 
                BCTestHelper.password(), false);
        assertEquals("BC hash algorithm is derived from signature type", 
                PGPUtil.MD2, factory.getBCKeyAlgorithm());
    }
    
    public void testMD5HashAlgorithm() throws Exception {
        factory = new BCOpenPGPSignatureFactory(
                BCTestHelper.loadStandardPGPSecretKey(), OpenPGPSignatureType.PGP_MD5, 
                BCTestHelper.password(), false);
        assertEquals("BC hash algorithm is derived from signature type", 
                PGPUtil.MD5, factory.getBCKeyAlgorithm());
    }
}
