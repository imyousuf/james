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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.mailet.MailAddress;

public class MailEnvelopeImpl implements MailEnvelope{

    private List<MailAddress> recipients;

    private MailAddress sender;

    private ByteArrayOutputStream outputStream;

    public int getSize() {
        if (outputStream == null)
            return -1;
        return outputStream.size();
    }

    public List<MailAddress> getRecipients() {
        return recipients;
    }

    public MailAddress getSender() {
        return sender;
    }

    public void setRecipients(List<MailAddress> recipientCollection) {
        this.recipients = recipientCollection;
    }

    public void setSender(MailAddress sender) {
        this.sender = sender;
    }

    public OutputStream getBodyOutputStream() {
        this.outputStream = new ByteArrayOutputStream(100000);
        return outputStream;
    }

    public InputStream getBodyInputStream() {
        return new ByteArrayInputStream(outputStream.toByteArray());
    }
}


