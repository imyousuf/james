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
package org.apache.james.queue.api;

import java.util.List;

/**
 * {@link MailQueue} which is manageable
 *
 */
public interface ManageableMailQueue extends MailQueue{

    public enum Type {
        Sender,
        Recipient,
        Name
    }
    
    /**
     * Return the size of the queue
     * 
     * @return size 
     * @throws MailQueueException
     */
    public long getSize() throws MailQueueException;
    
    /**
     * Flush the queue, which means it will make all message ready for dequeue
     * 
     * @return count the count of all flushed mails
     * @throws MailQueueException
     */
    public long flush() throws MailQueueException;
    
    /**
     * Remove all mails from the queue
     * 
     * @return count the count of all removed mails
     * @throws MailQueueException
     */
    public long clear() throws MailQueueException;
    
    /**
     * Remove all mails from the queue that match
     * 
     * @param type
     * @param value
     * @return count the count of all removed mails
     * @throws MailQueueException
     */
    public long remove(Type type, String value) throws MailQueueException;
    
    /**
     * Return a View on the content of the queue
     * 
     * @return content
     */
    public List<MailQueueItemView> view() throws MailQueueException;
    
    
    /**
     * A View of a {@link MailQueueItem}
     * 
     *
     */
    public interface MailQueueItemView {
        public String getName();
        public String getSender();
        public String[] getRecipients();
        public long getSize();
        public long getNextRetry();
    }
}
