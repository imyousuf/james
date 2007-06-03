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

package org.apache.james.experimental.imapserver.message.response.base;

import org.apache.james.experimental.imapserver.message.response.ImapResponseMessage;

abstract public class AbstractUntaggedStatusResponse implements ImapResponseMessage {

    // TODO: this should be an interface to allow i18n
    private final String text;
    // TODO: this should be a interface coded in the encoder
    private final String code;
    
    public AbstractUntaggedStatusResponse(final String text, final String code) {
        super();
        this.text = text;
        this.code = code;
    }

    public String getDisplayText() {
        return text;
    }

    public String getResponseCode() {
        return code;
    }

}
