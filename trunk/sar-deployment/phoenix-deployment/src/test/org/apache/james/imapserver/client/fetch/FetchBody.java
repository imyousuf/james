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

package org.apache.james.imapserver.client.fetch;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.imapserver.util.MessageGenerator;

public class FetchBody {

    final boolean peek;

    private FetchHeader fetchHeader;

    public FetchBody(boolean peek) {
        this.peek = peek;
    }

    public String getCommand() {
        String result= "";
        if (peek) {
            result += "BODY.PEEK[";
        } else {
            result += "BODY[";
        }
        if (fetchHeader!=null) {
            result += fetchHeader.getCommand();
        }
        result += "]";
        return result;
    }

    public String getResult(MimeMessage m) throws IOException,
            MessagingException {
        // TODO decide whether it should be BODY.PEEK when peek!
        String result = "BODY[";
        final String data;
        if (fetchHeader != null) {
            result += fetchHeader.getCommand();
            data = fetchHeader.getData(m);
        } else {
            data = getData(m);
        }
        result += "] {" + data.length() + "}\r\n" + data;
        // TODO Shouldn't we append another CRLF?
        return result;
    }

    private String getData(MimeMessage m) throws IOException,
            MessagingException {
        String data = MessageGenerator.messageContentToString(m);
        return data;
    }

    public void setFetchHeader(FetchHeader fetchHeader) {
        this.fetchHeader = fetchHeader;

    }

}
