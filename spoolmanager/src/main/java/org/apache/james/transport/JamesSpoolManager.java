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



package org.apache.james.transport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.mail.MessagingException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.lifecycle.Configurable;
import org.apache.james.lifecycle.LifecycleUtil;
import org.apache.james.lifecycle.LogEnabled;
import org.apache.james.queue.MailQueue;
import org.apache.james.queue.MailQueueFactory;
import org.apache.james.queue.MailQueue.DequeueOperation;
import org.apache.james.queue.MailQueue.MailQueueException;
import org.apache.james.services.SpoolManager;
import org.apache.mailet.Mail;
import org.apache.mailet.MailetConfig;
import org.apache.mailet.MatcherConfig;

/**
 * Manages the mail spool.  This class is responsible for retrieving
 * messages from the spool, directing messages to the appropriate
 * processor, and removing them from the spool when processing is
 * complete.
 *
 * @version CVS $Revision$ $Date$
 * 
 * TODO: We should better use a ExecutorService here and only spawn a new Thread if needed
 */
public class JamesSpoolManager implements Runnable, SpoolManager, Configurable, LogEnabled {

    
    private MailQueue queue;

    /**
     * The number of threads used to move mail through the spool.
     */
    private int numThreads;

  
    /**
     * Number of active threads
     */
    private int numActive;

    /**
     * Spool threads are active
     */
    private boolean active;

    /**
     * Spool threads
     */
    private Collection<Thread> spoolThreads;

    /**
     * The mail processor 
     */
    private MailProcessorList mailProcessor;

    private Log logger;

    private MailQueueFactory queueFactory;


    @Resource(name="mailQueueFactory")
    public void setMailQueueFactory(MailQueueFactory queueFactory) {
        this.queueFactory = queueFactory;
    }

    @Resource(name="mailProcessor")
    public void setMailProcessorList(MailProcessorList mailProcessor) {
        this.mailProcessor = mailProcessor;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.Configurable#configure(org.apache.commons.configuration.HierarchicalConfiguration)
     */
    public void configure(HierarchicalConfiguration config) throws ConfigurationException {
        numThreads = config.getInt("threads",100);
    }

    /**
     * Initialises the spool manager.
     */
    @PostConstruct
    public void init() throws Exception {
        logger.info("JamesSpoolManager init...");
        
        queue = queueFactory.getQueue(MailQueueFactory.SPOOL);

        if (logger.isInfoEnabled()) {
            StringBuffer infoBuffer =
                new StringBuffer(64)
                    .append("Spooler Manager uses ")
                    .append(numThreads)
                    .append(" Thread(s)");
            logger.info(infoBuffer.toString());
        }

        active = true;
        numActive = 0;
        spoolThreads = new java.util.ArrayList<Thread>(numThreads);
        for ( int i = 0 ; i < numThreads ; i++ ) {
            Thread reader = new Thread(this, "Spool Thread #" + i);
            spoolThreads.add(reader);
            reader.start();
        }
    }

    /**
     * This routinely checks the message spool for messages, and processes
     * them as necessary
     */
    public void run() {

        if (logger.isInfoEnabled()) {
            logger.info("Run JamesSpoolManager: "
                             + Thread.currentThread().getName());
            logger.info("Spool=" + queue.getClass().getName());
        }

        numActive++;
        while(active) {
            try {
                queue.deQueue(new DequeueOperation() {
                    
                    /*
                     * (non-Javadoc)
                     * @see org.apache.james.queue.MailQueue.DequeueOperation#process(org.apache.mailet.Mail)
                     */
                    public void process(Mail mail) throws MailQueueException, MessagingException {
                        if (logger.isDebugEnabled()) {
                            StringBuffer debugBuffer =
                                new StringBuffer(64)
                                        .append("==== Begin processing mail ")
                                        .append(mail.getName())
                                        .append("====");
                            logger.debug(debugBuffer.toString());
                        }

                        mailProcessor.service(mail);

                        if ((Mail.GHOST.equals(mail.getState())) ||
                            (mail.getRecipients() == null) ||
                            (mail.getRecipients().size() == 0)) {
                            LifecycleUtil.dispose(mail);
                            mail = null;
                        }                        
                    }
                });
                
               
            } catch (Throwable e) {
                if (logger.isErrorEnabled()) {
                    logger.error("Exception processing mail in JamesSpoolManager.run "
                                      + e.getMessage(), e);
                }
            }
        }
        if (logger.isInfoEnabled()){
            logger.info("Stop JamesSpoolManager: " + Thread.currentThread().getName());
        }
        numActive--;
    }

    /**
     * The dispose operation is called at the end of a components lifecycle.
     * Instances of this class use this method to release and destroy any
     * resources that they own.
     *
     * This implementation shuts down the LinearProcessors managed by this
     * JamesSpoolManager
     * 
     * @see org.apache.avalon.framework.activity.Disposable#dispose()
     */
    @PreDestroy
    public void dispose() {
        logger.info("JamesSpoolManager dispose...");
        active = false; // shutdown the threads
        for (Thread thread: spoolThreads) {
            thread.interrupt(); // interrupt any waiting accept() calls.
        }

        long stop = System.currentTimeMillis() + 60000;
        // give the spooler threads one minute to terminate gracefully
        while (numActive != 0 && stop > System.currentTimeMillis()) {
            try {
                Thread.sleep(1000);
            } catch (Exception ignored) {}
        }
        logger.info("JamesSpoolManager thread shutdown completed.");
    }

    /**
     * @see org.apache.james.services.SpoolManager#getProcessorNames()
     */
    public String[] getProcessorNames() {
        return mailProcessor.getProcessorNames();
    }

    /**
     * @see org.apache.james.services.SpoolManager#getMailetConfigs(java.lang.String)
     */
    public List<MailetConfig> getMailetConfigs(String processorName) {
        MailetContainer mailetContainer = getMailetContainerByName(processorName);
        if (mailetContainer == null) return new ArrayList<MailetConfig>();
        return mailetContainer.getMailetConfigs();
    }

    /**
     * @see org.apache.james.services.SpoolManager#getMatcherConfigs(java.lang.String)
     */
    public List<MatcherConfig> getMatcherConfigs(String processorName) {
        MailetContainer mailetContainer = getMailetContainerByName(processorName);
        if (mailetContainer == null) return new ArrayList<MatcherConfig>();
        return mailetContainer.getMatcherConfigs();
    }

    private MailetContainer getMailetContainerByName(String processorName) {        
        MailProcessor processor = mailProcessor.getProcessor(processorName);
        if (!(processor instanceof MailetContainer)) return null;
        // TODO: decide, if we have to visit all sub-processors for being ProcessorLists 
        // on their very own and collecting the processor names deeply.
        return (MailetContainer)processor;
    }



    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.LogEnabled#setLog(org.apache.commons.logging.Log)
     */
    public void setLog(Log log) {
        this.logger = log;
    }
}
