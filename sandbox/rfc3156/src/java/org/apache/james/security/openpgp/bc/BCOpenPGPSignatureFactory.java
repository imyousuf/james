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

import java.security.NoSuchProviderException;

import org.apache.james.security.openpgp.OpenPGPSignatureFactory;
import org.apache.james.security.openpgp.OpenPGPSignatureType;
import org.apache.james.security.openpgp.OpenPGPStreamer;
import org.apache.james.security.openpgp.Writable;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPUtil;

/**
 * Creates OpenPGP/MIME signatures using Bouncy Castle.
 */
public class BCOpenPGPSignatureFactory implements OpenPGPSignatureFactory {

    private static final String PROVIDER_ID = "BC";

    private final PGPPrivateKey signingKey;
    private final OpenPGPSignatureType signatureType;
    private final int hashAlgorithm;
    private final int keyAlgorithm;
    private final int pgpSignatureType;
    
    public BCOpenPGPSignatureFactory(final PGPSecretKey signingKey, 
            final OpenPGPSignatureType signatureType, char[] passPhrase, boolean useTextSignatureType) throws NoSuchProviderException, PGPException {
        super();
        this.signingKey = signingKey.extractPrivateKey(passPhrase, PROVIDER_ID);
        this.signatureType = signatureType;
        this.hashAlgorithm = toBCHashAlgorithm(signatureType);
        this.keyAlgorithm = signingKey.getPublicKey().getAlgorithm();
        this.pgpSignatureType = getOpenPGPSignatureType(useTextSignatureType);
    }
    
    private int toBCHashAlgorithm(OpenPGPSignatureType signatureType) throws PGPException {
        int result = -1;
        if (OpenPGPSignatureType.PGP_HAVEL_5_160.equals(signatureType)) {
            result = PGPUtil.HAVAL_5_160;
        } else if (OpenPGPSignatureType.PGP_MD2.equals(signatureType)) {
            result = PGPUtil.MD2;
        } else if (OpenPGPSignatureType.PGP_MD5.equals(signatureType)) {
            result = PGPUtil.MD5;
        } else if (OpenPGPSignatureType.PGP_SHA1.equals(signatureType)) {
            result = PGPUtil.SHA1;
        } else if (OpenPGPSignatureType.PGP_TIGER_192.equals(signatureType)) {
            result = PGPUtil.TIGER_192;
        } else if (OpenPGPSignatureType.PGP_RIPE_MD_160.equals(signatureType)) {
            result = PGPUtil.RIPEMD160;
        }
        if (result == -1) {
            throw new PGPException("Unsupported hash algorithm");
        }
        return result;
    }

    private int getOpenPGPSignatureType(boolean useTextSignatureType) {
        int result = PGPSignature.BINARY_DOCUMENT;
        if (useTextSignatureType) {
            result = PGPSignature.CANONICAL_TEXT_DOCUMENT;
        }
        return result;
    }
    
    public OpenPGPStreamer createSignatureStreamer(final Writable content)
            throws Exception {
        final PGPSignatureGenerator generator 
            = new PGPSignatureGenerator(keyAlgorithm, hashAlgorithm, PROVIDER_ID);
        generator.initSign(pgpSignatureType, signingKey);
        final GeneratorOutputStream stream = new GeneratorOutputStream(generator);
        content.write(stream);
        stream.close();
        final PGPSignature signature = generator.generate();
        final BCOpenPGPSignatureStreamer result = new BCOpenPGPSignatureStreamer(signature);
        return result;
    }

    public OpenPGPSignatureType getSignatureType() {
        return signatureType;
    }

    public int getBCKeyAlgorithm() {
        return hashAlgorithm;
    }
}
