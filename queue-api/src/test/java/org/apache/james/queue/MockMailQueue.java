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


package org.apache.james.queue;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.mail.MessagingException;

import org.apache.mailet.Mail;

public class MockMailQueue implements MailQueue{

    private final LinkedBlockingQueue<Mail> queue = new LinkedBlockingQueue<Mail>();
    private boolean throwException;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    /**
     * Throw an {@link MailQueueException} on next operation
     */
    public void throwExceptionOnNextOperation() {
        this.throwException = true;
    }
    
    public void deQueue(DequeueOperation operation) throws MailQueueException, MessagingException {
        if (throwException) {
            throwException = false;
            throw new MailQueueException("Mock");
        }
        try {
            operation.process(queue.take());
        } catch (InterruptedException e) {
            throw new MailQueueException("Mock",e);
        }
    }

    public void enQueue(final Mail mail, long delay, TimeUnit unit) throws MailQueueException, MessagingException {
        if (throwException) {
            throwException = false;
            throw new MailQueueException("Mock");
        }        
        scheduler.schedule(new Runnable() {

            public void run() {
                try {
                    queue.put(mail);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }                
            }
            
        }, delay, unit);
    }

    public void enQueue(Mail mail) throws MailQueueException, MessagingException {
        if (throwException) {
            throwException = false;
            throw new MailQueueException("Mock");
        }
        try {
            queue.put(mail);
        } catch (InterruptedException e) {
            throw new MailQueueException("Mock",e);
        }
    }

    
    public void clear() {
        queue.clear();
    }
}
