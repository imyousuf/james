/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.transport;

import java.io.*;
import java.util.*;
import javax.mail.*;
import org.apache.avalon.framework.logger.AbstractLoggable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.james.*;
import org.apache.james.core.*;
import org.apache.james.services.SpoolRepository;
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
 *
 * Note that the 'onerror' attribute is not yet supported.
 */
public class LinearProcessor
    extends AbstractLoggable
    implements Initializable {

    private List mailets;
    private List matchers;
    private List[] unprocessed;
    private Collection tempUnprocessed;
    private Random random;
    private Logger logger;
    private SpoolRepository spool;

    public void setSpool(SpoolRepository spool) {
        this.spool = spool;
    }


    public void initialize() {
        this.matchers = new Vector();
        this.mailets = new Vector();
        tempUnprocessed = new Vector();
        tempUnprocessed.add(new Vector(2, 2));
        random = new Random();
    }


    public void add(Matcher matcher, Mailet mailet) {
        matchers.add(matcher);
        mailets.add(mailet);
        //Make the collections array one larger
        tempUnprocessed.add(new Vector(2, 2));
    }


    public synchronized void service(MailImpl mail) throws MessagingException {
        getLogger().debug("Servicing mail: " + mail.getName());
        //unprocessed is an array of Lists of Mail objects
        //  the array indicates which matcher/mailet (stage in the linear
        //  processor) that this Mail needs to be processed.
        //  e.g., a Mail in unprocessed[0] needs to be
        //  processed by the first matcher/mailet.
        //
        //It is a List of Mail objects at each array spot as multiple Mail
        //  objects could be at the same stage.

        //make sure we have the array built
        if (unprocessed == null) {
            //Need to construct that object
            unprocessed = (List[])tempUnprocessed.toArray(new List[0]);
            tempUnprocessed = null;
        }
        //Wipe all the data (just to be sure)
        for (int i = 0; i < unprocessed.length; i++) {
            unprocessed[i].clear();
        }

        //Add the object to the bottom of the list
        unprocessed[0].add(mail);

        //This is the original state of the message
        String originalState = mail.getState();

        //We'll use these as temporary variables in the loop
        mail = null;  // the message we're currently processing
        int i = 0;    // where in the stage we're looking
        while (true) {
            //The last element in the unprocessed array has mail messages
            //  that have completed all stages.  We want them to just die,
            //  so we clear that spot.
            unprocessed[unprocessed.length - 1].clear();

            //initialize the mail reference we will be searching on
            mail = null;

            //Scan through all stages, trying to find a message to process
            for (i = 0; i < unprocessed.length; i++) {
                if (unprocessed[i].size() > 0) {
                    //Get the first element from the queue, and remove it from there
                    mail = (MailImpl)unprocessed[i].get(0);
                    unprocessed[i].remove(mail);
                    break;
                }
            }

            //Check it we found anything
            if (mail == null) {
                //We found no messages to process... we're done servicing the mail object
                return;
            }


            //Call the matcher and find what recipients match
            Collection recipients = null;
            Matcher matcher = (Matcher) matchers.get(i);
            getLogger().debug("Checking " + mail.getName() + " with " + matcher);
            try {
                recipients = matcher.match(mail);
                if (recipients == null) {
                    //In case the matcher returned null, create an empty Vector
                    recipients = new Vector();
                }
                //Make sure all the objects are MailAddress objects
                verifyMailAddresses(recipients);
            } catch (MessagingException me) {
                handleException(me, mail, matcher.getMatcherConfig().getMatcherName());
            }
            //Split the recipients into two pools.  notRecipients will contain the
            //  recipients on the message that the matcher did not return.
            Collection notRecipients = new Vector();
            notRecipients.addAll(mail.getRecipients());
            notRecipients.removeAll(recipients);

            if (recipients.size() == 0) {
                //Everything was not a match... store it in the next spot in the array
                unprocessed[i + 1].add(mail);
                continue;
            }
            if (notRecipients.size() != 0) {
                //There are a mix of recipients and not recipients.
                //We need to clone this message, put the notRecipients on the clone
                //  and store it in the next spot
                MailImpl notMail = (MailImpl)mail.duplicate(newName(mail));
                notMail.setRecipients(notRecipients);
                unprocessed[i + 1].add(notMail);
                //We have to set the reduce possible recipients on the old message
                mail.setRecipients(recipients);
            }
            //We have messages that need to process... time to run the mailet.
            Mailet mailet = (Mailet) mailets.get(i);
            getLogger().debug("Servicing " + mail.getName() + " by " + mailet.getMailetInfo());
            try {
                mailet.service(mail);
                //Make sure all the recipients are still MailAddress objects
                verifyMailAddresses(mail.getRecipients());
            } catch (MessagingException me) {
                handleException(me, mail, mailet.getMailetConfig().getMailetName());
            }

            //See if the state was changed by the mailet
            if (!mail.getState().equals(originalState)) {
                //If this message was ghosted, we just want to let it die
                if (mail.getState().equals(mail.GHOST)) {
                    //let this instance die...
                    mail = null;
                    continue;
                }
                //This was just set to another state... store this back in the spool
                //  and it will get picked up and run in that processor

                //Note we need to store this with a new mail name, otherwise it
                //  will get deleted upon leaving this processor
                mail.setName(newName(mail));
                spool.store(mail);
                mail = null;
                continue;
            } else {
                //Ok, we made it through with the same state... move it to the next
                //  spot in the array
                unprocessed[i + 1].add(mail);
            }

        }
    }

    /**
     * Create a unique new primary key name
     */
    private String newName(MailImpl mail) {
        String name = mail.getName();
        return name + "-!" + Math.abs(random.nextInt());
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
        System.err.println("exception! " + me);
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
        getLogger().error(sout.toString());
        throw me;
    }
}
