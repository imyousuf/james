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

package org.apache.james.imap.message.response.imap4rev1.legacy;

import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.message.response.ImapResponseMessage;

/**
 * Carries the response to a request with bad syntax..
 * @deprecated responses should correspond directly to the specification
 */
public class BadResponse implements ImapMessage,
        ImapResponseMessage {

    private final String message;
    private final String tag;
    
    public BadResponse(final String message) {
    	// TODO: check calls that use this are specficiation compliant
    	this(message, null);
    }
    
    public BadResponse(final String message, final String tag) {
        super();
        this.message = message;
        this.tag = tag;
    }

    public final String getMessage() {
        return message;
    }

    public final String getTag() {
        return tag;
    }
}
