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

package org.apache.james.imap.message.response.imap4rev1.status;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.display.HumanReadableTextKey;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponse;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponseFactory;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponse.ResponseCode;

public abstract class AbstactStatusResponseFactory implements StatusResponseFactory {

    public AbstactStatusResponseFactory() {
        super();
    }

    protected abstract StatusResponse createResponse(StatusResponse.Type type, String tag, ImapCommand command, HumanReadableTextKey displayTextKey, ResponseCode code);
    
    public StatusResponse bye(HumanReadableTextKey displayTextKey, ResponseCode code) {
        return createResponse(StatusResponse.Type.BYE, null, null, displayTextKey, code);
    }

    public StatusResponse bye(HumanReadableTextKey displayTextKey) {
        return bye(displayTextKey, null);
    }

    public StatusResponse preauth(HumanReadableTextKey displayTextKey, ResponseCode code)  {
        return createResponse(StatusResponse.Type.PREAUTH, null, null, displayTextKey, code);
    }
    
    public StatusResponse preauth(HumanReadableTextKey displayTextKey) {
        return preauth(displayTextKey, null);
    }

    public StatusResponse taggedBad(String tag, ImapCommand command, HumanReadableTextKey displayTextKey, ResponseCode code)  {
        return createResponse(StatusResponse.Type.BAD, tag, command, displayTextKey, code);
    }

    public StatusResponse taggedBad(String tag, ImapCommand command, HumanReadableTextKey displayTextKey) {
        return taggedBad(tag, command, displayTextKey, null);
    }

    public StatusResponse taggedNo(String tag, ImapCommand command, HumanReadableTextKey displayTextKey, ResponseCode code)  {
        return createResponse(StatusResponse.Type.NO, tag, command, displayTextKey, code);
    }

    public StatusResponse taggedNo(String tag, ImapCommand command, HumanReadableTextKey displayTextKey) {
        return taggedNo(tag, command, displayTextKey, null);
    }

    public StatusResponse taggedOk(String tag, ImapCommand command, HumanReadableTextKey displayTextKey, ResponseCode code)  {
        return createResponse(StatusResponse.Type.OK, tag, command, displayTextKey, code);
    }

    public StatusResponse taggedOk(String tag, ImapCommand command, HumanReadableTextKey displayTextKey) {
        return taggedOk(tag, command, displayTextKey, null);
    }

    public StatusResponse untaggedBad(HumanReadableTextKey displayTextKey, ResponseCode code) {
        return taggedBad(null, null, displayTextKey, code);
    }

    public StatusResponse untaggedBad(HumanReadableTextKey displayTextKey) {
        return untaggedBad(displayTextKey, null);
    }

    public StatusResponse untaggedNo(HumanReadableTextKey displayTextKey, ResponseCode code) {
        return taggedNo(null, null, displayTextKey, code);
    }

    public StatusResponse untaggedNo(HumanReadableTextKey displayTextKey) {
        return untaggedNo(displayTextKey, null);
    }

    public StatusResponse untaggedOk(HumanReadableTextKey displayTextKey, ResponseCode code) {
        return taggedOk(null, null, displayTextKey, code);
    }

    public StatusResponse untaggedOk(HumanReadableTextKey displayTextKey) {
        return untaggedOk(displayTextKey, null);
    }
}