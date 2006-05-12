/***********************************************************************
 * Copyright (c) 1999-2006 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.transport;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.services.SpoolRepository;
import org.apache.mailet.Mail;
import org.apache.mailet.MailetException;

import javax.mail.MessagingException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Manages the mail spool.  This class is responsible for retrieving
 * messages from the spool, directing messages to the appropriate
 * processor, and removing them from the spool when processing is
 * complete.
 *
 * @version CVS $Revision$ $Date$
 */
public class JamesSpoolManager
    extends AbstractLogEnabled
    implements Serviceable, Configurable, Initializable, Runnable, Disposable {

    /**
     * System component manager
     */
    private ServiceManager compMgr;

    /**
     * The configuration object used by this spool manager.
     */
    private Configuration conf;

    /**
     * The spool that this manager will process
     */
    private SpoolRepository spool;

    /**
     * The map of processor names to processors
     */
    private HashMap processors;

    /**
     * The number of threads used to move mail through the spool.
     */
    private int numThreads;

    /**
     * The ThreadPool containing worker threads.
     *
     * This used to be used, but for threads that lived the entire
     * lifespan of the application.  Currently commented out.  In
     * the future, we could use a thread pool to run short-lived
     * workers, so that we have a smaller number of readers that
     * accept a message from the spool, and dispatch to a pool of
     * worker threads that process the message.
     */
    // private ThreadPool workerPool;

    /**
     * The ThreadManager from which the thread pool is obtained.
     */
    // private ThreadManager threadManager;

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
    private Collection spoolThreads;

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager)
     */
    public void service(ServiceManager comp) throws ServiceException {
        // threadManager = (ThreadManager) comp.lookup(ThreadManager.ROLE);
        compMgr = comp;
        spool = (SpoolRepository) compMgr.lookup(SpoolRepository.ROLE);
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration conf) throws ConfigurationException {
        this.conf = conf;
        numThreads = conf.getChild("threads").getValueAsInteger(1);
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {

        getLogger().info("JamesSpoolManager init...");

        //A processor is a Collection of
        processors = new HashMap();

        final Configuration[] processorConfs = conf.getChildren( "processor" );
        for ( int i = 0; i < processorConfs.length; i++ )
        {
            Configuration processorConf = processorConfs[i];
            String processorName = processorConf.getAttribute("name");
            try {
                LinearProcessor processor = new LinearProcessor();
                processors.put(processorName, processor);
                
                setupLogger(processor, processorName);
                ContainerUtil.service(processor, compMgr);
                ContainerUtil.configure(processor, processorConf);
                
                if (getLogger().isInfoEnabled()) {
                    StringBuffer infoBuffer =
                        new StringBuffer(64)
                                .append("Processor ")
                                .append(processorName)
                                .append(" instantiated.");
                    getLogger().info(infoBuffer.toString());
                }
            } catch (Exception ex) {
                if (getLogger().isErrorEnabled()) {
                    StringBuffer errorBuffer =
                       new StringBuffer(256)
                               .append("Unable to init processor ")
                               .append(processorName)
                               .append(": ")
                               .append(ex.toString());
                    getLogger().error( errorBuffer.toString(), ex );
                }
                throw ex;
            }
        }
        if (getLogger().isInfoEnabled()) {
            StringBuffer infoBuffer =
                new StringBuffer(64)
                    .append("Spooler Manager uses ")
                    .append(numThreads)
                    .append(" Thread(s)");
            getLogger().info(infoBuffer.toString());
        }

        active = true;
        numActive = 0;
        spoolThreads = new java.util.ArrayList(numThreads);
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

        if (getLogger().isInfoEnabled())
        {
            getLogger().info("Run JamesSpoolManager: "
                             + Thread.currentThread().getName());
            getLogger().info("Spool=" + spool.getClass().getName());
        }

        numActive++;
        while(active) {
            String key = null;
            try {
                Mail mail = (Mail)spool.accept();
                key = mail.getName();
                if (getLogger().isDebugEnabled()) {
                    StringBuffer debugBuffer =
                        new StringBuffer(64)
                                .append("==== Begin processing mail ")
                                .append(mail.getName())
                                .append("====");
                    getLogger().debug(debugBuffer.toString());
                }
                process(mail);
                // Only remove an email from the spool is processing is
                // complete, or if it has no recipients
                if ((Mail.GHOST.equals(mail.getState())) ||
                    (mail.getRecipients() == null) ||
                    (mail.getRecipients().size() == 0)) {
                    spool.remove(key);
                    if (getLogger().isDebugEnabled()) {
                        StringBuffer debugBuffer =
                            new StringBuffer(64)
                                    .append("==== Removed from spool mail ")
                                    .append(mail.getName())
                                    .append("====");
                        getLogger().debug(debugBuffer.toString());
                    }
                }
                else {
                    // spool.remove() has a side-effect!  It unlocks the
                    // message so that other threads can work on it!  If
                    // we don't remove it, we must unlock it!
                    spool.unlock(key);
                }
                mail = null;
            } catch (InterruptedException ie) {
                getLogger().info("Interrupted JamesSpoolManager: " + Thread.currentThread().getName());
            } catch (Throwable e) {
                if (getLogger().isErrorEnabled()) {
                    getLogger().error("Exception processing " + key + " in JamesSpoolManager.run "
                                      + e.getMessage(), e);
                }
                /* Move the mail to ERROR state?  If we do, it could be
                 * deleted if an error occurs in the ERROR processor.
                 * Perhaps the answer is to resolve that issue by
                 * having a special state for messages that are not to
                 * be processed, but aren't to be deleted?  The message
                 * would already be in the spool, but would not be
                 * touched again.
                if (mail != null) {
                    try {
                        mail.setState(Mail.ERROR);
                        spool.store(mail);
                    }
                }
                */
            }
        }
        if (getLogger().isInfoEnabled())
        {
            getLogger().info("Stop JamesSpoolManager: " + Thread.currentThread().getName());
        }
        numActive--;
    }

    /**
     * Process this mail message by the appropriate processor as designated
     * in the state of the Mail object.
     *
     * @param mail the mail message to be processed
     */
    protected void process(Mail mail) {
        while (true) {
            String processorName = mail.getState();
            if (processorName.equals(Mail.GHOST)) {
                //This message should disappear
                return;
            }
            try {
                LinearProcessor processor
                    = (LinearProcessor)processors.get(processorName);
                if (processor == null) {
                    StringBuffer exceptionMessageBuffer =
                        new StringBuffer(128)
                            .append("Unable to find processor ")
                            .append(processorName)
                            .append(" requested for processing of ")
                            .append(mail.getName());
                    String exceptionMessage = exceptionMessageBuffer.toString();
                    getLogger().debug(exceptionMessage);
                    mail.setState(Mail.ERROR);
                    throw new MailetException(exceptionMessage);
                }
                StringBuffer logMessageBuffer = null;
                if (getLogger().isDebugEnabled()) {
                    logMessageBuffer =
                        new StringBuffer(64)
                                .append("Processing ")
                                .append(mail.getName())
                                .append(" through ")
                                .append(processorName);
                    getLogger().debug(logMessageBuffer.toString());
                }
                processor.service(mail);
                if (getLogger().isDebugEnabled()) {
                    logMessageBuffer =
                        new StringBuffer(128)
                                .append("Processed ")
                                .append(mail.getName())
                                .append(" through ")
                                .append(processorName);
                    getLogger().debug(logMessageBuffer.toString());
                    getLogger().debug("Result was " + mail.getState());
                }
                return;
            } catch (Throwable e) {
                // This is a strange error situation that shouldn't ordinarily
                // happen
                StringBuffer exceptionBuffer = 
                    new StringBuffer(64)
                            .append("Exception in processor <")
                            .append(processorName)
                            .append(">");
                getLogger().error(exceptionBuffer.toString(), e);
                if (processorName.equals(Mail.ERROR)) {
                    // We got an error on the error processor...
                    // kill the message
                    mail.setState(Mail.GHOST);
                    mail.setErrorMessage(e.getMessage());
                } else {
                    //We got an error... send it to the requested processor
                    if (!(e instanceof MessagingException)) {
                        //We got an error... send it to the error processor
                        mail.setState(Mail.ERROR);
                    }
                    mail.setErrorMessage(e.getMessage());
                }
            }
            if (getLogger().isErrorEnabled()) {
                StringBuffer logMessageBuffer =
                    new StringBuffer(128)
                            .append("An error occurred processing ")
                            .append(mail.getName())
                            .append(" through ")
                            .append(processorName);
                getLogger().error(logMessageBuffer.toString());
                getLogger().error("Result was " + mail.getState());
            }
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
     * @throws Exception if an error is encountered during shutdown
     */
    public void dispose() {
        getLogger().info("JamesSpoolManager dispose...");
        active = false; // shutdown the threads
        for (Iterator it = spoolThreads.iterator(); it.hasNext(); ) {
            ((Thread) it.next()).interrupt(); // interrupt any waiting accept() calls.
        }

        long stop = System.currentTimeMillis() + 60000;
        // give the spooler threads one minute to terminate gracefully
        while (numActive != 0 && stop > System.currentTimeMillis()) {
            try {
                Thread.sleep(1000);
            } catch (Exception ignored) {}
        }
        getLogger().info("JamesSpoolManager thread shutdown completed.");

        Iterator it = processors.keySet().iterator();
        while (it.hasNext()) {
            String processorName = (String)it.next();
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Processor " + processorName);
            }
            LinearProcessor processor = (LinearProcessor)processors.get(processorName);
            processor.dispose();
            processors.remove(processor);
        }
    }

}
