/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.james.transport;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.mail.MessagingException;
import org.apache.avalon.*;
import org.apache.avalon.blocks.*;
import org.apache.avalon.utils.*;
import org.apache.james.*;
import org.apache.james.core.*;
import org.apache.james.mailrepository.*;
import org.apache.mailet.*;

/**
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
 */
public class JamesSpoolManager implements org.apache.avalon.Component, Composer, Configurable, Stoppable, Service, Contextualizable {

    private SimpleComponentManager comp;
    private Configuration conf;
    private SimpleContext context;
    private SpoolRepository spool;
    private Logger logger;
    private MailetContext mailetcontext;

    private HashMap processors;

    public void setConfiguration(Configuration conf) {
        this.conf = conf;
    }

    public void setContext(Context context) {
        this.context = new SimpleContext(context);
    }

    public void setComponentManager(ComponentManager comp) {
        this.comp = new SimpleComponentManager(comp);
    }

    public void init() throws Exception {

        logger = (Logger) comp.getComponent(Interfaces.LOGGER);
        logger.log("JamesSpoolManager init...", "Processor", logger.INFO);
        spool = (SpoolRepository) comp.getComponent(Constants.SPOOL_REPOSITORY);
        mailetcontext = (MailetContext) comp.getComponent(Interfaces.MAIL_SERVER);

        MailetLoader mailetLoader = new MailetLoader();
        mailetLoader.setConfiguration(conf.getConfiguration("mailetpackages"));
        comp.put(Resources.MAILET_LOADER, mailetLoader);

        MatchLoader matchLoader = new MatchLoader();
        matchLoader.setConfiguration(conf.getConfiguration("matcherpackages"));
        comp.put(Resources.MATCH_LOADER, matchLoader);

        //A processor is a Collection of
        processors = new HashMap();

        for (Enumeration e = conf.getConfigurations("processor"); e.hasMoreElements(); ) {
            Configuration processorConf = (Configuration) e.nextElement();
            String processorName = processorConf.getAttribute("name");
            try {
                LinearProcessor processor = new LinearProcessor();
                processor.setSpool(spool);
                processor.setLogger(logger);
                processor.init();
                processors.put(processorName, processor);

                //If this is the root processor, add the PostmasterAlias mailet silently
                //  to the top
                if (processorName.equals("root")) {
                    Matcher matcher = matchLoader.getMatcher("All", mailetcontext);
                    Mailet mailet = mailetLoader.getMailet("PostmasterAlias", mailetcontext, null);
                    processor.add(matcher, mailet);
                }

                for (Enumeration mailetConfs = processorConf.getConfigurations("mailet"); mailetConfs.hasMoreElements(); ) {
                    Configuration c = (Configuration) mailetConfs.nextElement();
                    String mailetClassName = c.getAttribute("class");
                    String matcherName = c.getAttribute("match");
                    Mailet mailet = null;
                    Matcher matcher = null;
                    try {
                        matcher = matchLoader.getMatcher(matcherName, mailetcontext);
                        //The matcher itself should log that it's been inited.
                        logger.log("Matcher " + matcherName + " instantiated", "Processor", logger.INFO);
                    } catch (MessagingException ex) {
                        // **** Do better job printing out exception
                        logger.log("Unable to init matcher " + matcherName + ": " + ex.toString(), "Processor", logger.ERROR);
                        throw ex;
                    }
                    try {
                        mailet = mailetLoader.getMailet(mailetClassName, mailetcontext, c);
                        logger.log("Mailet " + mailetClassName + " instantiated", "Processor", logger.INFO);
                    } catch (MessagingException ex) {
                        // **** Do better job printing out exception
                        logger.log("Unable to init mailet " + mailetClassName + ": " + ex.getMessage(), "Processor", logger.ERROR);
                        throw ex;
                    }
                    //Add this pair to the proces
                    processor.add(matcher, mailet);
                }

                //Loop through all mailets within processor initializing them

                //Add a Null mailet with All matcher to the processor
                //  this is so if a message gets to the end of a processor, it will always be
                //  ghosted
                Matcher matcher = matchLoader.getMatcher("All", mailetcontext);
                Mailet mailet = mailetLoader.getMailet("Null", mailetcontext, null);
                processor.add(matcher, mailet);

                logger.log("processor " + processorName + " instantiated", "Processor", logger.INFO);
            } catch (Exception ex) {
                logger.log("Unable to init processor " + processorName + ": " + ex.getMessage(), "Processor", logger.ERROR);
                throw ex;
            }
        }

    }

    /**
     * This routinely checks the message spool for messages, and processes them as necessary
     */
    public void run() {

        logger.log("run JamesSpoolManager", "Processor", logger.INFO);
        while(true) {

            try {
                String key = spool.accept();
                MailImpl mail = spool.retrieve(key);
                logger.log("==== Begin processing mail " + mail.getName() + " ====", "Processor", logger.INFO);
                process(mail);
                spool.remove(key);
                logger.log("==== Removed from spool mail " + mail.getName() + " ====", "Processor", logger.INFO);
                mail = null;
            } catch (Exception e) {
                e.printStackTrace();
                logger.log("Exception in JamesSpoolManager.run " + e.getMessage(), "Processor", logger.ERROR);
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
            try {
                LinearProcessor processor = (LinearProcessor)processors.get(processorName);
                if (processor == null) {
                    throw new MailetException("Unable to find processor " + processorName);
                }
                logger.log("Processing " + mail.getName() + " through " + processorName, "Processor", logger.INFO);
                processor.service(mail);
                return;
            } catch (Exception e) {
                //This is a strange error situation that shouldn't ordinarily happen
                System.err.println("Exception in processor <" + processorName + ">");
                e.printStackTrace();
                if (processorName.equals(Mail.ERROR)) {
                    //We got an error on the error processor... kill the message
                    mail.setState(Mail.GHOST);
                    mail.setErrorMessage(e.getMessage());
                } else {
                    //We got an error... send it to the error processor
                    mail.setState(Mail.ERROR);
                    mail.setErrorMessage(e.getMessage());
                }
            }
        }
    }

    public void destroy() {}

    public void stop() {}
}
