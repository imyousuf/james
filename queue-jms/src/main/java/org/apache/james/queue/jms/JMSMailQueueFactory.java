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
package org.apache.james.queue.jms;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.jms.ConnectionFactory;

import org.apache.commons.logging.Log;
import org.apache.james.lifecycle.LogEnabled;
import org.apache.james.queue.api.MailQueue;
import org.apache.james.queue.api.MailQueueFactory;

/**
 * {@link MailQueueFactory} implementation which use JMS
 * 
 *
 */
public class JMSMailQueueFactory implements MailQueueFactory, LogEnabled{

    
    
    private final Map<String, MailQueue> queues = new HashMap<String, MailQueue>();
    protected ConnectionFactory connectionFactory;
    protected Log log;
    
    @Resource(name="jmsConnectionFactory")
    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }
    
    /*
     * 
     * (non-Javadoc)
     * @see org.apache.james.queue.activemq.MailQueueFactory#getQueue(java.lang.String)
     */
    public synchronized final MailQueue getQueue(String name) {
        MailQueue queue = queues.get(name);
        if (queue == null) {
            queue = createMailQueue(name);
            queues.put(name, queue);
        }

        return queue;
    }


    /**
     * Create a {@link MailQueue} for the given name
     * 
     * @param name
     * @return queue
     */
    protected MailQueue createMailQueue(String name) {
    	return new JMSMailQueue(connectionFactory, name, log);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.LogEnabled#setLog(org.apache.commons.logging.Log)
     */
    public void setLog(Log log) {
        this.log = log;
    }
}