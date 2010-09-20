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

import java.util.concurrent.TimeUnit;

import javax.mail.MessagingException;

import org.apache.mailet.Mail;

/**
 * A Queue/Spool for Mails. How the Queue handles the ordering of the 
 * dequeuing is up to the implementation. 
 * 
 * IMPORTANT:
 * 
 * Implementations does not need to keep all {@link Mail} Attributes when enqueue emails. The implementations are only 
 * in the need of supporting at least this kind of Primitives as values:
 *     - Long
 *     - Byte
 *     - Integer
 *     - String
 *     - Boolean
 *     - Short
 *     - Float
 *     - Double
 *
 */
public interface MailQueue {

    /**
     * Enqueue the Mail to the queue. The given delay and unit are used to calculate the time when the 
     * Mail will be avaible for dequeue
     * 
     * @param mail
     * @param delay
     * @param unit
     * @throws MailQueueException
     */
    public void enQueue(Mail mail, long delay, TimeUnit unit) throws MailQueueException, MessagingException;
    
    
    /**
     * Enqueue the Mail to the queue
     * 
     * @param mail
     * @throws MailQueueException
     */
    public void enQueue(Mail mail) throws MailQueueException, MessagingException;
    
    
    /**
     * Dequeue the next ready-to-process Mail of the queue. This method will block until a Mail is ready and then process
     * the {@link DequeueOperation}. Implementations should take care todo some kind of transactions to not loose any mail on error
     * 
     * @param dequeueOperation
     * @throws MailQueueException
     */
    public void deQueue(DequeueOperation operation) throws MailQueueException, MessagingException;
    
    
    /**
     * Exception which will get thrown if any problems occur while working the {@link MailQueue}
     * 
     *
     */
    @SuppressWarnings("serial")
    public class MailQueueException extends MessagingException {
        public MailQueueException(String msg, Exception e) {
            super(msg, e);
        }
        
        public MailQueueException(String msg) {
            super(msg);
        }
    }
    
    
    /**
     * 
     * Operation which will get executed once a new Mail is ready to process
     */
    public interface DequeueOperation {
        
        /**
         * Process some action on the mail
         * @param mail
         * @throws MailQueueException
         * @throws MessagingException
         */
        public void process(Mail mail) throws MailQueueException, MessagingException;
    }
}
