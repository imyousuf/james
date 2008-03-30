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

package org.apache.james.imapserver.codec.encode.imap4rev1;

import java.io.IOException;
import java.util.Collection;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.display.HumanReadableTextKey;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponse;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponse.ResponseCode;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponse.Type;
import org.apache.james.imapserver.codec.encode.ImapEncoder;
import org.apache.james.imapserver.codec.encode.ImapResponseComposer;
import org.apache.james.imapserver.codec.encode.base.AbstractChainedImapEncoder;

public class StatusResponseEncoder extends AbstractChainedImapEncoder {

    public StatusResponseEncoder(ImapEncoder next) {
        super(next);
    }

    protected void doEncode(ImapMessage acceptableMessage,
            ImapResponseComposer composer) throws IOException {
        StatusResponse response = (StatusResponse) acceptableMessage;
        final Type serverResponseType = response.getServerResponseType();
        final String type = asString(serverResponseType);
        final ResponseCode responseCode = response.getResponseCode();
        final String code = asString(responseCode);
        final String tag = response.getTag();
        final ImapCommand command = response.getCommand();
        final HumanReadableTextKey textKey = response.getTextKey();
        final String text = asString(textKey);
        final Collection parameters;
        final long number;
        if (responseCode == null) {
            parameters = null;
            number = 0;
        } else { 
            parameters = responseCode.getParameters();
            number = responseCode.getNumber();
        }
        composer.statusResponse(tag, command, type, code, parameters, number, text);
    }

    private String asString(HumanReadableTextKey text)
    {
        final String result;
        if (text == null) {
            result = null;
        } else {
            result = text.getDefaultValue();
        }
        return result;
    }
    
    private String asString(StatusResponse.ResponseCode code)
    {
        final String result;
        if (code == null) {
            result = null;
        } else {
            result = code.getCode();
        }
        return result;
    }
    
    private String asString(StatusResponse.Type type)
    {
        final String result;
        if (type == null) {
            result = null;
        } else {
            result = type.getCode();
        }
        return result;
    }
    
    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof StatusResponse);
    }

}
