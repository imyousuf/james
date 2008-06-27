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

import org.apache.james.services.SpoolRepository;
import org.apache.mailet.Mail;

import javax.mail.MessagingException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class MockSpoolRepository implements SpoolRepository {
    public ArrayList storedMails = new ArrayList();

    public Mail accept() throws InterruptedException {
        // TODO Auto-generated method stub
        return null;
    }

    public Mail accept(long delay) throws InterruptedException {
        // TODO Auto-generated method stub
        return null;
    }

    public Mail accept(AcceptFilter filter) throws InterruptedException {
        // TODO Auto-generated method stub
        return null;
    }

    public void store(Mail mc) throws MessagingException {
        
    }

    public Iterator list() throws MessagingException {
        // TODO Auto-generated method stub
        return null;
    }

    public Mail retrieve(String key) throws MessagingException {
        // TODO Auto-generated method stub
        return null;
    }

    public void remove(Mail mail) throws MessagingException {
        // TODO Auto-generated method stub
        
    }

    public void remove(Collection mails) throws MessagingException {
        // TODO Auto-generated method stub
        
    }

    public void remove(String key) throws MessagingException {
        // TODO Auto-generated method stub
        
    }

    public boolean lock(String key) throws MessagingException {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean unlock(String key) throws MessagingException {
        // TODO Auto-generated method stub
        return false;
    }
}
