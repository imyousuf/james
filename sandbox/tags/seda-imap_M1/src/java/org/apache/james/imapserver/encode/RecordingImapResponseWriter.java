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
package org.apache.james.imapserver.encode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.james.imapserver.ImapResponse;
import org.apache.james.imapserver.ImapResponseWriter;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.commands.ImapCommandMessage;
import org.apache.james.imapserver.commands.ImapResponseMessage;

public class RecordingImapResponseWriter implements ImapResponseMessage,
        ImapCommandMessage, ImapResponseWriter {

    private static final EndResponse END_RESPONSE = new EndResponse();
    private static final UntaggedResponse UNTAGGED_RESPONSE = new UntaggedResponse();
    
    private final List responses = new ArrayList();
    
    public void encode(final ImapResponse response, ImapSession session) {
        for (final Iterator it=responses.iterator();it.hasNext();) {
            ImapResponseMessage message = (ImapResponseMessage) it.next();
            message.encode(response, null);
        }
    }

    public ImapResponseMessage process(ImapSession session) {
        return this;
    }

    public void commandName(String commandName) {
        final CommandNameResponse commandNameResponse = new CommandNameResponse(commandName);
        responses.add(commandNameResponse);
    }
    
    private static final class CommandNameResponse implements ImapResponseMessage {
        
        private final String name;
        public CommandNameResponse(final String name) {
            this.name = name;
        }
        
        public void encode(ImapResponse response, ImapSession session) {
            response.commandName(name);
        }
        
    }
    
    public void end() {
        responses.add(END_RESPONSE);
    }

    private static final class EndResponse implements ImapResponseMessage {
        public void encode(ImapResponse response, ImapSession session) {
            response.end();
        }
    }
    
    public void message(String message) {
        TextMessageResponse response = new TextMessageResponse(message);
        responses.add(response);
    }

    private static final class TextMessageResponse  implements ImapResponseMessage {
        private final String message;
        public TextMessageResponse(String message) {
            this.message = message;
        }
        public void encode(ImapResponse response, ImapSession session) {
            response.message(message);
        }
    }
    
    public void message(int number) {
        final NumericMessageResponse response = new NumericMessageResponse(number);
        responses.add(response);
    }
    
    private static final class NumericMessageResponse implements ImapResponseMessage {
        private final int message;
        public NumericMessageResponse(final int message) {
            this.message = message;
        }
        public void encode(ImapResponse response, ImapSession session) {
            response.message(message);
        }
    }

    public void responseCode(String responseCode) {
        ResponseCodeResponse message = new ResponseCodeResponse(responseCode);
        responses.add(message);
    }
    
    private static final class ResponseCodeResponse implements ImapResponseMessage {
        private final String responseCode;
        public ResponseCodeResponse(final String responseCode) {
            this.responseCode = responseCode;
        }
        public void encode(ImapResponse response, ImapSession session) {
                response.responseCode(responseCode);
        }
    }

    public void tag(String tag) {
        TagResponse response = new TagResponse(tag);
        responses.add(response);
    }
    
    private static final class TagResponse implements ImapResponseMessage {
        private final String tag;
        public TagResponse(final String tag) {
            this.tag = tag;
        }
        public void encode(ImapResponse response, ImapSession session) {
            response.tag(tag);
        }
        
        
    }

    public void untagged() {
        responses.add(UNTAGGED_RESPONSE);
    }

    private static final class UntaggedResponse implements ImapResponseMessage {
        
        public void encode(ImapResponse response, ImapSession session) {
            response.untagged();
        }
        
    }
}
