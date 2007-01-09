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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.ParseException;

/**
 * <p>Generates <code>RFC3156 OpenPGP/MIME</code> signatures.</p>
 * <p>
 * Contains a general OpenPGP/MIME implementation decoupled
 * from both cryptographic implementation and mail server.
 * </p>
 */
public class OpenPGPMIMEGenerator {

    private static final String OPENPGP_SIGNED_MESSAGE_CONTENT_TYPE 
        = "signed;  protocol=\"" 
        + OpenPGPMIMEConstants.OPENPGP_PROTOCOL_TYPE + "\"; micalg=";
    
    private static final String SIGNATURE_PART_CONTENT_TYPE 
        = OpenPGPMIMEConstants.MIME_TYPE_OPENPGP_SIGNATURE + "; name=";
    
    public static final String DEFAULT_SIGNATURE_FILE_NAME = "signature.asc";
    
    private final OpenPGPSignatureFactory signatureGenerator;
    private final String contentType;
    private final String signatureContentType;
    
    public OpenPGPMIMEGenerator(final OpenPGPSignatureFactory signatureGenerator) {
        this(signatureGenerator, DEFAULT_SIGNATURE_FILE_NAME);
    }
    
    /**
     * Constucts an <code>RFC3156 OpenPGP/MIME</code> generator.
     * @param signatureGenerator <code>OpenPGPSignatureGenerator</code>, not null
     * @param signatureName the display name to be used for the signature attachment.
     * Used to populate the name field of the signature part content-type
     */
    public OpenPGPMIMEGenerator(final OpenPGPSignatureFactory signatureGenerator, final String signatureName) {
        this.signatureGenerator = signatureGenerator;
        contentType = createContentType();
        this.signatureContentType = SIGNATURE_PART_CONTENT_TYPE + signatureName;
    }

    public MimeMultipart generate(MimeMessage message) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    public MimeMultipart generate(MimeBodyPart content) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Generates a OpenPGP signed message MIME message from the given content.
     * @param content <code>MimeBodyPart</code> content, not null
     * @return <code>MimeMultipart</code> RFC-3156 OpenPGP/MIME format signed message
     * @throws Exception
     */
    public MimeMultipart generateSignedMessage(final MimeBodyPart content) throws Exception {
        final MimeMultipart result = new MimeMultipart(contentType);
        MimeBodyPart canonicalised = canonicalise(content);
        result.addBodyPart(canonicalised);
        final MimeBodyPart signaturePart = createSignaturePart(canonicalised);
        result.addBodyPart(signaturePart);
        return result;
    }

    private MimeBodyPart canonicalise(final MimeBodyPart content) throws Exception {
        MimeBodyPart result = content;
        final String contentTypeString = content.getContentType();
        final ContentType contentType = new ContentType(contentTypeString);
        // OpenPGP/MIME: text should be canonicalised
        if (contentType.match("text/*")) {

        }
        return result;
    }

    private String createContentType() {
        final OpenPGPSignatureType signatureType = signatureGenerator.getSignatureType();
        final String micAlgorithm = signatureType.getMessageIntegrityCheckAlgorithmCode();
        final String contentType = OPENPGP_SIGNED_MESSAGE_CONTENT_TYPE + micAlgorithm;
        return contentType;
    }
    
    /**
     * <p>Creates the signature part of the message from the given content.</p>
     * <p>
     * <strong>Note</strong> that the content should be already processed into
     * the form required OpenPGP/MIME. In particular, it must have been 
     * canonicalised and transfer encoded (if necessary).
     * </p>
     * @param content <code>MimeBodyPart</code> containing the processed content to be signed
     * @return <code>MimeBodyPart</code> encoding the OpenPGP/MIME detached signature
     * @throws Exception
     */
    private MimeBodyPart createSignaturePart(final MimeBodyPart content) throws Exception
    {
        final MimeBodyPart result = new MimeBodyPart();
        final WritableMimeBodyPart writableMimeBodyPart = new WritableMimeBodyPart(content);
        OpenPGPStreamer signatureContent = signatureGenerator.createSignatureStreamer(writableMimeBodyPart);
        result.setContent(signatureContent, OpenPGPMIMEConstants.MIME_TYPE_OPENPGP_SIGNATURE);
        result.addHeader("Content-Type", signatureContentType);
        return result;
    }
}
