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
package org.apache.james.mailetcontainer.lib;

import java.util.Collection;

import javax.mail.MessagingException;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.james.mailetcontainer.api.MatcherManagementMBean;
import org.apache.mailet.Mail;
import org.apache.mailet.Matcher;
import org.apache.mailet.MatcherConfig;

/**
 * JMX MBean which will collection statistics for the wrapped {@link Matcher}
 * 
 *
 */
public final class MatcherManagement extends StandardMBean implements MatcherManagementMBean, Matcher{
    private Matcher matcher;
    private long slowestProcessing = -1;
    private long fastestProcessing = -1;
    private long successCount = 0;
    private long errorCount = 0;
    private long matched = 0;
    private long notMatched = 0;
    
    public MatcherManagement(Matcher matcher) throws NotCompliantMBeanException {
        super(MatcherManagementMBean.class);
        this.matcher = matcher;

    }

    /**
     * Return the wrapped {@link Matcher}
     * 
     * @return wrappedMatcher
     */
    public Matcher getMatcher() {
        return matcher;
    }
    
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.MatcherManagementMBean#getMatcherName()
     */
    public String getMatcherName() {
        return matcher.getMatcherConfig().getMatcherName();
    }
    
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.MatcherManagementMBean#getMatcherCondition()
     */
    public String getMatcherCondition() {
        return matcher.getMatcherConfig().getCondition();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.MailProcessingMBean#getErrorCount()
     */
    public long getErrorCount() {
        return errorCount;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.MailProcessingMBean#getFastestProcessing()
     */
    public long getFastestProcessing() {
        return fastestProcessing;
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
        return slowestProcessing;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.MailProcessingMBean#getSuccessCount()
     */
    public long getSuccessCount() {
        return successCount;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.mailet.Matcher#destroy()
     */
    public void destroy() {
        matcher.destroy();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.mailet.Matcher#getMatcherConfig()
     */
    public MatcherConfig getMatcherConfig() {
        return matcher.getMatcherConfig();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.mailet.Matcher#getMatcherInfo()
     */
    public String getMatcherInfo() {
        return matcher.getMatcherInfo();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.mailet.Matcher#init(org.apache.mailet.MatcherConfig)
     */
    public void init(MatcherConfig config) throws MessagingException {
        matcher.init(config);
    }

    @SuppressWarnings("unchecked")
    public Collection match(Mail mail) throws MessagingException {
        try {
            long startProcessing = System.currentTimeMillis();
             Collection origRcpts = mail.getRecipients();
             Collection rcpts =  matcher.match(mail);
             
             long processTime = System.currentTimeMillis() - startProcessing;
             if (processTime > slowestProcessing) {
                 slowestProcessing = processTime;
             }
             if (fastestProcessing == -1 || fastestProcessing > processTime) {
                 fastestProcessing = processTime;
             }
             successCount++;
             
             long match = 0;
             if (rcpts != null) {
                  match = rcpts.size();
                  matched =+ match;
             }
             
             if (origRcpts != null) {
                 notMatched =+ origRcpts.size() - match;
             }
             return rcpts;
         } catch (MessagingException ex) {
             errorCount++;
             throw ex;
         }              
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.MatcherManagementMBean#getMatchedRecipientCount()
     */
    public long getMatchedRecipientCount() {
        return matched;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.MatcherManagementMBean#getNotMatchedRecipientCount()
     */
    public long getNotMatchedRecipientCount() {
        return notMatched;
    }
}

