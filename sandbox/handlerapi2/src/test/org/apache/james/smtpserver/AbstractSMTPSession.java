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

package org.apache.james.smtpserver;

import org.apache.james.util.watchdog.Watchdog;
import org.apache.mailet.Mail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Abstract class to simplify the mocks
 */
public class AbstractSMTPSession implements SMTPSession {

    /**
     * @see org.apache.james.smtpserver.SMTPSession#abortMessage()
     */
    public void abortMessage() {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#clearResponseBuffer()
     */
    public String clearResponseBuffer() {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#endSession()
     */
    public void endSession() {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#getCommandArgument()
     */
    public String getCommandArgument() {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#getCommandName()
     */
    public String getCommandName() {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#getConfigurationData()
     */
    public SMTPHandlerConfigurationData getConfigurationData() {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#getConnectionState()
     */
    public Map getConnectionState() {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#getInputStream()
     */
    public InputStream getInputStream() {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#getMail()
     */
    public Mail getMail() {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#getRcptCount()
     */
    public int getRcptCount() {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#getRemoteHost()
     */
    public String getRemoteHost() {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#getRemoteIPAddress()
     */
    public String getRemoteIPAddress() {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#getResponseBuffer()
     */
    public StringBuffer getResponseBuffer() {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#getSessionID()
     */
    public String getSessionID() {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#getState()
     */
    public Map getState() {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#getUser()
     */
    public String getUser() {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#getWatchdog()
     */
    public Watchdog getWatchdog() {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#isAuthRequired()
     */
    public boolean isAuthRequired() {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#isRelayingAllowed()
     */
    public boolean isRelayingAllowed() {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#isSessionEnded()
     */
    public boolean isSessionEnded() {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#readCommandLine()
     */
    public String readCommandLine() throws IOException {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#resetConnectionState()
     */
    public void resetConnectionState() {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#resetState()
     */
    public void resetState() {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#setMail(org.apache.mailet.Mail)
     */
    public void setMail(Mail mail) {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#setRelayingAllowed(boolean)
     */
    public void setRelayingAllowed(boolean relayingAllowed) {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#setUser(java.lang.String)
     */
    public void setUser(String user) {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#useHeloEhloEnforcement()
     */
    public boolean useHeloEhloEnforcement() {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#writeResponse(java.lang.String)
     */
    public void writeResponse(String respString) {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#getSMTPResponse()
     */
    public SMTPResponse getSMTPResponse() {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#setSMTPResponse(SMTPResponse)
     */
    public void setSMTPResponse(SMTPResponse response) {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    public void doChain() {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    public IOState getIOState() {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    public void popIOState() {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

    public void pushIOState(IOState io) {
    throw new UnsupportedOperationException("Unimplemented Stub Method");
    }

}
