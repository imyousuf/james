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
    <processor name="try" class="org.apache.james.transport.LinearProcessor">
        <mailet match="RecipientIsLocal" class="LocalDelivery">
        </mailet>
        <mailet match="All" class="RemoteDelivery">
            <delayTime>21600000</delayTime>
            <maxRetries>5</maxRetries>
        </mailet>
    </processor>
*/
public class LinearProcessor extends AbstractMailet {

    private MatchLoader matchLoader;
    private MailetLoader mailetLoader;
    private Logger logger;
    private List mailets;
    private List mailetMatchs;
    private Vector unprocessed;
    private Random random;

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

        this.mailetMatchs = new Vector();
        this.mailets = new Vector();
        this.unprocessed = new Vector();    //mailets.size() + 1, 2);
        for (Enumeration e = conf.getConfigurations("mailet"); e.hasMoreElements(); ) {
            Configuration c = (Configuration) e.nextElement();
            String className = c.getAttribute("class");
            try {
                Mailet mailet = mailetLoader.getMailet(className, context.getChildContext(c));
                mailets.add(mailet);
                logger.log("Mailet " + className + " instantiated", "Mailets", Logger.INFO);
            } catch (Exception ex) {
                logger.log("Unable to init mailet " + className + ": " + ex, "Mailets", Logger.INFO);
//                ex.printStackTrace();/*DEBUG*/
                throw ex;
            }
            String matchName = c.getAttribute("match");
            try {
                Matcher match = matchLoader.getMatch(matchName, context);
                mailetMatchs.add(match);
                logger.log("Matcher " + matchName + " instantiated", "Mailets", Logger.INFO);
            } catch (Exception ex) {
                logger.log("Unable to init matcher " + matchName + ": " + ex, "Mailets", Logger.INFO);
//                ex.printStackTrace();/*DEBUG*/
                throw ex;
            }
        }
        random = new Random();
    }

    public void service(Mail mail) throws Exception {

        logger.log("Processing mail " + mail.getName(), "Mailets", Logger.INFO);
        unprocessed.setSize(mailets.size() + 2);
        unprocessed.add(0, mail);
        printPipe(unprocessed);/*DEBUG*/
        for (int i = 0; true ; i++) {
            logger.log("===== i = " + i + " =====", "Mailets", Logger.INFO);
            Mail next = (Mail) unprocessed.get(i);
            if (!isEmpty(next)) {
                Collection rcpts = ((Matcher) mailetMatchs.get(i)).match(next);
                //Split the recipients
                if (rcpts == null) {
                    rcpts = new Vector();
                }
                Collection notRcpts = new Vector();
                notRcpts.addAll(next.getRecipients());
                notRcpts.removeAll(rcpts);

                //Leave recipients that match (and the other Mail info)
                //in this spot in the array, and push all others onto the next spot (mailet).
                Mail[] mailBucket = {null, null};
                if (rcpts.isEmpty()) {
                    next.setRecipients(notRcpts);
                    mailBucket[0] = (Mail) null;
                    mailBucket[1] = next;
                } else if (notRcpts.isEmpty()) {
                    next.setRecipients(rcpts);
                    mailBucket[0] = next;
                    mailBucket[1] = (Mail) null;
                } else {
                    //This old method of key creation might create
                    //duplicates in certain circumtances
                    //Mail notNext = mail.duplicate(next.getName() + "!");
                    Mail notNext = mail.duplicate(newName(next));
                    next.setRecipients(rcpts);
                    notNext.setRecipients(notRcpts);
                    mailBucket[0] = next;
                    mailBucket[1] = notNext;
                }
                unprocessed.set(i, mailBucket[0]);
                unprocessed.set(i + 1, mailBucket[1]);

                logger.log("--- after split (" + i + ")---", "Mailets", Logger.INFO);
                printPipe(unprocessed);/*DEBUG*/
            } else {
                try {
                    do {
                        next = (Mail) unprocessed.get(--i);
                    } while (isEmpty(next));
                } catch (ArrayIndexOutOfBoundsException emptyPipe) {
                    break;
                }
                Mailet mailet = (Mailet) mailets.get(i);
                try {
                    mailet.service(next);
                } catch (Exception ex) {
                    ex.printStackTrace();/*DEBUG*/
                    next.setState(Mail.ERROR);
                    next.setErrorMessage("Exception during " + mailet + " service: " + ex.getMessage());
                    logger.log("Exception during " + mailet + " service: " + ex.getMessage(), "Mailets", Logger.INFO);
                    throw ex;
                }
                if (isEmpty(next)) {
                    unprocessed.set(i + 1, null);
                } else {
                    unprocessed.set(i + 1, next);
                }
                unprocessed.set(i, null);
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

    private String newName(Mail mail) {
        String name = mail.getName();
        return name + "-!" + Math.abs(random.nextInt());
    }

// Debuggin methods...
    private String printRecipients(Mail mail) {
        if (mail == null) return "Null ";
        Collection rec = mail.getRecipients();
        StringBuffer buffer = new StringBuffer("Recipients: ");
        boolean empty = true;
        for (Iterator i = rec.iterator(); i.hasNext(); ) {
            buffer.append((String) i.next() + " ");
            empty = false;
        }
        if (empty) return "Empty";
        else return buffer.toString();
    }

    private void printPipe(List unprocessed) {
        int j = 0;
        for (Iterator i = unprocessed.iterator(); i.hasNext(); j++) {
            Mail m = (Mail) i.next();
            if (m == null) {
                logger.log("unprocessed " + j + " -> Null ", "Mailets", Logger.INFO);
            } else {
                logger.log("unprocessed " + j + " -> " + printRecipients(m), "Mailets", Logger.INFO);
            }
        }
    }
    public String getMailetInfo() {
        return "LinearProcessor";
    }
}
