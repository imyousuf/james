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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import org.apache.james.queue.api.MailQueueManagementMBean;
import org.apache.james.queue.api.ManageableMailQueue;
import org.apache.james.queue.api.MailQueue.MailQueueException;
import org.apache.james.queue.api.ManageableMailQueue.MailQueueIterator;
import org.apache.james.queue.api.ManageableMailQueue.Type;
import org.apache.mailet.Mail;

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
    public long clear() throws Exception{
        try {
            return queue.clear();
        } catch (MailQueueException e) {
            throw new Exception(e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.queue.api.MailQueueManagementMBean#flush()
     */
    public long flush() throws Exception {
        try {
            return queue.flush();
        } catch (MailQueueException e) {
            throw new Exception(e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.queue.api.MailQueueManagementMBean#getSize()
     */
    public long getSize() throws Exception {
        try {
            return queue.getSize();
        } catch (MailQueueException e) {
            throw new Exception(e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.queue.api.MailQueueManagementMBean#removeWithName(java.lang.String)
     */
    public long removeWithName(String name) throws Exception {
        try {
            return queue.remove(Type.Name, name);
        } catch (MailQueueException e) {
            throw new Exception(e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.queue.api.MailQueueManagementMBean#removeWithRecipient(java.lang.String)
     */
    public long removeWithRecipient(String address) throws Exception {
        try {
            return queue.remove(Type.Recipient, address);
        } catch (MailQueueException e) {
            throw new Exception(e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.queue.api.MailQueueManagementMBean#removeWithSender(java.lang.String)
     */
    public long removeWithSender(String address) throws Exception {
        try {
            return queue.remove(Type.Sender, address);
        } catch (MailQueueException e) {
            throw new Exception(e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.queue.api.MailQueueManagementMBean#browse()
     */
    public List<CompositeData> browse() throws Exception {
        MailQueueIterator it = queue.browse();
        List<CompositeData> data = new ArrayList<CompositeData>();
        while(it.hasNext()) {
            Mail m = it.next();
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("name", m.getName());
            map.put("sender", m.getSender());
            map.put("state", m.getState());
            CompositeDataSupport c = new CompositeDataSupport(new CompositeType(Mail.class.getName(), "Mail", new String[] {"name", "sender", "state"}, new String[] {"The name of the Mail", "The sender of the Mail", "The state of the Mail"}, new OpenType[] { SimpleType.STRING, SimpleType.STRING, SimpleType.STRING}), map);
            data.add(c);
        }
        it.close();
        return data;
    }
    
    
}
