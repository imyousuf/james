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
package org.apache.james.mailetcontainer.lib.jmx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.james.mailetcontainer.api.jmx.MailetManagementMBean;
import org.apache.mailet.MailetConfig;

public final class MailetManagement extends StandardMBean implements MailetManagementMBean{

    private AtomicLong errorCount = new AtomicLong(0);
    private AtomicLong successCount = new AtomicLong(0);
    private AtomicLong fastestProcessing = new AtomicLong(-1);
    private AtomicLong slowestProcessing = new AtomicLong(-1);
    private final MailetConfig config;
    
    public MailetManagement(MailetConfig config) throws NotCompliantMBeanException {
        super(MailetManagementMBean.class);
        this.config = config;
        
    }
 
   
    public void update(long processTime, boolean success) {
        long fastest = fastestProcessing.get();
        
        if ( fastest > processTime || fastest == -1) {
            fastestProcessing.set(processTime);
        }
        
        if (slowestProcessing.get() < processTime) {
            slowestProcessing.set(processTime);
        }
        if (success) {
            successCount.incrementAndGet();
        } else {
            errorCount.incrementAndGet();
        }
    } 
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.MailetManagementMBean#getMailetName()
     */
    public String getMailetName() {
        return config.getMailetName();
    }

    /*
     * 
     */
    @SuppressWarnings("unchecked")
    public String[] getMailetParameters() {
        List<String> parameterList = new ArrayList<String>();
        Iterator<String> iterator = config.getInitParameterNames();
        while (iterator.hasNext()) {
            String name = (String) iterator.next();
            String value = config.getInitParameter(name);
            parameterList.add(name + "=" + value);
        }
        String[] result = (String[]) parameterList.toArray(new String[] {});
        return result;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.MailetManagementMBean#getErrorCount()
     */
    public long getErrorCount() {
        return errorCount.get();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.MailetManagementMBean#getFastestProcessing()
     */
    public long getFastestProcessing() {
        return fastestProcessing.get();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.MailetManagementMBean#getHandledMailCount()
     */
    public long getHandledMailCount() {
        return getErrorCount() + getSuccessCount();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.MailetManagementMBean#getSlowestProcessing()
     */
    public long getSlowestProcessing() {
        return slowestProcessing.get();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.MailetManagementMBean#getSuccessCount()
     */
    public long getSuccessCount() {
        return successCount.get();
    }

}
