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

package org.apache.james.util.mail.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.activation.ActivationDataFlavor;

import junit.framework.TestCase;

import org.apache.james.security.openpgp.MockOpenPGPStreamer;
import org.apache.james.security.openpgp.OpenPGPMIMEConstants;

public class pgp_signatureTest extends TestCase {

    pgp_signature datahandler;

    protected void setUp() throws Exception {
        super.setUp();
        datahandler = new pgp_signature();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testComputeDataFlavor() {
        ActivationDataFlavor flavour = datahandler.getDataFlavor();
        assertNotNull(flavour);
        assertEquals("MIME type as per RFC3156", "application/pgp-signature", flavour.getMimeType());
    }

    public void testWriteToStreamer() throws Exception {
        MockOpenPGPStreamer streamer = new MockOpenPGPStreamer();
        OutputStream out = new ByteArrayOutputStream();
        datahandler.writeTo(streamer, OpenPGPMIMEConstants.MIME_TYPE_OPENPGP_SIGNATURE, out);
        assertEquals("Only call once", 1, streamer.streams.size());
        assertEquals("All processing delegated. Call with raw stream.", out, streamer.streams.get(0));
    }

    public void testWriteToOther() throws Exception {
        OutputStream out = new ByteArrayOutputStream();
        try {
            datahandler.writeTo("Bogus", OpenPGPMIMEConstants.MIME_TYPE_OPENPGP_SIGNATURE, out);
            fail("Exception should be thrown when the object cannot be handled.");
        } catch (IOException e) {
            // expected
        }
    }

    public void testWriteToMIME() throws Exception {
        OutputStream out = new ByteArrayOutputStream();
        try {
            MockOpenPGPStreamer streamer = new MockOpenPGPStreamer();
            datahandler.writeTo(streamer, "application/pgp", out);
            fail("Exception should be thrown when the MIME is incorrect");
        } catch (IOException e) {
            // expected
        }
    }
}
