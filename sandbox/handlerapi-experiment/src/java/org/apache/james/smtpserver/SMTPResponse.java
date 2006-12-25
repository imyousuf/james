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

import java.util.LinkedList;
import java.util.List;

/**
 * Contains an SMTP result
 */
public class SMTPResponse {

    private String retCode = null;
    private List lines = null;
    private String rawLine = null;
    private boolean endSession = false;
    
    public SMTPResponse() {
        
    }
    
    public SMTPResponse(String code, String description) {
        this.setRetCode(code);
        this.appendLine(description);
    }
    
    public SMTPResponse(String code, StringBuffer description) {
        this.setRetCode(code);
        this.appendLine(description);
    }
    
    public SMTPResponse(String rawLine) {
    String args[] = rawLine.split(" ");
    if (args != null && args.length > 1) {
        this.setRetCode(args[0]);
        this.appendLine(new StringBuffer(rawLine.substring(args[0].length()+1)));
    } else {
        // TODO: Throw exception ?
    }
        this.rawLine = rawLine;
    }
    
    public void appendLine(String line) {
        if (lines == null) {
            lines = new LinkedList();
        }
        lines.add(line);
    }
    
    public void appendLine(StringBuffer line) {
        if (lines == null) {
            lines = new LinkedList();
        }
        lines.add(line);
    }

    public String getRetCode() {
        return retCode;
    }

    public void setRetCode(String retCode) {
        this.retCode = retCode;
    }

    public List getLines() {
        return lines;
    }

    public String getRawLine() {
        return rawLine;
    }

    public boolean isEndSession() {
        return endSession;
    }

    public void setEndSession(boolean endSession) {
        this.endSession = endSession;
    }

}
