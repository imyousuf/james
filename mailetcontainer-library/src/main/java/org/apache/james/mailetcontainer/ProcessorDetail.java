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

package org.apache.james.mailetcontainer;

import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.mailet.Mail;
import org.apache.mailet.Mailet;
import org.apache.mailet.Matcher;

/**
 * Wrapper which helps to expose JMX statistics for {@link MailProcessor} and {@link MailetContainer} implementations
 * 
 *
 */
public class ProcessorDetail extends StandardMBean implements MailProcessor, MailetContainer, ProcessorDetailMBean{
    private String processorName;
    private long slowestProcessing = -1;
    private long fastestProcessing = -1;
    private long successCount = 0;
    private long errorCount = 0;
    private MailProcessor processor;
    
    public ProcessorDetail(String processorName, MailProcessor processor) throws NotCompliantMBeanException {
        super(ProcessorDetailMBean.class);
        this.processorName = processorName;
        this.processor = processor;
    }
    

    /*
     * (non-Javadoc)
     * @see org.apache.james.transport.MailProcessor#service(org.apache.mailet.Mail)
     */
    public void service(Mail mail) throws MessagingException {
        try {
            long startProcessing = System.currentTimeMillis();

            processor.service(mail);
             
             long processTime = System.currentTimeMillis() - startProcessing;
             if (processTime > slowestProcessing) {
                 slowestProcessing = processTime;
             }
             if (fastestProcessing == -1 || fastestProcessing > processTime) {
                 fastestProcessing = processTime;
             }
             successCount++;
         } catch (MessagingException ex) {
             errorCount++;
             throw ex;
         }        
     }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.transport.MailetContainer#getMailets()
     */
    public List<Mailet> getMailets() {
        if (processor instanceof MailetContainer) {
            return ((MailetContainer) processor).getMailets();
        }
        return new ArrayList<Mailet>(); 
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.transport.MailetContainer#getMatchers()
     */
    public List<Matcher> getMatchers() {
        if (processor instanceof MailetContainer) {
            return ((MailetContainer) processor).getMatchers();
        }
        return new ArrayList<Matcher>();  
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.mailetcontainer.ProcessorDetailMBean#getHandledMailCount
     * ()
     */
    public long getHandledMailCount() {
        return getSuccessCount() + getErrorCount();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.mailetcontainer.ProcessorDetailMBean#getName()
     */
    public String getName() {
        return processorName;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.apache.james.mailetcontainer.ProcessorDetailMBean#
     * getFastestProcessing()
     */
    public long getFastestProcessing() {
        return fastestProcessing;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.apache.james.mailetcontainer.ProcessorDetailMBean#
     * getSlowestProcessing()
     */
    public long getSlowestProcessing() {
        return slowestProcessing;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.mailetcontainer.ProcessorDetailMBean#getErrorCount()
     */
    public long getErrorCount() {
        return errorCount;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.mailetcontainer.ProcessorDetailMBean#getSuccessCount
     * ()
     */
    public long getSuccessCount() {
        return successCount;
    }
}
