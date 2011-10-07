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

package org.apache.james.pop3server.core;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageRange;
import org.apache.james.mailbox.MessageResult;
import org.apache.james.mailbox.MessageResult.FetchGroup;
import org.apache.james.pop3server.POP3Response;
import org.apache.james.pop3server.POP3Session;
import org.apache.james.pop3server.POP3StreamResponse;
import org.apache.james.protocols.api.Request;
import org.apache.james.protocols.api.Response;

/**
 * Handles TOP command
 */
public class TopCmdHandler extends RetrCmdHandler implements CapaCapability {
    private final static String COMMAND_NAME = "TOP";
    private final static FetchGroup GROUP = new FetchGroup() {

        @Override
        public int content() {
            return BODY_CONTENT | HEADERS;
        }

        @Override
        public Set<PartContentDescriptor> getPartContentDescriptors() {
            return null;
        }
        
    };
    
    /**
     * Handler method called upon receipt of a TOP command. This command
     * retrieves the top N lines of a specified message in the mailbox.
     * 
     * The expected command format is TOP [mail message number] [number of lines
     * to return]
     */
    @SuppressWarnings("unchecked")
    @Override
    public Response onCommand(POP3Session session, Request request) {
        POP3Response response = null;
        String parameters = request.getArgument();
        if (parameters == null) {
            response = new POP3Response(POP3Response.ERR_RESPONSE, "Usage: TOP [mail number] [Line number]");
            return response;
        }

        String argument = "";
        String argument1 = "";
        int pos = parameters.indexOf(" ");
        if (pos > 0) {
            argument = parameters.substring(0, pos);
            argument1 = parameters.substring(pos + 1);
        }

        if (session.getHandlerState() == POP3Session.TRANSACTION) {
            int num = 0;
            int lines = -1;
            try {
                num = Integer.parseInt(argument);
                lines = Integer.parseInt(argument1);
            } catch (NumberFormatException nfe) {
                response = new POP3Response(POP3Response.ERR_RESPONSE, "Usage: TOP [mail number] [Line number]");
                return response;
            }
            try {
                List<MessageMetaData> uidList = (List<MessageMetaData>) session.getState().get(POP3Session.UID_LIST);
                List<Long> deletedUidList = (List<Long>) session.getState().get(POP3Session.DELETED_UID_LIST);

                MailboxSession mailboxSession = (MailboxSession) session.getState().get(POP3Session.MAILBOX_SESSION);
                Long uid = uidList.get(num - 1).getUid();
                if (deletedUidList.contains(uid) == false) {

                    Iterator<MessageResult> results = session.getUserMailbox().getMessages(MessageRange.one(uid), GROUP, mailboxSession);

                    if (results.hasNext()) {
                        MessageResult result = results.next();

                        InputStream headersIn = result.getHeaders().getInputStream();
                        InputStream bodyIn = new CountingBodyInputStream(new ExtraDotInputStream(new CRLFTerminatedInputStream(result.getBody().getInputStream())), lines);

                        // write body
                        InputStream in = new SequenceInputStream(Collections.enumeration(Arrays.asList(headersIn, new ByteArrayInputStream("\r\n".getBytes()), bodyIn)));

                        response = new POP3StreamResponse(POP3Response.OK_RESPONSE, "Message follows", in);
                        return response;

                    } else {
                        StringBuilder exceptionBuffer = new StringBuilder(64).append("Message (").append(num).append(") does not exist.");
                        response = new POP3Response(POP3Response.ERR_RESPONSE, exceptionBuffer.toString());
                    }

                } else {
                    StringBuilder responseBuffer = new StringBuilder(64).append("Message (").append(num).append(") already deleted.");
                    response = new POP3Response(POP3Response.ERR_RESPONSE, responseBuffer.toString());
                }
            } catch (IOException ioe) {
                response = new POP3Response(POP3Response.ERR_RESPONSE, "Error while retrieving message.");
            } catch (MailboxException me) {
                response = new POP3Response(POP3Response.ERR_RESPONSE, "Error while retrieving message.");
            } catch (IndexOutOfBoundsException iob) {
                StringBuilder exceptionBuffer = new StringBuilder(64).append("Message (").append(num).append(") does not exist.");
                response = new POP3Response(POP3Response.ERR_RESPONSE, exceptionBuffer.toString());
            } catch (NoSuchElementException iob) {
                StringBuilder exceptionBuffer = new StringBuilder(64).append("Message (").append(num).append(") does not exist.");
                response = new POP3Response(POP3Response.ERR_RESPONSE, exceptionBuffer.toString());
            }
        } else {
            response = new POP3Response(POP3Response.ERR_RESPONSE);
        }
        return response;

    }

    /**
     * @see org.apache.james.pop3server.core.CapaCapability#getImplementedCapabilities(org.apache.james.pop3server.POP3Session)
     */
    public List<String> getImplementedCapabilities(POP3Session session) {
        List<String> caps = new ArrayList<String>();
        if (session.getHandlerState() == POP3Session.TRANSACTION) {
            caps.add(COMMAND_NAME);
            return caps;
        }
        return caps;
    }

    /**
     * @see org.apache.james.protocols.api.handler.CommandHandler#getImplCommands()
     */
    public Collection<String> getImplCommands() {
        List<String> commands = new ArrayList<String>();
        commands.add(COMMAND_NAME);
        return commands;
    }

    /**
     * This {@link InputStream} implementation can be used to limit the body
     * lines which will be read from the wrapped {@link InputStream}
     */
    private final class CountingBodyInputStream extends FilterInputStream {

        private int count = 0;
        private int limit = -1;
        private int lastChar;

        /**
         * 
         * @param in
         *            InputStream to read from
         * @param limit
         *            the lines to read. -1 is used for no limits
         */
        public CountingBodyInputStream(InputStream in, int limit) {
            super(in);
            this.limit = limit;
        }

        @Override
        public int read() throws IOException {
            if (limit != -1) {
                if (count <= limit) {
                    int a = in.read();

                    if (lastChar == '\r' && a == '\n') {
                        count++;
                    }
                    lastChar = a;

                    return a;
                } else {
                    return -1;
                }
            } else {
                return in.read();
            }

        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (limit == -1) {
                return in.read(b, off, len);
            } else {
                int i;
                for (i = 0; i < len; i++) {
                    int a = read();
                    if (i == 0 && a == -1) {
                        return -1;
                    } else {
                        if (a == -1) {
                            break;
                        } else {
                            b[off++] = (byte) a;
                        }
                    }
                }
                return i;
            }
        }

        @Override
        public int read(byte[] b) throws IOException {
            if (limit == -1) {
                return in.read(b);
            } else {
                return read(b, 0, b.length);
            }
        }

    }
}
