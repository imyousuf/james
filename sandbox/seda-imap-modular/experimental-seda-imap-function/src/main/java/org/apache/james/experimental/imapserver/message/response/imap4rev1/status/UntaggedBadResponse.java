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

package org.apache.james.experimental.imapserver.message.response.imap4rev1.status;

import org.apache.james.experimental.imapserver.message.response.base.AbstractUntaggedStatusResponse;

/**
 * <p>Indicates an error message from the server.
 * This may be:</p>
 * <ul>
 * <li>A protocol-level error for which the associated
 * command cannot be determined</li>
 * <li>An internal server failure</li>
 * </ul>
 */
public class UntaggedBadResponse extends AbstractUntaggedStatusResponse {

    public UntaggedBadResponse(String text, String code) {
        super(text, code);
    }

}
