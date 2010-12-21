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

import java.util.concurrent.atomic.AtomicLong;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.james.mailetcontainer.api.jmx.MatcherManagementMBean;
import org.apache.mailet.MatcherConfig;


public final class MatcherManagement extends StandardMBean implements MatcherManagementMBean{
    private MatcherConfig matcherConfig;
    private AtomicLong errorCount = new AtomicLong(0);
    private AtomicLong successCount = new AtomicLong(0);
    private AtomicLong fastestProcessing = new AtomicLong(-1);
    private AtomicLong slowestProcessing = new AtomicLong(-1);
    private AtomicLong matchedCount = new AtomicLong(0);
    private AtomicLong notMatchedCount = new AtomicLong(0);
    
    public MatcherManagement(MatcherConfig matcherConfig) throws NotCompliantMBeanException {
        super(MatcherManagementMBean.class);
        this.matcherConfig = matcherConfig;

    }


    public void update(long processTime, boolean success, boolean matched) {
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
        if (matched) {
            matchedCount.incrementAndGet();
        } else {
            notMatchedCount.incrementAndGet();
        }
        
    } 
    
    
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.MatcherManagementMBean#getMatcherName()
     */
    public String getMatcherName() {
        return matcherConfig.getMatcherName();
    }
    
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.MatcherManagementMBean#getMatcherCondition()
     */
    public String getMatcherCondition() {
        return matcherConfig.getCondition();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.MailProcessingMBean#getErrorCount()
     */
    public long getErrorCount() {
        return errorCount.get();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.MailProcessingMBean#getFastestProcessing()
     */
    public long getFastestProcessing() {
        return fastestProcessing.get();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.MailProcessingMBean#getHandledMailCount()
     */
    public long getHandledMailCount() {
        return getSuccessCount() + getErrorCount();

    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.MailProcessingMBean#getSlowestProcessing()
     */
    public long getSlowestProcessing() {
        return slowestProcessing.get();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.MailProcessingMBean#getSuccessCount()
     */
    public long getSuccessCount() {
        return successCount.get();
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.MatcherManagementMBean#getMatchedRecipientCount()
     */
    public long getMatchedRecipientCount() {
        return matchedCount.get();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.MatcherManagementMBean#getNotMatchedRecipientCount()
     */
    public long getNotMatchedRecipientCount() {
        return notMatchedCount.get();
    }
}

