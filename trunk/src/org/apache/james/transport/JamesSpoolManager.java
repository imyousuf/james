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
import org.apache.java.lang.*;
import org.apache.avalon.interfaces.*;
import org.apache.java.util.*;
import org.apache.james.*;
import org.apache.mail.*;

/**
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
 */
public class JamesSpoolManager implements Component, Composer, Configurable, Stoppable, Service, Contextualizable {

    private SimpleComponentManager comp;
    private Configuration conf;
    private SimpleContext context;
    private MailRepository spool;
    private Logger logger;
    private Mailet rootMailet;
    private Mailet errorMailet;

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
        this.spool = (MailRepository) comp.getComponent(Constants.SPOOL_REPOSITORY);

        MailetLoader mailetLoader = new MailetLoader();
        mailetLoader.setConfiguration(conf.getConfiguration("servletpackages"));
        comp.put(Resources.MAILET_LOADER, mailetLoader);
        
        MatchLoader matchLoader = new MatchLoader();
        comp.put(Resources.MATCH_LOADER, matchLoader);
        
        for (Enumeration e = conf.getConfigurations("processor"); e.hasMoreElements(); ) {
            Configuration c = (Configuration) e.nextElement();
            String className = c.getAttribute("class");
            String processorName = c.getAttribute("name");
            try {
                JamesMailetContext cont = new JamesMailetContext(context);
                cont.setConfiguration(c);
                cont.setComponentManager(comp);
                AbstractMailet mailet = (AbstractMailet) Class.forName(className).newInstance();
                mailet.setMailetContext(cont);
                context.put(processorName, mailet);
                logger.log("processor " + processorName + " (" + className + ") instantiated", "Processor", logger.INFO);
            } catch (Exception ex) {
                logger.log("Unable to init processor " + processorName + "(" + className + "): " + ex, "Processor", logger.ERROR);
                throw ex;
            }
        }
        for (Enumeration e = conf.getConfigurations("processor"); e.hasMoreElements(); ) {
            Configuration c = (Configuration) e.nextElement();
            String processorName = c.getAttribute("name");
            try {
                AbstractMailet mailet = (AbstractMailet) context.get(processorName);
                mailet.init();
                logger.log("processor " + processorName + " initialized", "Processor", logger.INFO);
            } catch (Exception ex) {
                logger.log("Unable to init processor " + processorName + ": " + ex, "Processor", logger.ERROR);
                throw ex;
            }
        }
        rootMailet = (Mailet) context.get("root");
        errorMailet = (Mailet) context.get("error");
    }

    /**
     * This routinely checks the message spool for messages, and processes them as necessary
     */
    public void run() {

        logger.log("run JamesSpoolManager", "Processor", logger.INFO);
        while(true) {

            try {
                String key = spool.accept();
                Mail mail = spool.retrieve(key);
                logger.log("==== Begin processing mail " + mail.getName() + " ====", "Processor", logger.INFO);
                try {
                    rootMailet.service(mail);
                } catch (Exception ex) {
                    mail.setState(Mail.ERROR);
                    mail.setErrorMessage("Exception in rootMailet service: " + ex.getMessage());
                    logger.log("Exception in root service (" + mail.getName() + "): " + ex.getMessage(), "Processor", logger.ERROR);
                }
                if (mail.getState() != mail.GHOST) {
                    try {
                        errorMailet.service(mail);
                    } catch (Exception ex) {
                    logger.log("Exception in errorMailet service (" + mail.getName() + "): " + ex.getMessage(), "Processor", logger.ERROR);
                    }
                }
                spool.remove(key);
                logger.log("==== Removed from spool mail " + mail.getName() + " ====", "Processor", logger.INFO);
            } catch (Exception e) {
                logger.log("Exception in JamesSpoolManager.run " + e.getMessage(), "Processor", logger.ERROR);
            }
        }
    }
    
    public void destroy() {}
    
    public void stop() {}
}
