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

/**
 * <p>
 * Constants specified in <code>RFC 3156 OpenPGP/MIME</code>
 * </p>
 * 
 */
public final class OpenPGPMIMEConstants {

    /**
     * MIME type for RFC-3156 OpenPGP/MIME signature content.
     */
    public static final String MIME_TYPE_OPENPGP_SIGNATURE = "application/pgp-signature";
    
    /**
     * Content type for RFC-3156 OpenPGP/MIME signed mail messages;
     */
    public static final String SIGNED_MESSAGE_CONTENT_TYPE = "multipart/signed";

    /**
     * RFC-3156 OpenPGP/MIME  signature protocol for content type.
     */
    public static final String OPENPGP_PROTOCOL_TYPE = "application/pgp-signature";
}
