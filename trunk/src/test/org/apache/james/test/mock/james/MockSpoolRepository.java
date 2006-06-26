/***********************************************************************
 * Copyright (c) 2000-2006 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/
package org.apache.james.test.mock.james;

import org.apache.james.services.SpoolRepository;
import org.apache.mailet.Mail;

import javax.mail.MessagingException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;

public class MockSpoolRepository implements SpoolRepository {
    public Map storedMails = new LinkedHashMap();

    public Mail accept() throws InterruptedException {
        throw new java.lang.NoSuchMethodError("method not yet implemented");
    }

    public Mail accept(long delay) throws InterruptedException {
        throw new java.lang.NoSuchMethodError("method not yet implemented");
    }

    public Mail accept(AcceptFilter filter) throws InterruptedException {
        throw new java.lang.NoSuchMethodError("method not yet implemented");
    }

    public void store(Mail mc) throws MessagingException {
        storedMails.put(mc.getMessage().getMessageID(), mc);
    }

    public Iterator list() throws MessagingException {
        return new ArrayList(storedMails.keySet()).iterator(); // iterate over clone!
    }

    public Mail retrieve(String key) throws MessagingException {
        return (Mail)storedMails.get(key);
    }

    public void remove(Mail mail) throws MessagingException {
        throw new java.lang.NoSuchMethodError("method not yet implemented");
    }

    public void remove(Collection mails) throws MessagingException {
        throw new java.lang.NoSuchMethodError("method not yet implemented");
    }

    public void remove(String key) throws MessagingException {
        storedMails.remove(key);
    }

    public boolean lock(String key) throws MessagingException {
        return true;
    }

    public boolean unlock(String key) throws MessagingException {
        return true;
    }
}
