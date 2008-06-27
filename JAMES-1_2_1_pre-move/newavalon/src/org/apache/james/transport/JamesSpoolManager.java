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
import org.apache.avalon.services.Service;
//import org.apache.avalon.utils.*;
import org.apache.james.*;
import org.apache.james.core.*;
import org.apache.james.services.*;
import org.apache.log.LogKit;
import org.apache.log.Logger;
import org.apache.mailet.*;

/**
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
 */
public class JamesSpoolManager implements Component, Composer, Configurable, Initializable, Runnable, Stoppable, Service, Contextualizable {

    private DefaultComponentManager compMgr;
                   //using implementation as we need put method.
    private Configuration conf;
    private Context context;
    private SpoolRepository spool;
    private Logger logger =  LogKit.getLoggerFor("james.SpoolManager");
    private MailetContext mailetcontext;

    private HashMap processors;

    public JamesSpoolManager() {
    }

    public void configure(Configuration conf) throws ConfigurationException {
        this.conf = conf;
    }

    public void contextualize(Context context) {
        this.context = new DefaultContext(context);
    }

    public void compose(ComponentManager comp) {
        compMgr = new DefaultComponentManager(comp);
    }

    public void init() throws Exception {

        logger.info("JamesSpoolManager init...");
        spool = (SpoolRepository) compMgr.lookup("org.apache.james.services.SpoolRepository");
        mailetcontext = (MailetContext) compMgr.lookup("org.apache.mailet.MailetContext");
	MailetLoader mailetLoader = new MailetLoader();
	MatchLoader matchLoader = new MatchLoader();
	try {
	    mailetLoader.configure(conf.getChild("mailetpackages"));
	    matchLoader.configure(conf.getChild("matcherpackages"));
	    compMgr.put(Resources.MAILET_LOADER, mailetLoader);
	    compMgr.put(Resources.MATCH_LOADER, matchLoader);
	} catch (ConfigurationException ce) {
	    logger.error("Unable to configure mailet/matcher Loaders: " + ce.getMessage());
	    throw new RuntimeException("Unable to start Spool Manager - failed to configure Loaders.");
	}

        //A processor is a Collection of
        processors = new HashMap();

        for (Iterator it = conf.getChildren("processor"); it.hasNext(); ) {
            Configuration processorConf = (Configuration) it.next();
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

                for (Iterator mailetConfs = processorConf.getChildren("mailet"); mailetConfs.hasNext(); ) {
                    Configuration c = (Configuration) mailetConfs.next();
                    String mailetClassName = c.getAttribute("class");
                    String matcherName = c.getAttribute("match");
                    Mailet mailet = null;
                    Matcher matcher = null;
                    try {
                        matcher = matchLoader.getMatcher(matcherName, mailetcontext);
                        //The matcher itself should log that it's been inited.
                        logger.info("Matcher " + matcherName + " instantiated");
                    } catch (MessagingException ex) {
                        // **** Do better job printing out exception
                        logger.error("Unable to init matcher " + matcherName + ": " + ex.toString());
                        throw ex;
                    }
                    try {
                        mailet = mailetLoader.getMailet(mailetClassName, mailetcontext, c);
                        logger.info("Mailet " + mailetClassName + " instantiated");
                    } catch (MessagingException ex) {
                        // **** Do better job printing out exception
                        logger.error("Unable to init mailet " + mailetClassName + ": " + ex.getMessage());
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

                logger.info("processor " + processorName + " instantiated");
            } catch (Exception ex) {
                logger.error("Unable to init processor " + processorName + ": " + ex.getMessage());
                throw ex;
            }
        }

    }

    /**
     * This routinely checks the message spool for messages, and processes them as necessary
     */
    public void run() {

        logger.info("run JamesSpoolManager");
        while(true) {

            try {
                String key = spool.accept();
                MailImpl mail = spool.retrieve(key);
                logger.info("==== Begin processing mail " + mail.getName() + " ====");
                process(mail);
                spool.remove(key);
                logger.info("==== Removed from spool mail " + mail.getName() + " ====");
		mail = null;
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Exception in JamesSpoolManager.run " + e.getMessage());
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
		logger.info("Processing " + mail.getName() + " through " + processorName);
                processor.service(mail);
		return;
            } catch (Exception e) {
               //This is a strange error situation that shouldn't ordinarily happen
               System.err.println("Exception in processor <" + processorName + ">");
                e.printStackTrace();
                if (processorName.equals("Mail.ERROR")) {
                    //We got an error on the error processor... kill the message
                    mail.setState(Mail.GHOST);
                    mail.setErrorMessage(e.getMessage());
                } else {
                    //We got an error... send it to the error processor
                    mail.setState("Mail.ERROR");
                    mail.setErrorMessage(e.getMessage());
                }
            }
            logger.info("Processed " + mail.getName() + " through " + processorName);
            logger.info("Result was " + mail.getState());

        }
    }

    public void destroy() {}

    public void stop() {}
}
