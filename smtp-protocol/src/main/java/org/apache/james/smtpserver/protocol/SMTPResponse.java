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

package org.apache.james.smtpserver.protocol;

import java.util.LinkedList;
import java.util.List;

import org.apache.james.api.protocol.RetCodeResponse;

/**
 * Contains an SMTP result
 */
public final class SMTPResponse implements RetCodeResponse {

    private String retCode = null;
    private List<CharSequence> lines = null;
    private String rawLine = null;
    private boolean endSession = false;
    
    /**
     * Construct a new SMTPResponse. The given code and description can not be null, if null an IllegalArgumentException
     * get thrown
     * 
     * @param code the returnCode
     * @param description the description 
     */
    public SMTPResponse(String code, CharSequence description) {
        if (code == null) throw new IllegalArgumentException("SMTPResponse code can not be null");
        if (description == null) new IllegalArgumentException("SMTPResponse description can not be null");
    
        this.setRetCode(code);
        this.appendLine(description);
        this.rawLine = code + " " + description;
    }
    
    /**
     * Construct a new SMTPResponse. The given rawLine need to be in format [SMTPResponseReturnCode SMTResponseDescription].
     * If this is not the case an IllegalArgumentException get thrown.
     * 
     * @param rawLine the raw SMTPResponse
     */
    public SMTPResponse(String rawLine) {
        String args[] = rawLine.split(" ");
        if (args != null && args.length > 1) {
            this.setRetCode(args[0]);
            this.appendLine(new StringBuilder(rawLine.substring(args[0].length()+1)));
        } else {
            throw new IllegalArgumentException("Invalid SMTPResponse format. Format should be [SMTPCode SMTPReply]");
        }
        this.rawLine = rawLine;
    }
    
    /**
     * Append the responseLine to the SMTPResponse
     * 
     * @param line the responseLine to append
     */
    public void appendLine(CharSequence line) {
        if (lines == null) {
            lines = new LinkedList<CharSequence>();
        }
        lines.add(line);
    }
    
    /**
     * Return the SMTPCode 
     * 
     * @return the SMTPCode
     */
    public String getRetCode() {
        return retCode;
    }

    /**
     * Set the SMTPCode
     *  
     * @param retCode the SMTPCode
     */
    public void setRetCode(String retCode) {
        this.retCode = retCode;
    }

    /**
     * Return a List of all responseLines stored in this SMTPResponse
     * 
     * @return all responseLines
     */
    public List<CharSequence> getLines() {
        return lines;
    }

    /**
     * Return the raw representation of the Stored SMTPResponse
     * 
     * @return rawLine the raw SMTPResponse
     */
    public String getRawLine() {
        return rawLine;
    }

    /**
     * Return true if the session is ended
     * 
     * @return true if session is ended
     */
    public boolean isEndSession() {
        return endSession;
    }

    /**
     * Set to true to end the session
     * 
     * @param endSession
     */
    public void setEndSession(boolean endSession) {
        this.endSession = endSession;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return getRetCode() + " " + getLines();
    }
}
