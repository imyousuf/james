/***********************************************************************
 * Copyright (c) 2000-2005 The Apache Software Foundation.             *
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

import org.apache.james.services.MailRepository;
import org.apache.mailet.Mail;

import javax.mail.MessagingException;
import java.util.Iterator;
import java.util.Collection;

public class MockMailRepository implements MailRepository {

    public void store(Mail mc) throws MessagingException {
        // trivial implementation
    }

    public Iterator list() throws MessagingException {
        return null;  // trivial implementation
    }

    public Mail retrieve(String key) throws MessagingException {
        return null;  // trivial implementation
    }

    public void remove(Mail mail) throws MessagingException {
        // trivial implementation
    }

    public void remove(Collection mails) throws MessagingException {
        // trivial implementation
    }

    public void remove(String key) throws MessagingException {
        // trivial implementation
    }

    public boolean lock(String key) throws MessagingException {
        return false;  // trivial implementation
    }

    public boolean unlock(String key) throws MessagingException {
        return false;  // trivial implementation
    }
}
