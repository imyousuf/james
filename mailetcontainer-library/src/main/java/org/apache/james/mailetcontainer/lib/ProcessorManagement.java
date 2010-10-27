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

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Iterator;
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
import org.apache.james.mailetcontainer.api.MailProcessor;
import org.apache.james.mailetcontainer.api.MailProcessorList;
import org.apache.james.mailetcontainer.api.MailetContainer;
import org.apache.james.mailetcontainer.api.ProcessorManagementMBean;
import org.apache.james.mailetcontainer.lib.matchers.CompositeMatcher;
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
    
    /**
     * Unregister all JMX MBeans
     */
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


    /**
     * Register all JMX MBeans
     * 
     * @throws NotCompliantMBeanException
     */
    private void registerMBeans() throws NotCompliantMBeanException {
       
        String baseObjectName = "org.apache.james:type=component,name=processor,";

        String[] processorNames = getProcessorNames();
        for (int i = 0; i < processorNames.length; i++) {
            String processorName = processorNames[i];
            registerProcessorMBean(baseObjectName, processorName);
        }
    }

    /**
     * Register a JMX MBean for a {@link MailProcessor}
     * 
     * @param baseObjectName
     * @param processorName
     * @throws NotCompliantMBeanException
     */
    private void registerProcessorMBean(String baseObjectName, String processorName) throws NotCompliantMBeanException {
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
        registerMBean(processorMBeanName, processorDetail);


        // check if the processor holds Mailets and Matchers
        if (processor instanceof MailetContainer) {
            MailetContainer container = (MailetContainer) processor;
            registerMailets(processorMBeanName, container.getMailets().iterator());
            registerMatchers(processorMBeanName, container.getMatchers().iterator(), 0);
           
        }
       

    }
    
    /**
     * Register the Mailets as JMX MBeans
     * 
     * @param parentMBeanName
     * @param mailets
     * @throws NotCompliantMBeanException
     */
    private void registerMailets(String parentMBeanName, Iterator<Mailet> mailets) throws NotCompliantMBeanException {
        int i = 0;
        while(mailets.hasNext()) {
            
            MailetManagement mailetManagement;

            Mailet mailet = mailets.next();
            
            // check if the mailet is an instance of MailetManagement. If not create a wrapper around it. This will give us not all
            // statistics but at least a few of them
            if (mailet instanceof MailetManagement) {
                mailetManagement = (MailetManagement) mailet;
            } else {
                mailetManagement = new MailetManagement(mailet);
            }
            String mailetMBeanName = parentMBeanName + ",subtype=mailet,index=" + (i++) + ",mailetname=" + mailetManagement.getMailetName();
            registerMBean(mailetMBeanName, mailetManagement);
        }
        
    }
    

    /**
     * Register the {@link Matcher}'s as JMX MBeans
     * 
     * @param parentMBeanName
     * @param matchers
     * @param nestingLevel
     * @throws NotCompliantMBeanException
     */
    @SuppressWarnings("unchecked")
    private void registerMatchers(String parentMBeanName, Iterator<Matcher> matchers, int nestingLevel) throws NotCompliantMBeanException {
        // current level
        int currentLevel = nestingLevel;
        int i = 0;

        while (matchers.hasNext()) {
            MatcherManagement matcherManagement;
            Matcher matcher = matchers.next();
            
            // check if the matcher is an instance of MatcherManagement. If not create a wrapper around it. This will give us not all
            // statistics but at least a few of them
            if (matcher instanceof MatcherManagement) {
               matcherManagement = (MatcherManagement) matcher;
            } else {
                matcherManagement = new MatcherManagement(matcher);
            }

            String matcherMBeanName = parentMBeanName + ",subtype" + currentLevel +"=matcher,index" + currentLevel+"=" + (i++) + ",matchername" + currentLevel+"=" + matcherManagement.getMatcherName();
            registerMBean(matcherMBeanName, matcherManagement);
            
            Matcher wrappedMatcher = getWrappedMatcher(matcherManagement);
            
            // Handle CompositeMatcher which were added by JAMES-948
            if (wrappedMatcher instanceof CompositeMatcher) {
                // we increment the nesting as we have one more child level and register the child matchers
                registerMatchers(matcherMBeanName, ((CompositeMatcher) wrappedMatcher).iterator(), ++nestingLevel);
            }
        }
    }

    private void registerMBean(String mBeanName, Object object) {
        ObjectName objectName = null;
        try {
            objectName = new ObjectName(mBeanName);
        } catch (MalformedObjectNameException e) {
            logger.info("Unable to register mbean", e);

            return;
        }
        try {
            mbeanserver.registerMBean(object, objectName);
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
    

    
    private Matcher getWrappedMatcher(Matcher matcher) {
        // call recursive to find the real Matcher
        if (matcher instanceof MatcherManagement) {
            return getWrappedMatcher(((MatcherManagement)matcher).getMatcher());
        }
        return matcher;
        
    }

}
