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


package org.apache.james.test.mock.mailet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

public class MockMail implements Mail {

    private MimeMessage msg = null;

    private Collection recipients = new ArrayList();

    private String name = null;

    private MailAddress sender = null;

    private String state = null;

    private String errorMessage;

    private Date lastUpdated;

    private HashMap attributes = new HashMap();

    private String hostAddress;

    private String hostName;

    private MailAddress reversePath;

    private static final long serialVersionUID = 1L;

    public String getName() {
        return name;
    }

    public void setName(String newName) {
        this.name = newName;
    }

    public MimeMessage getMessage() throws MessagingException {
        return msg;
    }

    public Collection getRecipients() {
        return recipients;
    }

    public void setRecipients(Collection recipients) {
        this.recipients = recipients;
    }

    public MailAddress getSender() {
        return sender;
    }

    public String getState() {
        return state;
    }

    public String getRemoteHost() {
        return "111.222.333.444";
    }

    public String getRemoteAddr() {
        return "my.mock.remote.host";
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String msg) {
        this.errorMessage = msg;
    }

    public void setMessage(MimeMessage message) {
        this.msg = message;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Serializable getAttribute(String name) {
        return (Serializable) attributes.get(name);
    }

    public Iterator getAttributeNames() {
        return attributes.keySet().iterator();
    }

    public boolean hasAttributes() {
        return !attributes.isEmpty();
    }

    public Serializable removeAttribute(String name) {
        return (Serializable) attributes.remove(name);

    }

    public void removeAllAttributes() {
        attributes.clear();
    }

    public Serializable setAttribute(String name, Serializable object) {

        return (Serializable) attributes.put(name, object);
    }

    public long getMessageSize() throws MessagingException {
        throw new UnsupportedOperationException("Unimplemented mock service");
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /**
     * @see org.apache.mailet.Mail#dispose()
     */
    public void dispose() {

        // TODO Auto-generated method stub MUST contain a statement
        // Blame: angusd
        
    }

    /**
     * @see org.apache.mailet.Mail#setRemoteAddr(java.lang.String)
     */
    public void setRemoteAddr(String hostAddress) {

        this.hostAddress = hostAddress;
        
    }

    /**
     * @see org.apache.mailet.Mail#setRemoteHost(java.lang.String)
     */
    public void setRemoteHost(String hostName) {

        this.hostName=hostName;
    }

    /**
     * @see org.apache.mailet.Mail#setSender(org.apache.mailet.MailAddress)
     */
    public void setSender(MailAddress reversePath) {

        this.reversePath = reversePath;
        
    }

}
