/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
 
package org.apache.james.transport;

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
 
/*      SAMPLE CONFIGURATION
    <processor class="org.apache.james.transport.LinearProcessor">
        <servlet match="RecipientIsLocal" class="LocalDelivery">
        </servlet>
        <servlet match="All" class="RemoteDelivery">
            <delayTime>21600000</delayTime>
            <maxRetries>5</maxRetries>
        </servlet>
    </processor>
*/ 
public class LinearProcessor extends AbstractMailet {

    private MatchLoader matchLoader;
    private MailetLoader mailetLoader;
    private Logger logger;
    private Vector servlets;
    private Vector servletMatchs;
    private Vector unprocessed;

    private static final String OP_NOT = "!";
    private static final String OP_OR = "|";
    private static final String OP_AND = "&";

    public void init() throws Exception {
        
        MailetContext context = getContext();
        Configuration conf = context.getConfiguration();
        ComponentManager comp = context.getComponentManager();
        logger = (Logger) comp.getComponent(Interfaces.LOGGER);

        logger.log("LinearProcessor init...", "Mailets", Logger.INFO);

        mailetLoader = (MailetLoader) comp.getComponent(Resources.MAILET_LOADER);
        matchLoader = (MatchLoader) comp.getComponent(Resources.MATCH_LOADER);

        this.servletMatchs = new Vector();
        this.servlets = new Vector();
        this. unprocessed = new Vector();//servlets.size() + 1, 2);
        for (Enumeration e = conf.getConfigurations("servlet"); e.hasMoreElements(); ) {
            Configuration c = (Configuration) e.nextElement();
            String className = c.getAttribute("class");
            try {
                Mailet servlet = mailetLoader.getMailet(className, context.getChildContext(c));
                servlets.addElement(servlet);
                logger.log("Mailet " + className + " instantiated", "Mailets", Logger.INFO);
            } catch (Exception ex) {
                logger.log("Unable to init mailet " + className + ": " + ex, "Mailets", Logger.INFO);
//                ex.printStackTrace();/*DEBUG*/
                throw ex;
            }
            String matchName = c.getAttribute("match");
            try {
                Matcher match = matchLoader.getMatch(matchName, context);
                servletMatchs.addElement(match);
                logger.log("Matcher " + matchName + " instantiated", "Mailets", Logger.INFO);
            } catch (Exception ex) {
                logger.log("Unable to init matcher " + matchName + ": " + ex, "Mailets", Logger.INFO);
//                ex.printStackTrace();/*DEBUG*/
                throw ex;
            }
        }
    }

    public void service(Mail mail) throws Exception {

        logger.log("Processing mail " + mail.getName(), "Mailets", Logger.INFO);
        unprocessed.setSize(servlets.size() + 1);
        unprocessed.insertElementAt(mail, 0);
        printPipe(unprocessed);/*DEBUG*/
        for (int i = 0; true ; i++) {
            logger.log("===== i = " + i + " =====", "Mailets", Logger.INFO);
            Mail next = (Mail) unprocessed.elementAt(i);
            if (!isEmpty(next)) {
                Mail[] res = ((Matcher) servletMatchs.elementAt(i)).match(next);
                unprocessed.setElementAt(res[0], i);
                unprocessed.setElementAt(res[1], i + 1);
                logger.log("--- after split (" + i + ")---", "Mailets", Logger.INFO);
                printPipe(unprocessed);/*DEBUG*/
            } else {
                try {
                    do {
                        next = (Mail) unprocessed.elementAt(--i);
                    } while (isEmpty(next));
                } catch (ArrayIndexOutOfBoundsException emptyPipe) {
                    break;
                }
                Mailet mailet = (Mailet) servlets.elementAt(i);
                try {
                    mailet.service(next);
                } catch (Exception ex) {
//                    ex.printStackTrace();/*DEBUG*/
                    next.setState(Mail.ERROR);
                    next.setErrorMessage("Exception during " + mailet + " service: " + ex.getMessage());
                    logger.log("Exception during " + mailet + " service: " + ex.getMessage(), "Mailets", Logger.INFO);
                    throw ex;
                }
                if (isEmpty(next)) {
                    unprocessed.setElementAt(null, i + 1);
                } else {
                    unprocessed.setElementAt(next, i + 1);
                }
                unprocessed.setElementAt(null, i);
                logger.log("--- after service (" + i + ")---", "Mailets", Logger.INFO);
                printPipe(unprocessed);/*DEBUG*/
            }
        }
        logger.log("Mail " + mail.getName() + " processed", "Mailets", Logger.INFO);
    }

    private boolean isEmpty(Mail mail) {
        if (mail == null) return true;
        else if (mail.getRecipients().isEmpty()) return true;
        else if (mail.getState() == mail.GHOST) return true;
        else return false;
    }

// Debuggin methods...
    private String printRecipients(Mail mail) {
        if (mail == null) return "Null ";
        Vector rec = mail.getRecipients();
        StringBuffer buffer = new StringBuffer("Recipients: ");
        boolean empty = true;
        for (Enumeration e = rec.elements(); e.hasMoreElements(); ) {
            buffer.append((String) e.nextElement() + " ");
            empty = false;
        }
        if (empty) return "Empty";
        else return buffer.toString();
    }

    private void printPipe(Vector unprocessed) {
        for (int j = 0; j < unprocessed.size(); j++) {
            Mail m = (Mail) unprocessed.elementAt(j);
            if (m == null) {
                logger.log("unprocessed " + j + " -> Null ", "Mailets", Logger.INFO);
            } else {
                logger.log("unprocessed " + j + " -> " + printRecipients(m), "Mailets", Logger.INFO);
            }
        }
    }
    public String getServletInfo() {
        return "LinearProcessor";
    }
}
