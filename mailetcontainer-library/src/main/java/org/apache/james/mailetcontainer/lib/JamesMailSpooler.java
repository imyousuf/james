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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.lifecycle.api.Configurable;
import org.apache.james.lifecycle.api.LifecycleUtil;
import org.apache.james.lifecycle.api.LogEnabled;
import org.apache.james.mailetcontainer.api.MailProcessor;
import org.apache.james.mailetcontainer.api.jmx.MailSpoolerMBean;
import org.apache.james.queue.api.MailQueue;
import org.apache.james.queue.api.MailQueue.MailQueueItem;
import org.apache.james.queue.api.MailQueueFactory;
import org.apache.mailet.Mail;

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
public class JamesMailSpooler implements Runnable, Configurable, LogEnabled, MailSpoolerMBean {

    
    private MailQueue queue;

    /**
     * The number of threads used to move mail through the spool.
     */
    private int numThreads;

  
    /**
     * Number of active threads
     */
    private AtomicInteger numActive = new AtomicInteger(0);;

    private AtomicInteger processingActive = new AtomicInteger(0);;

    
    /**
     * Spool threads are active
     */
    private AtomicBoolean active = new AtomicBoolean(false);

    /**
     * Spool threads
     */
    private Collection<Thread> spoolThreads;

    /**
     * The mail processor 
     */
    private MailProcessor mailProcessor;

    private Log logger;

    private MailQueueFactory queueFactory;


    @Resource(name="mailQueueFactory")
    public void setMailQueueFactory(MailQueueFactory queueFactory) {
        this.queueFactory = queueFactory;
    }

    @Resource(name="mailprocessor")
    public void setMailProcessor(MailProcessor mailProcessor) {
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
        logger.info(getClass().getName() + " init...");
        
        queue = queueFactory.getQueue(MailQueueFactory.SPOOL);

        if (logger.isInfoEnabled()) {
            StringBuffer infoBuffer =
                new StringBuffer(64)
                    .append(getClass().getName())
                    .append(" uses ")
                    .append(numThreads)
                    .append(" Thread(s)");
            logger.info(infoBuffer.toString());
        }

        active.set(true);
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
            logger.info("Run " + getClass().getName() +": "
                             + Thread.currentThread().getName());
            logger.info("Queue=" + queue.toString());
        }

        while(active.get()) {
            numActive.incrementAndGet();

            try {
                MailQueueItem queueItem = queue.deQueue();
                
                // increase count
                processingActive.incrementAndGet();
                
                Mail mail = queueItem.getMail();
                if (logger.isDebugEnabled()) {
                    StringBuffer debugBuffer =
                        new StringBuffer(64)
                                .append("==== Begin processing mail ")
                                .append(mail.getName())
                                .append("====");
                    logger.debug(debugBuffer.toString());
                }

                try {
                    mailProcessor.service(mail);
                    queueItem.done(true);
                } catch (Exception e) {
                    if (active.get() && logger.isErrorEnabled()) {
                        logger.error("Exception processing mail while spooling " + e.getMessage(), e);
                    }
                    queueItem.done(false);

                } finally {
                    LifecycleUtil.dispose(mail);
                    mail = null;
                }
               
            } catch (Throwable e) {
                if (active.get() && logger.isErrorEnabled()) {
                    logger.error("Exception processing mail while spooling " + e.getMessage(), e);

                }
            } finally {
                processingActive.decrementAndGet();
                numActive.decrementAndGet();
            }

        }
        if (logger.isInfoEnabled()){
            logger.info("Stop " +  getClass().getName() +": " + Thread.currentThread().getName());
        }
    }

    /**
     * The dispose operation is called at the end of a components lifecycle.
     * Instances of this class use this method to release and destroy any
     * resources that they own.
     *
     * This implementation shuts down the LinearProcessors managed by this
     * JamesSpoolManager
     * 
     * @see org.apache.james.lifecycle.api.avalon.framework.activity.Disposable#dispose()
     */
    @PreDestroy
    public void dispose() {
        logger.info(getClass().getName() +" dispose...");
        active.set(false); // shutdown the threads
        for (Thread thread: spoolThreads) {
            thread.interrupt(); // interrupt any waiting accept() calls.
        }

        long stop = System.currentTimeMillis() + 60000;
        // give the spooler threads one minute to terminate gracefully      
        while (numActive.get() != 0 && stop > System.currentTimeMillis()) {
            try {
                Thread.sleep(1000);
            } catch (Exception ignored) {}
        }
        
        logger.info(getClass().getName() +" thread shutdown completed.");
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.LogEnabled#setLog(org.apache.commons.logging.Log)
     */
    public void setLog(Log log) {
        this.logger = log;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.api.MailSpoolerMBean#getThreadCount()
     */
    public int getThreadCount() {
        return numThreads;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.api.MailSpoolerMBean#getCurrentSpoolCount()
     */
    public int getCurrentSpoolCount() {
        return processingActive.get();
    }
}
