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

package org.apache.james.transport.remotedeliverytester;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;

/**
 * An SMTPTransport to be used for testing purposes.
 * Behaviour is programmatically managed by the Tester class.
 */
public class SMTPTransport extends Transport {
    
    Tester owner;
    boolean connected;
    
    public SMTPTransport(Session session, URLName urlname) {
        super(session, urlname);
        owner = (Tester) session.getProperties().get("Tester");
        connected = false;
        if (owner == null) {
            owner = Tester.getInstance();
            // throw new IllegalStateException("Can only be used by a Tester: "+session.getProperty("Tester")+"|"+session.getProperties().get("mail.smtp.class"));
        }
    }
    
    protected SMTPTransport(Session session, URLName urlname, String s, int i, boolean flag) {
        this(session, urlname);
    }

    public synchronized void connect() throws MessagingException {
        connected = true;
        owner.onTransportConnect(this);
    }

    public synchronized void sendMessage(Message arg0, Address[] arg1) throws MessagingException, SendFailedException {
        owner.onTransportSendMessage(this, arg0, arg1);
    }

    public synchronized void close() throws MessagingException {
        owner.onTransportClose(this);
        connected = false;
    }

    public boolean supportsExtension(String arg0) {
        return owner.OnTransportSupportsExtension(this, arg0);
    }

    public synchronized boolean isConnected() {
        return connected;
    }

}
