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


public interface MailQueueManagementMBean {

    /**
     * Return the size of the queue or -1 if the size could not get calculated
     * 
     * @return size the size or -1 if it could not get calculated
     */
    public long getSize();
    
    /**
     * Flush queue to make every Mail ready to consume. 
     * 
     * @return count the count of all flushed mails or -1 if the flush was not possible
     */
    public long flush();
    
    /**
     * Clear the queue
     * 
     * @return count the count of all removed mails or -1 if clear was not possible
     */
    public long clear();
    
    /**
     * Remove mail with name from the queue
     * 
     * @return count the count of all removed mails or -1 if clear was not possible
     */
    public long removeWithName(String name);
    
    /**
     * Remove mail with specific sender from the queue
     * 
     * @return count the count of all removed mails or -1 if clear was not possible
     */
    public long removeWithSender(String address);
    
    /**
     * Remove mail with specific recipient from the queue
     * 
     * @return count the count of all removed mails or -1 if clear was not possible
     */
    public long removeWithRecipient(String address);
    
}
