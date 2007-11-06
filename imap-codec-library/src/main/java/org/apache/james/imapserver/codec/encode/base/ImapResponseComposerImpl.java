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

package org.apache.james.imapserver.codec.encode.base;

import java.util.Iterator;
import java.util.List;

import javax.mail.Flags;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapConstants;
import org.apache.james.api.imap.message.MessageFlags;
import org.apache.james.imapserver.codec.encode.ImapResponseComposer;
import org.apache.james.imapserver.codec.encode.ImapResponseWriter;

/**
 * Class providing methods to send response messages from the server to the
 * client.
 */
public class ImapResponseComposerImpl extends AbstractLogEnabled implements
        ImapConstants, ImapResponseWriter, ImapResponseComposer {

    public static final String FETCH = "FETCH";

    public static final String EXPUNGE = "EXPUNGE";

    public static final String RECENT = "RECENT";

    public static final String EXISTS = "EXISTS";

    public static final String FLAGS = "FLAGS";

    public static final String FAILED = "failed.";

    private final ImapResponseWriter writer;

    public ImapResponseComposerImpl(final ImapResponseWriter writer) {
        this.writer = writer;
    }

    /**
     * @see org.apache.james.imapserver.codec.encode.ImapResponseComposer#commandComplete(org.apache.james.api.imap.ImapCommand, java.lang.String)
     */
    public void commandComplete(final ImapCommand command, final String tag) {
        commandComplete(command, null, tag);
    }

    /**
     * @see org.apache.james.imapserver.codec.encode.ImapResponseComposer#commandComplete(org.apache.james.api.imap.ImapCommand, java.lang.String, java.lang.String)
     */
    public void commandComplete(final ImapCommand command,
            final String responseCode, final String tag) {
        tag(tag);
        message(OK);
        responseCode(responseCode);
        commandName(command);
        message("completed.");
        end();
    }

    /**
     * @see org.apache.james.imapserver.codec.encode.ImapResponseComposer#commandFailed(org.apache.james.api.imap.ImapCommand, java.lang.String, java.lang.String)
     */
    public void commandFailed(final ImapCommand command, final String reason,
            final String tag) {
        commandFailed(command, null, reason, tag);
    }

    /**
     * @see org.apache.james.imapserver.codec.encode.ImapResponseComposer#commandFailed(org.apache.james.api.imap.ImapCommand, java.lang.String, java.lang.String, java.lang.String)
     */
    public void commandFailed(ImapCommand command, String responseCode,
            String reason, final String tag) {
        tag(tag);
        message(NO);
        responseCode(responseCode);
        commandName(command);
        message(FAILED);
        message(reason);
        end();
        final Logger logger = getLogger();
        if (logger != null && logger.isInfoEnabled()) {
            logger.info("COMMAND FAILED [" + responseCode + "] - " + reason);
        }
    }

    /**
     * @see org.apache.james.imapserver.codec.encode.ImapResponseComposer#commandError(java.lang.String, java.lang.String)
     */
    public void commandError(final String message, final String tag) {
        tag(tag);
        message(BAD);
        message(message);
        end();
        final Logger logger = getLogger();
        if (logger != null && logger.isInfoEnabled()) {
            logger.info("ERROR - " + message);
        }
    }

    /**
     * @see org.apache.james.imapserver.codec.encode.ImapResponseComposer#badResponse(java.lang.String)
     */
    public void badResponse(String message) {
        untagged();
        message(BAD);
        message(message);
        end();
        final Logger logger = getLogger();
        if (logger != null && logger.isInfoEnabled()) {
            logger.info("BAD - " + message);
        }
    }

    /**
     * @see org.apache.james.imapserver.codec.encode.ImapResponseComposer#badResponse(java.lang.String, java.lang.String)
     */
    public void badResponse(String message, String tag) {
        tag(tag);
        message(BAD);
        message(message);
        end();
        final Logger logger = getLogger();
        if (logger != null && logger.isInfoEnabled()) {
            logger.info("BAD - " + message);
        }
    }

    /**
     * @see org.apache.james.imapserver.codec.encode.ImapResponseComposer#okResponse(java.lang.String, java.lang.String)
     */
    public void okResponse(String responseCode, String message) {
        untagged();
        message(OK);
        responseCode(responseCode);
        message(message);
        end();
    }

    /**
     * @see org.apache.james.imapserver.codec.encode.ImapResponseComposer#untaggedNoResponse(java.lang.String, java.lang.String)
     */
    public void untaggedNoResponse(String displayMessage, String responseCode) {
        untagged();
        message(NO);
        responseCode(responseCode);
        message(displayMessage);
        end();
    }

    /**
     * @see org.apache.james.imapserver.codec.encode.ImapResponseComposer#flagsResponse(javax.mail.Flags)
     */
    public void flagsResponse(Flags flags) {
        untagged();
        message(FLAGS);
        message(MessageFlags.format(flags));
        end();
    }

    /**
     * @see org.apache.james.imapserver.codec.encode.ImapResponseComposer#existsResponse(int)
     */
    public void existsResponse(int count) {
        untagged();
        message(count);
        message(EXISTS);
        end();
    }

    /**
     * @see org.apache.james.imapserver.codec.encode.ImapResponseComposer#recentResponse(int)
     */
    public void recentResponse(int count) {
        untagged();
        message(count);
        message(RECENT);
        end();
    }

    /**
     * @see org.apache.james.imapserver.codec.encode.ImapResponseComposer#expungeResponse(int)
     */
    public void expungeResponse(int msn) {
        untagged();
        message(msn);
        message(EXPUNGE);
        end();
    }

    /**
     * @see org.apache.james.imapserver.codec.encode.ImapResponseComposer#fetchResponse(int, java.lang.String)
     */
    public void fetchResponse(int msn, String msgData) {
        untagged();
        message(msn);
        message(FETCH);
        message("(" + msgData + ")");
        end();
    }

    /**
     * @see org.apache.james.imapserver.codec.encode.ImapResponseComposer#commandResponse(org.apache.james.api.imap.ImapCommand, java.lang.String)
     */
    public void commandResponse(ImapCommand command, String message) {
        untagged();
        commandName(command);
        message(message);
        end();
    }

    /**
     * @see org.apache.james.imapserver.codec.encode.ImapResponseComposer#taggedResponse(java.lang.String, java.lang.String)
     */
    public void taggedResponse(String message, String tag) {
        tag(tag);
        message(message);
        end();
    }

    /**
     * @see org.apache.james.imapserver.codec.encode.ImapResponseComposer#untaggedResponse(java.lang.String)
     */
    public void untaggedResponse(String message) {
        untagged();
        message(message);
        end();
    }

    /**
     * @see org.apache.james.imapserver.codec.encode.ImapResponseComposer#byeResponse(java.lang.String)
     */
    public void byeResponse(String message) {
        untaggedResponse(BYE + SP + message);
    }

    /**
     * @see org.apache.james.imapserver.codec.encode.ImapResponseComposer#untagged()
     */
    public void untagged() {
        writer.untagged();
    }

    private void commandName(final ImapCommand command) {
        final String name = command.getName();
        commandName(name);
    }

    /**
     * @see org.apache.james.imapserver.codec.encode.ImapResponseComposer#commandName(java.lang.String)
     */
    public void commandName(final String name) {
        writer.commandName(name);
    }

    /**
     * @see org.apache.james.imapserver.codec.encode.ImapResponseComposer#message(java.lang.String)
     */
    public void message(final String message) {
        if (message != null) {
            // TODO: consider message normalisation
            // TODO: CR/NFs in message must be replaced
            // TODO: probably best done in the writer
            writer.message(message);
        }
    }

    /**
     * @see org.apache.james.imapserver.codec.encode.ImapResponseComposer#message(int)
     */
    public void message(final int number) {
        writer.message(number);
    }

    /**
     * @see org.apache.james.imapserver.codec.encode.ImapResponseComposer#responseCode(java.lang.String)
     */
    public void responseCode(final String responseCode) {
        if (responseCode != null) {
            writer.responseCode(responseCode);
        }
    }

    /**
     * @see org.apache.james.imapserver.codec.encode.ImapResponseComposer#end()
     */
    public void end() {
        writer.end();
    }

    /**
     * @see org.apache.james.imapserver.codec.encode.ImapResponseComposer#permanentFlagsResponse(javax.mail.Flags)
     */
    public void permanentFlagsResponse(Flags flags) {
        untagged();
        message(OK);
        responseCode("PERMANENTFLAGS " + MessageFlags.format(flags));
        end();
    }

    /**
     * @see org.apache.james.imapserver.codec.encode.ImapResponseComposer#tag(java.lang.String)
     */
    public void tag(String tag) {
        writer.tag(tag);
    }
    
    /**
     * @see org.apache.james.imapserver.codec.encode.ImapResponseComposer#statusResponse(java.lang.String, org.apache.james.api.imap.ImapCommand, java.lang.String, java.lang.String, java.lang.String)
     */
    public void statusResponse(String tag, ImapCommand command, String type, String responseCode, String text) {
        if (tag == null) {
            untagged();
        } else {
            tag(tag);
        }
        message(type);
        if (responseCode != null) {
            message(responseCode);
        }
        if (command != null) {
            commandName(command);
        }
        if (text != null) {
            message(text);
        }
        end();
    }

    public void listResponse(String typeName, List attributes, String hierarchyDelimiter, String name) {
        untagged();
        message(typeName);
        openParen();
        if (attributes != null) {
            for (Iterator it=attributes.iterator();it.hasNext();) {
                final String attribute = (String) it.next();
                message(attribute);
            }
        }
        closeParen();
        
        if (hierarchyDelimiter == null) {
            message(NIL);
        } else {
            quote(hierarchyDelimiter);
        }
        
        quote(name);
        
        end();
    }

    public void quote(String message) {
        writer.quote(message);
    }

    public void closeParen() {
        writer.closeParen();
        
    }

    public void openParen() {
        writer.openParen();
    }
}
