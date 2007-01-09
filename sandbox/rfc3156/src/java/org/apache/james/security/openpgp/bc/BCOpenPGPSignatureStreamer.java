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

import java.io.IOException;
import java.io.OutputStream;

import org.apache.james.security.openpgp.OpenPGPStreamer;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.openpgp.PGPSignature;

/**
 * Writes a signature for the content using BouncyCastle.
 *
 */
public class BCOpenPGPSignatureStreamer implements OpenPGPStreamer {

    private final PGPSignature signature;
    
    public BCOpenPGPSignatureStreamer(final PGPSignature signature) {
        this.signature = signature;
    }
    
    public void writeOpenPGPContent(OutputStream out) throws IOException {
        final ArmoredOutputStream armoredOut = new ArmoredOutputStream(out);
        final BCPGOutputStream pgOut = new BCPGOutputStream(armoredOut);
        signature.encode(pgOut);
        pgOut.close();
        armoredOut.close();
    }

}
