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
package org.apache.james.smtpserver.jmx;

import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.apache.james.lifecycle.api.Disposable;
import org.apache.james.protocols.smtp.hook.HookReturnCode;

/**
 * JMX Bean which keep track of statistics for a given Hook
 * 
 *
 */
public class HookStats extends StandardMBean implements HookStatsMBean, Disposable {
    
    private AtomicLong ok = new AtomicLong(0);
    private AtomicLong declined = new AtomicLong(0);
    private AtomicLong deny = new AtomicLong(0);
    private AtomicLong denysoft = new AtomicLong(0);
    private AtomicLong all = new AtomicLong(0);

    private String name;
    private MBeanServer mbeanserver;
    private String hookname;

    public HookStats(String jmxName, String hookname) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, MalformedObjectNameException, NullPointerException {
        super(HookStatsMBean.class);
        this.hookname = hookname;
        name = "org.apache.james:type=server,name=" + jmxName + ",chain=handlerchain,handler=hook,hook=" + hookname;
        mbeanserver = ManagementFactory.getPlatformMBeanServer();
        ObjectName baseObjectName = new ObjectName(name);
        mbeanserver.registerMBean(this, baseObjectName);
    }
    
    public void increment(int code) {
        if ((code & HookReturnCode.OK) == HookReturnCode.OK) {
            ok.incrementAndGet();
        }
        if ((code & HookReturnCode.DECLINED) == HookReturnCode.DECLINED) {
            declined.incrementAndGet();
        }
        if ((code & HookReturnCode.DENYSOFT) == HookReturnCode.DENYSOFT) {
            denysoft.incrementAndGet();
        }
        if ((code & HookReturnCode.DENY) == HookReturnCode.DENY) {
            deny.incrementAndGet();
        }
       
        all.incrementAndGet();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.smtpserver.HookStatsMBean#getOk()
     */
    public long getOk() {
        return ok.get();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.smtpserver.HookStatsMBean#getDeclined()
     */
    public long getDeclined() {
        return declined.get();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.smtpserver.HookStatsMBean#getDeny()
     */
    public long getDeny() {
        return deny.get();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.smtpserver.HookStatsMBean#getDenysoft()
     */
    public long getDenysoft() {
        return denysoft.get();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.Disposable#dispose()
     */
    public void dispose() {
        try {
            mbeanserver.unregisterMBean(new ObjectName(name));
        } catch (Exception e) {
            // ignore here;
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.smtpserver.HookStatsMBean#getName()
     */
    public String getName() {
        return hookname;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.smtpserver.HookStatsMBean#getAll()
     */
    public long getAll() {
        return all.get();
    }
}
