/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport;

import java.util.*;
import org.apache.avalon.*;
import org.apache.avalon.blocks.*;
import org.apache.avalon.utils.*;
import org.apache.james.*;
import org.apache.james.core.*;
import org.apache.mailet.*;
import javax.mail.*;
import javax.mail.internet.*;

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
public class LinearProcessor {

    private List mailets;
    private List matchers;
    private Vector unprocessed;
    private Random random;
    private Logger logger;

    public void init() {
        this.matchers = new Vector();
        this.mailets = new Vector();
        this.unprocessed = new Vector();    //mailets.size() + 1, 2);
        random = new Random();
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void add(Matcher matcher, Mailet mailet) {
        matchers.add(matcher);
        mailets.add(mailet);
    }

    public void service(MailImpl mail) throws MailetException, MessagingException {
        logger.log("Processing mail " + mail.getName(), "Processor", logger.INFO);
        unprocessed.setSize(mailets.size() + 2);
        unprocessed.add(0, mail);
        printPipe(unprocessed);/*DEBUG*/
        for (int i = 0; true ; i++) {
            logger.log("===== i = " + i + " =====", "Processor", logger.INFO);
            MailImpl next = (MailImpl) unprocessed.get(i);
            if (!isEmpty(next)) {
                Collection rcpts = ((Matcher) matchers.get(i)).match(next);
                //Split the recipients
                if (rcpts == null) {
                    rcpts = new Vector();
                }
                Collection notRcpts = new Vector();
                notRcpts.addAll(next.getRecipients());
                notRcpts.removeAll(rcpts);

                //Leave recipients that match (and the other Mail info)
                //in this spot in the array, and push all others onto the next spot (mailet).
                MailImpl[] mailBucket = {null, null};
                if (rcpts.isEmpty()) {
                    next.setRecipients(notRcpts);
                    mailBucket[0] = (MailImpl) null;
                    mailBucket[1] = next;
                } else if (notRcpts.isEmpty()) {
                    next.setRecipients(rcpts);
                    mailBucket[0] = next;
                    mailBucket[1] = (MailImpl) null;
                } else {
                    //This old method of key creation might create
                    //duplicates in certain circumtances
                    //MailImpl notNext = mail.duplicate(next.getName() + "!");
                    MailImpl notNext = (MailImpl)mail.duplicate(newName(next));
                    next.setRecipients(rcpts);
                    notNext.setRecipients(notRcpts);
                    mailBucket[0] = next;
                    mailBucket[1] = notNext;
                }
                unprocessed.set(i, mailBucket[0]);
                unprocessed.set(i + 1, mailBucket[1]);

                logger.log("--- after split (" + i + ")---", "Processor", logger.INFO);
                printPipe(unprocessed);/*DEBUG*/
            } else {
                try {
                    do {
                        next = (MailImpl) unprocessed.get(--i);
                    } while (isEmpty(next));
                } catch (ArrayIndexOutOfBoundsException emptyPipe) {
                    break;
                }
                Mailet mailet = (Mailet) mailets.get(i);
                try {
                    mailet.service(next);
                } catch (MailetException ex) {
                    ex.printStackTrace();/*DEBUG*/
                    next.setState(Mail.ERROR);
                    next.setErrorMessage("Exception calling " + mailet.getMailetConfig().getMailetName() + ": " + ex.getMessage());
                    logger.log("exception calling " + mailet.getMailetConfig().getMailetName() + ": " + ex.getMessage(), "Processor", logger.ERROR);
                    throw ex;
                } catch (MessagingException me) {
                    me.printStackTrace();/*DEBUG*/
                    next.setState(Mail.ERROR);
                    next.setErrorMessage("Exception calling " + mailet.getMailetConfig().getMailetName() + ": " + me.getMessage());
                    logger.log("exception calling " + mailet.getMailetConfig().getMailetName() + ": " + me.getMessage(), "Processor", logger.ERROR);
                    throw me;
                }
                if (isEmpty(next)) {
                    unprocessed.set(i + 1, null);
                } else {
                    unprocessed.set(i + 1, next);
                }
                unprocessed.set(i, null);
                logger.log("--- after service (" + i + ")---", "Processor", logger.INFO);
                printPipe(unprocessed);/*DEBUG*/
            }
        }
        logger.log("Mail " + mail.getName() + " processed", "Processor", logger.INFO);
    }

    private boolean isEmpty(Mail mail) {
        if (mail == null) {
            return true;
        }  else if (mail.getRecipients().isEmpty()) {
            return true;
        } else if (mail.getState() == mail.GHOST) {
            return true;
        } else {
            return false;
        }
    }

    private String newName(MailImpl mail) {
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
            buffer.append(i.next().toString() + " ");
            empty = false;
        }
        if (empty) {
            return "Empty";
        } else {
            return buffer.toString();
        }
    }

    private void printPipe(List unprocessed) {
        int j = 0;
        for (Iterator i = unprocessed.iterator(); i.hasNext(); j++) {
            Mail m = (Mail) i.next();
            if (m == null) {
                logger.log("unprocessed " + j + " -> Null ", "Processor", logger.INFO);
            } else {
                logger.log("unprocessed " + j + " -> " + printRecipients(m), "Processor", logger.INFO);
            }
        }
    }
}
