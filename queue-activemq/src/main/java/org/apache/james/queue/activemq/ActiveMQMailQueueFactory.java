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
package org.apache.james.queue.activemq;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.jms.BytesMessage;
import javax.jms.ConnectionFactory;
import javax.jms.Message;

import org.apache.activemq.BlobMessage;
import org.apache.commons.logging.Log;
import org.apache.james.lifecycle.LogEnabled;
import org.apache.james.queue.MailQueue;
import org.apache.james.queue.MailQueueFactory;


/**
 * {@link MailQueueFactory} implementations which return {@link ActiveMQMailQueue} instances
 * 
 * 
 * 
 *
 */
public class ActiveMQMailQueueFactory implements MailQueueFactory, LogEnabled{

    
    
    private final Map<String, MailQueue> queues = new HashMap<String, MailQueue>();
    private ConnectionFactory connectionFactory;
    private long sizeTreshold = ActiveMQMailQueue.DISABLE_TRESHOLD;
    private Log log;
    
    @Resource(name="jmsConnectionFactory")
    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }
    
    /**
     * The size treshold which will be used for setting if a {@link BlobMessage} or {@link BytesMessage} will be used
     * as {@link Message} type. See {@link ActiveMQMailQueue} for more details
     * 
     * @param sizeTreshold
     */
    public void setSizeTreshold(long sizeTreshold) {
        this.sizeTreshold  = sizeTreshold;
    }
    
    
    
    /*
     * 
     * (non-Javadoc)
     * @see org.apache.james.queue.activemq.MailQueueFactory#getQueue(java.lang.String)
     */
    public synchronized MailQueue getQueue(String name) {
        MailQueue queue = queues.get(name);
        if (queue == null) {
            queue = new ActiveMQMailQueue(connectionFactory, name, sizeTreshold, log);
            queues.put(name, queue);
        }

        return queue;
    }



    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.LogEnabled#setLog(org.apache.commons.logging.Log)
     */
    public void setLog(Log log) {
        this.log = log;
    }

}
