/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.transport;

import org.apache.avalon.cornerstone.services.threads.ThreadManager;
import org.apache.avalon.excalibur.thread.ThreadPool;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.component.DefaultComponentManager;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.core.MailImpl;
import org.apache.james.services.MailStore;
import org.apache.james.services.SpoolRepository;
import org.apache.mailet.*;

import javax.mail.MessagingException;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Manages the mail spool.  This class is responsible for retrieving
 * messages from the spool, directing messages to the appropriate
 * processor, and removing them from the spool when processing is
 * complete.
 *
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
 *
 * @version This is $Revision: 1.20 $
 */
public class JamesSpoolManager
    extends AbstractLogEnabled
    implements Composable, Configurable, Initializable,
               Runnable, Disposable, Component {

    /**
     * Whether 'deep debugging' is turned on.
     */
    private final static boolean DEEP_DEBUG = false;

    /**
     * System component manager
     */
    private DefaultComponentManager compMgr;

    /**
     * The configuration object used by this spool manager.
     */
    private Configuration conf;

    private SpoolRepository spool;

    private MailetContext mailetContext;

    /**
     * The map of processor names to processors
     */
    private HashMap processors;

    /**
     * The number of threads used to move mail through the spool.
     */
    private int numThreads;

    /**
     * The ThreadPool containing the spool threads.
     */
    private ThreadPool workerPool;

    /**
     * The ThreadManager from which the thread pool is obtained.
     */
    private ThreadManager threadManager;

    /**
     * @see org.apache.avalon.framework.component.Composable#compose(ComponentManager)
     */
    public void compose(ComponentManager comp)
        throws ComponentException {
        threadManager = (ThreadManager)comp.lookup( ThreadManager.ROLE );
        compMgr = new DefaultComponentManager(comp);
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
        workerPool = threadManager.getThreadPool( "default" );
        MailStore mailstore
            = (MailStore) compMgr.lookup("org.apache.james.services.MailStore");
        spool = mailstore.getInboundSpool();
        if (null == spool)
        {
            String exceptionMessage = "The mailstore's inbound spool is null.  The mailstore is misconfigured";
            if (getLogger().isErrorEnabled()) {
                getLogger().error( exceptionMessage );
            }
            throw new ConfigurationException(exceptionMessage);
        }
        if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
            getLogger().debug("Got spool");
        }

        mailetContext
            = (MailetContext) compMgr.lookup("org.apache.mailet.MailetContext");
        MailetLoader mailetLoader = new MailetLoader();
        MatchLoader matchLoader = new MatchLoader();
        try {
            mailetLoader.configure(conf.getChild("mailetpackages"));
            matchLoader.configure(conf.getChild("matcherpackages"));
            compMgr.put(Resources.MAILET_LOADER, mailetLoader);
            compMgr.put(Resources.MATCH_LOADER, matchLoader);
        } catch (ConfigurationException ce) {
            final String message =
                "Unable to configure mailet/matcher Loaders: "
                + ce.getMessage();

            if (getLogger().isErrorEnabled()) {
                getLogger().error( message, ce );
            }
            throw new RuntimeException( message );
        }

        //A processor is a Collection of
        processors = new HashMap();

        final Configuration[] processorConfs = conf.getChildren( "processor" );
        for ( int i = 0; i < processorConfs.length; i++ )
        {
            Configuration processorConf = processorConfs[i];
            String processorName = processorConf.getAttribute("name");
            try {
                LinearProcessor processor = new LinearProcessor();
                setupLogger(processor, processorName);
                processor.setSpool(spool);
                processor.initialize();
                processors.put(processorName, processor);

                // If this is the root processor, add the PostmasterAlias
                //  mailet silently to the top
                if (processorName.equals("root")) {
                    Matcher matcher = matchLoader.getMatcher("All",
                                                             mailetContext);
                    Mailet mailet = mailetLoader.getMailet("PostmasterAlias",
                                                           mailetContext, null);
                    processor.add(matcher, mailet);
                }

                final Configuration[] mailetConfs
                    = processorConf.getChildren( "mailet" );
                // Loop through the mailet configuration, load
                // all of the matcher and mailets, and add
                // them to the processor.
                for ( int j = 0; j < mailetConfs.length; j++ )
                {
                    Configuration c = mailetConfs[j];
                    String mailetClassName = c.getAttribute("class");
                    String matcherName = c.getAttribute("match");
                    Mailet mailet = null;
                    Matcher matcher = null;
                    try {
                        matcher = matchLoader.getMatcher(matcherName,
                                                         mailetContext);
                        //The matcher itself should log that it's been inited.
                        if (getLogger().isInfoEnabled()) {
                            StringBuffer infoBuffer =
                                new StringBuffer(64)
                                        .append("Matcher ")
                                        .append(matcherName)
                                        .append(" instantiated.");
                            getLogger().info(infoBuffer.toString());
                        }
                    } catch (MessagingException ex) {
                        // **** Do better job printing out exception
                        if (getLogger().isErrorEnabled()) {
                            StringBuffer errorBuffer =
                                new StringBuffer(256)
                                        .append("Unable to init matcher ")
                                        .append(matcherName)
                                        .append(": ")
                                        .append(ex.toString());
                            getLogger().error( errorBuffer.toString(), ex );
                        }
                        System.err.println("Unable to init matcher " + matcherName);
                        System.err.println("Check spool manager logs for more details.");
                        ex.printStackTrace();
                        //System.exit(1);
                        throw ex;
                    }
                    try {
                        mailet = mailetLoader.getMailet(mailetClassName,
                                                        mailetContext, c);
                        if (getLogger().isInfoEnabled()) {
                            StringBuffer infoBuffer =
                                new StringBuffer(64)
                                        .append("Mailet ")
                                        .append(mailetClassName)
                                        .append(" instantiated.");
                            getLogger().info(infoBuffer.toString());
                        }
                    } catch (MessagingException ex) {
                        // **** Do better job printing out exception
                        if (getLogger().isErrorEnabled()) {
                            StringBuffer errorBuffer =
                                new StringBuffer(256)
                                        .append("Unable to init mailet ")
                                        .append(mailetClassName)
                                        .append(": ")
                                        .append(ex.toString());
                            getLogger().error( errorBuffer.toString(), ex );
                        }
                        System.err.println("Unable to init mailet " + mailetClassName);
                        System.err.println("Check spool manager logs for more details.");
                        ex.printStackTrace();
                        //System.exit(1);
                        throw ex;
                    }
                    //Add this pair to the processor
                    processor.add(matcher, mailet);
                }

                // Close the processor matcher/mailet lists.
                //
                // Please note that this is critical to the proper operation
                // of the LinearProcessor code.  The processor will not be
                // able to service mails until this call is made.
                processor.closeProcessorLists();

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
        for ( int i = 0 ; i < numThreads ; i++ )
            workerPool.execute(this);
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

        while(true) {
            try {
                String key = spool.accept();
                MailImpl mail = spool.retrieve(key);
                // Retrieve can return null if the mail is no longer on the spool
                // (i.e. another thread has gotten to it first).
                // In this case we simply continue to the next key
                if (mail == null) {
                    continue;
                }
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
            } catch (Exception e) {
                e.printStackTrace();
                if (getLogger().isErrorEnabled()) {
                    getLogger().error("Exception in JamesSpoolManager.run "
                                      + e.getMessage());
                }
            }
        }
    }

    /**
     * Process this mail message by the appropriate processor as designated
     * in the state of the Mail object.
     *
     * @param mail the mail message to be processed
     */
    protected void process(MailImpl mail) {
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
                                .append("Processed")
                                .append(mail.getName())
                                .append(" through ")
                                .append(processorName);
                    getLogger().debug(logMessageBuffer.toString());
                    getLogger().debug("Result was " + mail.getState());
                }
                return;
            } catch (Exception e) {
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
                    //We got an error... send it to the error processor
                    mail.setState(Mail.ERROR);
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
