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

import java.io.IOException;
import java.io.OutputStream;

import javax.activation.ActivationDataFlavor;
import javax.activation.DataSource;
import javax.mail.MessagingException;

import org.apache.james.security.openpgp.OpenPGPMIMEConstants;
import org.apache.james.security.openpgp.OpenPGPStreamer;

/**
 * <p>Handles OpenPGP/MIME signatures.</p>
 * <p>
 * Content for signatures should implement {@link OpenPGPStreamer}.
 * </p>
 * @see OpenPGPStreamer
 */
public class pgp_signature extends AbstractDataContentHandler {

    private static final String DISPLAY_NAME = "RFC 3156 OpenPGP/MIME Signature";

    protected Object computeContent(DataSource aDataSource)
    throws MessagingException {
        // TODO perhaps return a wrapper object implementing an 
        // org.apache.james.security.openpgp interface suitable for signing
        return null;
    }

    /**
     * Computes the data flavor.
     * @return <code>application/pgp-signature</code?
     */
    protected ActivationDataFlavor computeDataFlavor() {
        final ActivationDataFlavor result 
        = new ActivationDataFlavor(pgp_signature.class, 
                OpenPGPMIMEConstants.MIME_TYPE_OPENPGP_SIGNATURE, DISPLAY_NAME);
        return result;
    }

    public void writeTo(Object part, String mimeType, OutputStream out)
    throws IOException {
        if (OpenPGPMIMEConstants.MIME_TYPE_OPENPGP_SIGNATURE.equals(mimeType))
        {
            if (part instanceof OpenPGPStreamer) {
                OpenPGPStreamer streamer = (OpenPGPStreamer) part;
                streamer.writeOpenPGPContent(out);
            } else {
                throw new IOException("Type \"" + part.getClass().getName()
                        + "\" is not supported.");
            }
        }
        else {
            throw new IOException("MIME type \"" + mimeType + "\" is not supported.");
        }
    }

}
