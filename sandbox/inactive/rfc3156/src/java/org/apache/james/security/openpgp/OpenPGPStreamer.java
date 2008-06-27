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

import java.io.IOException;
import java.io.OutputStream;

/**
 * <p>Streams out OpenPGP/MIME content.</p>
 * <p>
 * <strong>Usage:</strong> content for body parts.
 * An appropriate <code>DataContentHandler</code> should be 
 * registered for this type.
 * </p>
 */
public interface OpenPGPStreamer {

    /**
     * Writes out content to the given stream.
     * Checked exceptions specific to the 
     * crytographic operations should be handled or adapted by
     * the implementation.
     * @param out <code>OutputStream</code>, not null
     * @throws IOException throw following an IO failure
     * or an irrecoverable cryptographic issue
     */
    void writeOpenPGPContent(OutputStream out) throws IOException;
}
