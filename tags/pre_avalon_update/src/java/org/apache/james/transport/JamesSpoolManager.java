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
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.context.DefaultContext;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.phoenix.Block;
import org.apache.james.core.MailImpl;
import org.apache.james.services.MailStore;
import org.apache.james.services.SpoolRepository;
import org.apache.mailet.*;

import javax.mail.MessagingException;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
 *
 * This is $Revision: 1.9 $
 * Committed on $Date: 2002/03/01 15:58:40 $ by: $Author: danny $
 */
public class JamesSpoolManager
    extends AbstractLogEnabled
    implements Contextualizable, Composable, Configurable, Initializable,
               Runnable, Disposable,  Block {

    private final static boolean DEEP_DEBUG = true;
    private DefaultComponentManager compMgr;
    //using implementation as we need put method.
    private Configuration conf;
    private Context context;
    private SpoolRepository spool;
    private MailetContext mailetcontext;
    private HashMap processors;
    private int threads;
    private ThreadPool workerPool;
    private ThreadManager threadManager;

    public void configure(Configuration conf) throws ConfigurationException {
        this.conf = conf;
        threads = conf.getChild("threads").getValueAsInteger(1);
    }

    public void contextualize(Context context) {
        this.context = new DefaultContext( context );
    }

    public void compose(ComponentManager comp)
        throws ComponentException {
        threadManager = (ThreadManager)comp.lookup( ThreadManager.ROLE );
        compMgr = new DefaultComponentManager(comp);
    }

    public void initialize() throws Exception {

        getLogger().info("JamesSpoolManager init...");
        workerPool = threadManager.getThreadPool( "default" );
        MailStore mailstore
            = (MailStore) compMgr.lookup("org.apache.james.services.MailStore");
        spool = mailstore.getInboundSpool();
        if (DEEP_DEBUG) getLogger().debug("Got spool");

        mailetcontext
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
            getLogger().error( message, ce );
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
                                                             mailetcontext);
                    Mailet mailet = mailetLoader.getMailet("PostmasterAlias",
                                                           mailetcontext, null);
                    processor.add(matcher, mailet);
                }

                final Configuration[] mailetConfs
                    = processorConf.getChildren( "mailet" );
                for ( int j = 0; j < mailetConfs.length; j++ )
                {
                    Configuration c = mailetConfs[j];
                    String mailetClassName = c.getAttribute("class");
                    String matcherName = c.getAttribute("match");
                    Mailet mailet = null;
                    Matcher matcher = null;
                    try {
                        matcher = matchLoader.getMatcher(matcherName,
                                                         mailetcontext);
                        //The matcher itself should log that it's been inited.
                        getLogger().info("Matcher " + matcherName
                                         + " instantiated");
                    } catch (MessagingException ex) {
                        // **** Do better job printing out exception
                        getLogger().error( "Unable to init matcher "
                                           + matcherName + ": " + ex.toString(), ex );
                        System.err.println("Unable to init mailet " + matcherName);
                        System.err.println("Check spool manager logs for more details.");
                        ex.printStackTrace();
                        //System.exit(1);
                        throw ex;
                    }
                    try {
                        mailet = mailetLoader.getMailet(mailetClassName,
                                                        mailetcontext, c);
                        getLogger().info("Mailet " + mailetClassName
                                         + " instantiated");
                    } catch (MessagingException ex) {
                        // **** Do better job printing out exception
                        getLogger().error("Unable to init mailet "
                                          + mailetClassName + ": " + ex.getMessage());
                        System.err.println("Unable to init mailet " + mailetClassName);
                        System.err.println("Check spool manager logs for more details.");
                        ex.printStackTrace();
                        //System.exit(1);
                        throw ex;
                    }
                    //Add this pair to the proces
                    processor.add(matcher, mailet);
                }

                //Loop through all mailets within processor initializing them

                //Add a Null mailet with All matcher to the processor
                //  this is so if a message gets to the end of a processor,
                //   it will always be ghosted
                Matcher matcher = matchLoader.getMatcher("All", mailetcontext);
                Mailet mailet
                    = mailetLoader.getMailet("Null", mailetcontext, null);
                processor.add(matcher, mailet);

                getLogger().info("processor " + processorName
                                 + " instantiated");
            } catch (Exception ex) {
                getLogger().error("Unable to init processor " + processorName
                                  + ": " + ex.getMessage());
                throw ex;
            }
        }
        getLogger().info("Spooler Manager uses "+threads+" Thread(s)");
        for ( int i = 0 ; i < threads ; i++ )
            workerPool.execute(this);
    }

    /**
     * This routinely checks the message spool for messages, and processes
     * them as necessary
     */
    public void run() {

        getLogger().info("run JamesSpoolManager: "
                         + Thread.currentThread().getName());
        getLogger().info("spool="+spool.getClass().getName());
        while(true) {

            try {
                String key = spool.accept();
                MailImpl mail = spool.retrieve(key);
                getLogger().info("==== Begin processing mail "
                                 + mail.getName() + " ====");
                process(mail);
                spool.remove(key);
                getLogger().info("==== Removed from spool mail "
                                 + mail.getName() + " ====");
                mail = null;
            } catch (Exception e) {
                e.printStackTrace();
                getLogger().error("Exception in JamesSpoolManager.run "
                                  + e.getMessage());
            }
        }
    }

    /**
     * Process this mail message by the appropriate processor as designated
     * in the state of the Mail object.
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
                    throw new MailetException("Unable to find processor "
                                              + processorName);
                }
                getLogger().info("Processing " + mail.getName() + " through "
                                 + processorName);
                processor.service(mail);
                return;
            } catch (Exception e) {
                // This is a strange error situation that shouldn't ordinarily
                // happen
                System.err.println("Exception in processor <" + processorName
                                   + ">");
                e.printStackTrace();
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
            getLogger().info("Processed " + mail.getName() + " through "
                             + processorName);
            getLogger().info("Result was " + mail.getState());

        }
    }

    // Shutdown processors
    public void dispose() {
        getLogger().info("JamesSpoolManager dispose...");
        Iterator it = processors.keySet().iterator();
        while (it.hasNext()) {
            String processorName = (String)it.next();
            getLogger().debug("Processor " + processorName);
            LinearProcessor processor = (LinearProcessor)processors.get(processorName);
            processor.dispose();
            processors.remove(processor);
        }
    }
}
