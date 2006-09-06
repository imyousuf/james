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


package org.apache.james.security;

import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * <p>Loads a {@link java.security.KeyStore} in memory and keeps it ready for the
 * cryptographic activity.</p>
 * <p>It has the role of being a simpler intermediate to the crypto libraries.
 * Uses specifically the <a href="http://www.bouncycastle.org/">Legion of the Bouncy Castle</a>
 * libraries, particularly for the SMIME activity.</p>
 * @version CVS $Revision:$ $Date:$
 * @since 3.0
 */
public interface KeyHolder {
    
    /**
     * Generates a signed MimeMultipart from a MimeMessage.
     * @param message The message to sign.
     * @return The signed <CODE>MimeMultipart</CODE>.
     */    
    public MimeMultipart generate(MimeMessage message) throws Exception;

    /**
     * Generates a signed MimeMultipart from a MimeBodyPart.
     * @param content The content to sign.
     * @return The signed <CODE>MimeMultipart</CODE>.
     */    
    public MimeMultipart generate(MimeBodyPart content) throws Exception;

    /**
     * Getter for property signerDistinguishedName.
     * @return Value of property signerDistinguishedName.
     */
    public String getSignerDistinguishedName();
    
    /**
     * Getter for property signerCN.
     * @return Value of property signerCN.
     */
    public String getSignerCN();
    
     /**
     * Getter for property signerAddress.
     * @return Value of property signerMailAddress.
     */
    public String getSignerAddress();
    
}
