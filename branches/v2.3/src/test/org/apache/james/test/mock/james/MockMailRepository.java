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


package org.apache.james.test.mock.james;

import org.apache.james.core.MailImpl;
import org.apache.james.services.MailRepository;
import org.apache.mailet.Mail;

import javax.mail.MessagingException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;

public class MockMailRepository implements MailRepository {
    
    private HashMap messages = new HashMap();

    public void store(Mail mc) throws MessagingException {
        MailImpl m = new MailImpl(mc,mc.getName());
        m.setState(mc.getState());
        this.messages.put(mc.getName(),m);
    }

    public Iterator list() throws MessagingException {
        return messages.keySet().iterator();  // trivial implementation
    }

    public Mail retrieve(String key) throws MessagingException {
        Mail m2 = new MailImpl((Mail) messages.get(key),key);
        m2.setState(((Mail) messages.get(key)).getState());
        return m2;  // trivial implementation
    }

    public void remove(Mail mail) throws MessagingException {
        messages.remove(mail.getName());
    }

    public void remove(Collection mails) throws MessagingException {
        for (Iterator i = mails.iterator(); i.hasNext(); ) {
            Mail m = (Mail) i.next();
            messages.remove(m.getName());
        }
    }

    public void remove(String key) throws MessagingException {
        messages.remove(key);
    }

    public boolean lock(String key) throws MessagingException {
        return false;  // trivial implementation
    }

    public boolean unlock(String key) throws MessagingException {
        return false;  // trivial implementation
    }
}
