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
package org.apache.james.queue.library;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.james.queue.api.MailQueueManagementMBean;
import org.apache.james.queue.api.ManageableMailQueue;
import org.apache.james.queue.api.MailQueue.MailQueueException;
import org.apache.james.queue.api.ManageableMailQueue.Type;

/**
 * 
 * JMX MBean implementation which expose management functions by wrapping a {@link ManageableMailQueue}
 * 
 *
 */
public class MailQueueManagement extends StandardMBean implements MailQueueManagementMBean{
    private final ManageableMailQueue queue;

    public MailQueueManagement(ManageableMailQueue queue) throws NotCompliantMBeanException {
        super(MailQueueManagementMBean.class);
        this.queue = queue;
        
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.queue.api.MailQueueManagementMBean#clear()
     */
    public long clear() {
        try {
            return queue.clear();
        } catch (MailQueueException e) {
            return -1;
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.queue.api.MailQueueManagementMBean#flush()
     */
    public long flush() {
        try {
            return queue.flush();
        } catch (MailQueueException e) {
            return -1;
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.queue.api.MailQueueManagementMBean#getSize()
     */
    public long getSize() {
        try {
            return queue.getSize();
        } catch (MailQueueException e) {
            return -1;
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.queue.api.MailQueueManagementMBean#removeWithName(java.lang.String)
     */
    public long removeWithName(String name) {
        try {
            return queue.remove(Type.Name, name);
        } catch (MailQueueException e) {
            return -1;
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.queue.api.MailQueueManagementMBean#removeWithRecipient(java.lang.String)
     */
    public long removeWithRecipient(String address) {
        try {
            return queue.remove(Type.Recipient, address);
        } catch (MailQueueException e) {
            return -1;
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.queue.api.MailQueueManagementMBean#removeWithSender(java.lang.String)
     */
    public long removeWithSender(String address) {
        try {
            return queue.remove(Type.Sender, address);
        } catch (MailQueueException e) {
            return -1;
        }
    }
}
