/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport;

import java.io.*;
import java.util.*;
import javax.mail.*;
import org.apache.avalon.*;

//import org.apache.avalon.utils.*;
import org.apache.james.*;
import org.apache.james.core.*;
import org.apache.james.mailrepository.*;
import org.apache.log.Logger;
import org.apache.mailet.*;

/**
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
 *
 *  SAMPLE CONFIGURATION
 *  <processor name="try" onerror="return,log">
 *      <mailet match="RecipientIsLocal" class="LocalDelivery">
 *      </mailet>
 *      <mailet match="All" class="RemoteDelivery">
 *          <delayTime>21600000</delayTime>
 *          <maxRetries>5</maxRetries>
 *      </mailet>
 *  </processor>
 */
public class LinearProcessor {
    private final static boolean DEBUG_PRINT_PIPE = false;

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

    public void service(MailImpl mail) throws MessagingException {
        if (DEBUG_PRINT_PIPE) {
            logger.info("Processing mail " + mail.getName());
        }
        unprocessed.setSize(mailets.size() + 2);
        unprocessed.add(0, mail);
        printPipe(unprocessed);/*DEBUG*/
        for (int i = 0; true ; i++) {
            if (DEBUG_PRINT_PIPE) {
                logger.info("===== i = " + i + " =====");
            }
            MailImpl next = (MailImpl) unprocessed.get(i);
            if (!isEmpty(next)) {
                Collection rcpts = null;
                Matcher matcher = (Matcher) matchers.get(i);
                try {
                    rcpts = matcher.match(next);
                    if (rcpts == null) {
                        rcpts = new Vector();
                    }
                    verifyMailAddresses(rcpts);
                } catch (MessagingException me) {
                    handleException(me, next, matcher.getMatcherConfig().getMatcherName());
                }
                //Split the recipients
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
                    MailImpl notNext = (MailImpl)mail.duplicate(newName(next));
                    next.setRecipients(rcpts);
                    notNext.setRecipients(notRcpts);
                    mailBucket[0] = next;
                    mailBucket[1] = notNext;
                }
                unprocessed.set(i, mailBucket[0]);
                unprocessed.set(i + 1, mailBucket[1]);

                if (DEBUG_PRINT_PIPE) {
                    logger.info("--- after split (" + i + ")---");
                }
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
                    verifyMailAddresses(mail.getRecipients());
                } catch (MessagingException me) {
                    handleException(me, next, mailet.getMailetConfig().getMailetName());
                }
                if (isEmpty(next)) {
                    unprocessed.set(i + 1, null);
                } else {
                    unprocessed.set(i + 1, next);
                }
                unprocessed.set(i, null);
                if (DEBUG_PRINT_PIPE) {
                    logger.info("--- after service (" + i + ")---");
                }
                printPipe(unprocessed);/*DEBUG*/
            }
        }
        if (DEBUG_PRINT_PIPE) {
            logger.info("Mail " + mail.getName() + " processed");
        }
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
        if (!DEBUG_PRINT_PIPE) {
            return;
        }
        int j = 0;
        for (Iterator i = unprocessed.iterator(); i.hasNext(); j++) {
            Mail m = (Mail) i.next();
            if (m == null) {
                logger.info("unprocessed " + j + " -> Null ");
            } else {
                logger.info("unprocessed " + j + " -> " + printRecipients(m));
            }
        }
    }

    /**
     * Checks that all objects in this class are of the form MailAddress
     */
    private void verifyMailAddresses(Collection col) throws MessagingException {
        MailAddress addresses[] = (MailAddress[])col.toArray(new MailAddress[0]);
        if (addresses.length != col.size()) {
            throw new MailetException("The recipient list contains objects other than MailAddress objects");
        }
    }

    private void handleException(MessagingException me, Mail mail, String offendersName) throws MessagingException {
        mail.setState(Mail.ERROR);
        StringWriter sout = new StringWriter();
        PrintWriter out = new PrintWriter(sout, true);
        out.println("Exception calling " + offendersName + ": " + me.getMessage());
        Exception e = me;
        while (e != null) {
            e.printStackTrace(out);
            if (e instanceof MessagingException) {
                e = ((MessagingException)e).getNextException();
            } else {
                e = null;
            }
        }
        mail.setErrorMessage(sout.toString());
        logger.error(sout.toString());
        throw me;
    }
}
