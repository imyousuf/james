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

import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.james.security.KeyHolder;

/**
 * <p>Holds a OpenPGP private key used to sign email.</p>
 * <p><strong>Note</strong> that this class is an implementation of 
 * <code>RFC3156 - OpenPGP/MIME</code> only. Older PGP forms are
 * not supported.
 * </p>
 * <p>
 * </p>
 */
public class OpenPGPMIMEKeyHolder extends OpenPGPMIMEGenerator implements KeyHolder {

    public OpenPGPMIMEKeyHolder(OpenPGPSignatureFactory signatureGenerator) {
        super(signatureGenerator);
    }

    /**
     * Gets the signer address as requested.
     * An OpenPGP key may be associated with more than one
     * email address. 
     * @return an email address associated with the key
     */
    public String getSignerAddress() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Not available.
     * @return null
     */
    public String getSignerCN() {
        return null;
    }

    /**
     * Not available.
     * @return null
     */
    public String getSignerDistinguishedName() {
        return null;
    }
}
