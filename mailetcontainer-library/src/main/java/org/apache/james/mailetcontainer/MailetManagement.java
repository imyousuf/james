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
import java.util.Iterator;
import java.util.List;

import javax.mail.MessagingException;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.mailet.Mail;
import org.apache.mailet.Mailet;
import org.apache.mailet.MailetConfig;

/**
 * Class which wraps a {@link Mailet} and expose statistics via JMX
 * 
 * 
 * 
 *
 */
public final class MailetManagement extends StandardMBean implements Mailet, MailetManagementMBean{

    private final Mailet mailet;
    private long errorCount = 0;
    private long successCount = 0;
    private long fastestProcessing = -1;
    private long slowestProcessing = -1;
    
    public MailetManagement(Mailet mailet) throws NotCompliantMBeanException {
        super(MailetManagementMBean.class);
        this.mailet = mailet;
        
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.mailet.Mailet#destroy()
     */
    public void destroy() {
        mailet.destroy();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.mailet.Mailet#getMailetConfig()
     */
    public MailetConfig getMailetConfig() {
        return mailet.getMailetConfig();
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.mailet.Mailet#getMailetInfo()
     */
    public String getMailetInfo() {
        return mailet.getMailetInfo();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.mailet.Mailet#init(org.apache.mailet.MailetConfig)
     */
    public void init(MailetConfig config) throws MessagingException {
        mailet.init(config);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.mailet.Mailet#service(org.apache.mailet.Mail)
     */
    public void service(Mail mail) throws MessagingException {
        long startProcessing = System.currentTimeMillis();
        try {
            mailet.service(mail);
            long processTime = System.currentTimeMillis() - startProcessing;
            if (processTime > slowestProcessing) {
                slowestProcessing = processTime;
            }
            if (fastestProcessing == -1 || fastestProcessing > processTime) {
                fastestProcessing = processTime;
            }
            successCount++;
        } catch (MessagingException e) {
            errorCount++;
            throw e;
        }
    }
    
    /**
     * Return the wrapped {@link Mailet}
     * 
     * @return mailet
     */
    public Mailet getMailet() {
        return mailet;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.MailetManagementMBean#getMailetName()
     */
    public String getMailetName() {
        return mailet.getMailetConfig().getMailetName();
    }

    /*
     * 
     */
    @SuppressWarnings("unchecked")
    public String[] getMailetParameters() {
        List<String> parameterList = new ArrayList<String>();
        MailetConfig mailetConfig = getMailet().getMailetConfig();
        Iterator<String> iterator = mailetConfig.getInitParameterNames();
        while (iterator.hasNext()) {
            String name = (String) iterator.next();
            String value = mailetConfig.getInitParameter(name);
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
        return errorCount;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.MailetManagementMBean#getFastestProcessing()
     */
    public long getFastestProcessing() {
        return fastestProcessing;
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
        return slowestProcessing;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.MailetManagementMBean#getSuccessCount()
     */
    public long getSuccessCount() {
        return successCount;
    }

}
