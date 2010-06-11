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

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

import javax.mail.BodyPart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.apache.james.security.openpgp.bc.BCTestHelper;

import junit.framework.TestCase;

public class OpenPGPMIMEGeneratorTest extends TestCase {

    OpenPGPMIMEGenerator generator;
    MockOpenPGPSignatureGenerator signatureGenerator;
    
    protected void setUp() throws Exception {
        super.setUp();
        signatureGenerator = new MockOpenPGPSignatureGenerator(OpenPGPSignatureType.PGP_MD5);
        generator = new OpenPGPMIMEGenerator(signatureGenerator);
    }



    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    private MimeBodyPart createSimpleContent() throws Exception {
        final MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.attachFile(new File("src/test/org/apache/james/security/openpgp/simple-message.txt"));
        return mimeBodyPart;
    }

    public void testRFC3156RequiredHeadersMessage() throws Exception {
        MimeBodyPart content = createSimpleContent();
        MimeMultipart message = generator.generateSignedMessage(content);
        assertNotNull("Multipart message should be generated", message);
        String[] contentTypeParts = message.getContentType().split(";");
        for (int i=0;i<contentTypeParts.length;i++) {
            contentTypeParts[i] = contentTypeParts[i].trim();
        }
        Arrays.sort(contentTypeParts);
        assertEquals("Expect four required parts", 4, contentTypeParts.length);
        assertEquals("Content type is multipart/signed", "multipart/signed",contentTypeParts[2]);
        assertEquals("Message Integriry Check description is present and correct", 
                "micalg=pgp-md5",contentTypeParts[1]);        
        assertEquals("protocol is present and correct", "protocol=\"application/pgp-signature\"",contentTypeParts[3]);
    }
    
    public void testContentPartBasics() throws Exception {
        MimeBodyPart content = createSimpleContent();
        MimeMultipart message = generator.generateSignedMessage(content);
        assertNotNull("Multipart message should be generated", message);
        assertEquals("Expect signature and content parts", 2, message.getCount());
        BodyPart contentPart = message.getBodyPart(0);
        assertTrue("Content should be MIME encoded", contentPart instanceof MimeBodyPart);
        MimeBodyPart mimeContent = (MimeBodyPart) contentPart;
        assertEquals("Same as the contents of the file", 
                "This is a simple message.", mimeContent.getContent());
        assertEquals("text/plain",mimeContent.getContentType());
    }
     

    public void testSignaturePartHeaders() throws Exception {
        MimeBodyPart content = createSimpleContent();
        MimeMultipart message = generator.generateSignedMessage(content);
        assertNotNull("Multipart message should be generated", message);
        assertEquals("Expect signature and content parts", 2, message.getCount());
        BodyPart contentPart = message.getBodyPart(1);
        assertTrue("Content should be MIME encoded", contentPart instanceof MimeBodyPart);
        MimeBodyPart mimeContent = (MimeBodyPart) contentPart;
        assertEquals("Content type present and correct", 
                "application/pgp-signature; name=" + OpenPGPMIMEGenerator.DEFAULT_SIGNATURE_FILE_NAME, 
                mimeContent.getContentType());
        assertEquals("Content is streamer since no mailcap registered", 
                signatureGenerator.streamer, mimeContent.getContent());
    }
}
