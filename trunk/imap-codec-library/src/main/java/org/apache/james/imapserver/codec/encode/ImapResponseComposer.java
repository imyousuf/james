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

package org.apache.james.imapserver.codec.encode;

import java.util.List;

import javax.mail.Flags;

import org.apache.james.api.imap.ImapCommand;

public interface ImapResponseComposer {

    /**
     * Writes a standard tagged OK response on completion of a command. Response
     * is writen as:
     * 
     * <pre>
     *      a01 OK COMMAND_NAME completed.
     * </pre>
     * 
     * @param command
     *                The ImapCommand which was completed.
     */
    public abstract void commandComplete(final ImapCommand command,
            final String tag);

    /**
     * Writes a standard tagged OK response on completion of a command, with a
     * response code (eg READ-WRITE) Response is writen as:
     * 
     * <pre>
     *      a01 OK [responseCode] COMMAND_NAME completed.
     * </pre>
     * 
     * @param command
     *                The ImapCommand which was completed.
     * @param responseCode
     *                A string response code to send to the client.
     */
    public abstract void commandComplete(final ImapCommand command,
            final String responseCode, final String tag);

    /**
     * Writes a standard NO response on command failure, together with a
     * descriptive message. Response is writen as:
     * 
     * <pre>
     *      a01 NO COMMAND_NAME failed. &lt;reason&gt;
     * </pre>
     * 
     * @param command
     *                The ImapCommand which failed.
     * @param reason
     *                A message describing why the command failed.
     */
    public abstract void commandFailed(final ImapCommand command,
            final String reason, final String tag);

    /**
     * Writes a standard NO response on command failure, together with a
     * descriptive message. Response is writen as:
     * 
     * <pre>
     *      a01 NO [responseCode] COMMAND_NAME failed. &lt;reason&gt;
     * </pre>
     * 
     * @param command
     *                The ImapCommand which failed.
     * @param responseCode
     *                The Imap response code to send.
     * @param reason
     *                A message describing why the command failed.
     */
    public abstract void commandFailed(ImapCommand command,
            String responseCode, String reason, final String tag);

    /**
     * Writes a standard BAD response on command error, together with a
     * descriptive message. Response is writen as:
     * 
     * <pre>
     *      a01 BAD &lt;message&gt;
     * </pre>
     * 
     * @param message
     *                The descriptive error message.
     */
    public abstract void commandError(final String message, final String tag);

    /**
     * Writes a standard untagged BAD response, together with a descriptive
     * message.
     */
    public abstract void badResponse(String message);

    /**
     * Writes a standard untagged BAD response, together with a descriptive
     * message.
     */
    public abstract void badResponse(String message, String tag);

    /**
     * Writes an untagged OK response, with the supplied response code, and an
     * optional message.
     * 
     * @param responseCode
     *                The response code, included in [].
     * @param message
     *                The message to follow the []
     */
    public abstract void okResponse(String responseCode, String message);

    /**
     * Writes an untagged NO response. Indicates that a warning. The command may
     * still complete sucessfully.
     * 
     * @param displayMessage
     *                message for display, not null
     * @param responseCode
     *                response code or null when there is no response code
     */
    public abstract void untaggedNoResponse(String displayMessage,
            String responseCode);

    public abstract void flagsResponse(Flags flags);

    public abstract void existsResponse(int count);

    public abstract void recentResponse(int count);

    public abstract void expungeResponse(int msn);
    
    public abstract void searchResponse(long[] ids);

    public abstract void fetchResponse(int msn, String msgData);

    public abstract void commandResponse(ImapCommand command, String message);

    /**
     * Writes a list response
     * @param typeName <code>LIST</code> or <code>LSUB</code>.
     * @param attributes name attributes, 
     * or null if there are no attributes
     * @param hierarchyDelimiter hierarchy delimiter, 
     * or null if delimiter is <code>NIL</code>
     * @param name mailbox name
     */
    public abstract void listResponse(String typeName, List attributes, String hierarchyDelimiter, String name);
    
    /**
     * Writes the message provided to the client, prepended with the request
     * tag.
     * 
     * @param message
     *                The message to write to the client.
     */
    public abstract void taggedResponse(String message, String tag);

    /**
     * Writes the message provided to the client, prepended with the untagged
     * marker "*".
     * 
     * @param message
     *                The message to write to the client.
     */
    public abstract void untaggedResponse(String message);

    public abstract void byeResponse(String message);

    public abstract void untagged();

    public abstract void commandName(final String name);

    public abstract void message(final String message);

    public abstract void message(final long number);

    public abstract void responseCode(final String responseCode);

    public abstract void end();

    public abstract void permanentFlagsResponse(Flags flags);

    public abstract void tag(String tag);

    public abstract void statusResponse(String tag, ImapCommand command,
            String type, String responseCode, String text);

}