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

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.mail.Flags;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.imap.message.response.imap4rev1.Literal;

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
            final String tag) throws IOException;

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
            final String responseCode, final String tag) throws IOException;

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
            final String reason, final String tag) throws IOException;

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
            String responseCode, String reason, final String tag) throws IOException;

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
    public abstract void commandError(final String message, final String tag) throws IOException;

    /**
     * Writes a standard untagged BAD response, together with a descriptive
     * message.
     */
    public abstract void badResponse(String message) throws IOException;

    /**
     * Writes a standard untagged BAD response, together with a descriptive
     * message.
     */
    public abstract void badResponse(String message, String tag) throws IOException;

    /**
     * Writes an untagged OK response, with the supplied response code, and an
     * optional message.
     * 
     * @param responseCode
     *                The response code, included in [].
     * @param message
     *                The message to follow the []
     */
    public abstract void okResponse(String responseCode, String message) throws IOException;

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
            String responseCode) throws IOException;

    /**
     * Writes flags to output using standard format.
     * @param flags <code>Flags</code>, not null
     */
    public abstract void flags(Flags flags) throws IOException;
    
    /**
     * Writes a complete FLAGS response.
     * @param flags <code>Flags</code>, not null
     */
    public abstract void flagsResponse(Flags flags) throws IOException;
    
    public abstract void existsResponse(int count) throws IOException;

    public abstract void recentResponse(int count) throws IOException;

    public abstract void expungeResponse(int msn) throws IOException;
    
    public abstract void searchResponse(long[] ids) throws IOException;
    
    /**
     * Starts a FETCH response by writing the opening
     * star-FETCH-number-paren sequence.
     * @param msn message number
     * @see #closeFetchResponse()
     */
    public abstract void openFetchResponse(long msn) throws IOException;
    
    /**
     * Ends a FETCH response by writing the closing
     * paren-crlf sequence.
     */
    public abstract void closeFetchResponse() throws IOException;
    
    /**
     * Starts a <code>FETCH ENVELOPE</code> production.
     * @param date envelope date, or null for <code>NIL</code>
     * @param subject envelope subject, or null for <code>NIL</code>
     * @throws IOException
     * @see {@link #endEnvelope(String, String)} must be called
     */
    public abstract void startEnvelope(String date, String subject) throws IOException;
    
    /**
     * Starts a list of addresses.
     * @throws IOException
     */
    public abstract void startAddresses() throws IOException;
    
    /**
     * Composes an address.
     * @param name personal name, or null for <code>NIL</code>
     * @param domainList route address list, or null for <code>NIL</code>
     * @param mailbox mailbox name, or null for <code>NIL</code>
     * @param host host name, or null for <code>NIL</code>
     * @throws IOException
     */
    public abstract void address(String name, String domainList, String mailbox, String host) throws IOException;
    
    /**
     * Ends a list of addresses.
     * @throws IOException
     */
    public abstract void endAddresses() throws IOException;
    
    /**
     * Ends a <code>FETCH ENVELOPE</code> production.
     * @param inReplyTo envelope in-reply-to, or null for <code>NIL</code>
     * @param messageId envelope message-id, or null for <code>NIL</code>
     * @throws IOException
     */
    public abstract void endEnvelope(String inReplyTo, String messageId) throws IOException;
    
    /**
     * Composes a <code>NIL</code>.
     * @throws IOException
     */
    public abstract void nil() throws IOException;
    
    public abstract void commandResponse(ImapCommand command, String message) throws IOException;

    /**
     * Writes a list response
     * @param typeName <code>LIST</code> or <code>LSUB</code>.
     * @param attributes name attributes, 
     * or null if there are no attributes
     * @param hierarchyDelimiter hierarchy delimiter, 
     * or null if delimiter is <code>NIL</code>
     * @param name mailbox name
     */
    public abstract void listResponse(String typeName, List attributes, String hierarchyDelimiter, String name) throws IOException;
    
    /**
     * Writes the message provided to the client, prepended with the request
     * tag.
     * 
     * @param message
     *                The message to write to the client.
     */
    public abstract void taggedResponse(String message, String tag) throws IOException;

    /**
     * Writes the message provided to the client, prepended with the untagged
     * marker "*".
     * 
     * @param message
     *                The message to write to the client.
     */
    public abstract void untaggedResponse(String message) throws IOException;

    public abstract void byeResponse(String message) throws IOException;

    public abstract void untagged() throws IOException;

    public abstract void commandName(final String name) throws IOException;

    public ImapResponseComposer message(final String message) throws IOException;

    public abstract void message(final long number) throws IOException;

    public abstract void end() throws IOException;

    public abstract void tag(String tag) throws IOException;

    public abstract void statusResponse(String tag, ImapCommand command,
            String type, String responseCode, Collection parameters, long number, String text) throws IOException;
    
    public abstract void statusResponse(Long messages, Long recent,
            Long uidNext, Long uidValidity, Long unseen, String mailboxName) throws IOException;
    
    public void quote(String message) throws IOException;

    public void literal(Literal literal) throws IOException;
    
    public ImapResponseComposer openParen() throws IOException;
    public ImapResponseComposer closeParen() throws IOException;
    
    /**
     * Appends the given message after conversion to upper case.
     * The message may be assumed to be ASCII encoded.
     * Conversion of characters MUST NOT be performed according 
     * to the current locale but as per ASCII.
     * @param message ASCII encoded, not null
     * @return self, not null
     * @throws IOException
     */
    public ImapResponseComposer upperCaseAscii(final String message) throws IOException;
    
    /**
     * Appends the given message after conversion to upper case.
     * The message may be assumed to be ASCII encoded.
     * Conversion of characters MUST NOT be performed according 
     * to the current locale but as per ASCII.
     * @param message ASCII encoded, not null
     * @return self, not null
     * @throws IOException
     */
    public ImapResponseComposer quoteUpperCaseAscii(final String message) throws IOException;
    
    /**
     * Appends the given message after appropriate quoting (when not null)
     * or <code>NIL</code> (when null).
     * @param message possibly null
     * @return self, not null
     * @throws IOException
     */
    public ImapResponseComposer nillableQuote(String message) throws IOException;
    
    /**
     * Composes a sequence of nillables quotes.
     * When messages are null, a single <code>NIL</code> is appended.
     * Otherwise, each element is appended in sequence as per {@link #nillableQuote(String)}.
     * @param quotes messages, possibly null
     * @return self, not null
     * @throws IOException
     */
    public ImapResponseComposer nillableQuotes(String[] quotes) throws IOException;
    
    /**
     * Composes a nillable composition.
     * When the master quote is null, <code>NIL</code> is appended.
     * Otherwise, a parenthesized list is created starting with the master quote.
     * When the quotes are null, <code>NIL</code> is appended only.
     * Otherwise, each element is appended in sequence as per {@link #nillableQuote(String)}
     * @param quote master, possibly null
     * @param quotes quotes, possibly null
     * @return self, not null
     * @throws IOException
     */
    public ImapResponseComposer nillableComposition(String masterQuote, String[] quotes) throws IOException;

    public void skipNextSpace() throws IOException;
}