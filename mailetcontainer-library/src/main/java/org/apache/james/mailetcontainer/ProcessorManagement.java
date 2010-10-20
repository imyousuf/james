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

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.apache.commons.logging.Log;
import org.apache.james.lifecycle.LogEnabled;
import org.apache.mailet.Mailet;
import org.apache.mailet.Matcher;

/**
 * Expose JMX functions for {@link MailProcessorList} implementations
 * 
 *
 */
public class ProcessorManagement extends StandardMBean implements ProcessorManagementMBean, LogEnabled{

    private MailProcessorList mailProcessor;
    private MBeanServer mbeanserver;
    private Log logger;
    private List<ObjectName> mbeans = new ArrayList<ObjectName>();
    
    public ProcessorManagement() throws NotCompliantMBeanException {
        super(ProcessorManagementMBean.class);
    }
    @Resource(name="mailProcessor")
    public void setMailProcessorList(MailProcessorList mailProcessor) {
        this.mailProcessor = mailProcessor;
    }
    
   
    @PostConstruct
    public void init() throws Exception {
        mbeanserver = ManagementFactory.getPlatformMBeanServer();
        
        registerMBeans();
    }
    
    @PreDestroy
    public void destory() {
        unregisterMBeans();
    }
    
    private void unregisterMBeans() {
        List<ObjectName> unregistered = new ArrayList<ObjectName>();
        for (int i = 0; i < mbeans.size(); i++) {
            ObjectName name = mbeans.get(i);
            
            try {
                mbeanserver.unregisterMBean(name);
                unregistered.add(name);
            } catch (javax.management.JMException e) {
                logger.error("Unable to unregister mbean " + name, e);
            }
        }
        mbeans.removeAll(unregistered);
    }


    private void registerMBeans() throws NotCompliantMBeanException {
       
        String baseObjectName = "org.apache.james:type=component,name=processor,";

        String[] processorNames = getProcessorNames();
        for (int i = 0; i < processorNames.length; i++) {
            String processorName = processorNames[i];
            createProcessorMBean(baseObjectName, processorName, mbeanserver);
            continue;
        }
    }

    private void createProcessorMBean(String baseObjectName, String processorName, MBeanServer mBeanServer) throws NotCompliantMBeanException {
        String processorMBeanName = baseObjectName + "processor=" + processorName;
        
        MailProcessor processor = mailProcessor.getProcessor(processorName);
        ProcessorDetail processorDetail;
        
        // check if the processor is an instance of ProcessorDetail. If not create a wrapper around it. This will give us not all
        // statistics but at least a few of them
        if (processor instanceof ProcessorDetail) {
            processorDetail = (ProcessorDetail) processor;
        } else {
            processorDetail = new ProcessorDetail(processorName, processor);
        }
        registerMBean(mBeanServer, processorMBeanName, processorDetail);


        // check if the processor holds Mailets and Matchers
        if (processor instanceof MailetContainer) {
            MailetContainer container = (MailetContainer) processor;
            List<Mailet> mailets =  container.getMailets();
         
            for (int i = 0; i < mailets.size(); i++) {
                MailetManagement mailetManagement;

                Mailet mailet = mailets.get(i);
                
                // check if the mailet is an instance of MailetManagement. If not create a wrapper around it. This will give us not all
                // statistics but at least a few of them
                if (mailet instanceof MailetManagement) {
                    mailetManagement = (MailetManagement) mailet;
                } else {
                    mailetManagement = new MailetManagement(mailet);
                }
                String mailetMBeanName = processorMBeanName + ",subtype=mailet,index=" + (i+1) + ",mailetname=" + mailetManagement.getMailetName();
                registerMBean(mBeanServer, mailetMBeanName, mailetManagement);
            }

            List<Matcher> matchers =  container.getMatchers();
            for (int i = 0; i < matchers.size(); i++) {
                MatcherManagement matcherManagement;
                Matcher matcher = matchers.get(i);
                
                // check if the matcher is an instance of MatcherManagement. If not create a wrapper around it. This will give us not all
                // statistics but at least a few of them
                if (matcher instanceof MatcherManagement) {
                   matcherManagement = (MatcherManagement) matcher;
                } else {
                    matcherManagement = new MatcherManagement(matcher);
                }

                String matcherMBeanName = processorMBeanName + ",subtype=matcher,index=" + (i+1) + ",matchername=" + matcherManagement.getMatcherName();

                registerMBean(mBeanServer, matcherMBeanName, matcherManagement);
            }
           
        }
       

    }

    private void registerMBean(MBeanServer mBeanServer, String mBeanName, Object object) {
        ObjectName objectName = null;
        try {
            objectName = new ObjectName(mBeanName);
        } catch (MalformedObjectNameException e) {
            logger.info("Unable to register mbean", e);

            return;
        }
        try {
            mBeanServer.registerMBean(object, objectName);
            mbeans.add(objectName);
        } catch (javax.management.JMException e) {
            logger.error("Unable to register mbean", e);
        }
    }

    

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.ProcessorManagementMBean#getProcessorNames()
     */
    public String[] getProcessorNames() {
        return mailProcessor.getProcessorNames();
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.LogEnabled#setLog(org.apache.commons.logging.Log)
     */
    public void setLog(Log logger) {
        this.logger = logger;
    }

}
