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
public class LinearProcessor implements Loggable, Initializable {
    private final static boolean DEBUG_PRINT_PIPE = false;

    private List mailets;
    private List matchers;
    private List[] unprocessed;
    private Collection tempUnprocessed;
    private Random random;
    private Logger logger;
    private SpoolRepository spool;

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void setSpool(SpoolRepository spool) {
        this.spool = spool;
    }


    public void init() {
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
        mail = null;
        int i = 0;
        while (true) {
            //The last element in the unprocessed array is a bucket of mail messages
            //  that went through the entire processor.  We want them to just die,
            //  so we clear that List so they are GC'd.
            unprocessed[unprocessed.length - 1].clear();

            //Reset this to null before we start scanning for it
            mail = null;

            //Try to find a message to process
            for (i = 0; i < unprocessed.length; i++) {
                if (unprocessed[i].size() > 0) {
                    //Get the first element from the queue, and remove it from there
                    mail = (MailImpl)unprocessed[i].get(0);
                    unprocessed[i].remove(mail);
                    break;
                }
	    }

            //See if we didn't find any messages to process
            if (mail == null) {
                //We're done
                return;
            }
    

           //Call the matcher and find what recipients match
            Collection recipients = null;
            Matcher matcher = (Matcher) matchers.get(i);
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
            //Split the recipients into two pools
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
            try {
                mailet.service(mail);
                //Make sure all the recipients are still MailAddress objects
                verifyMailAddresses(mail.getRecipients());
            } catch (MessagingException me) {
                handleException(me, mail, mailet.getMailetConfig().getMailetName());
            }

            //See if the state was changed by the mailet
            if (!mail.getState().equals(originalState)) {
		logger.debug("State changed by: " + mailet.getMailetInfo());
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
		logger.debug("State not changed by: " + mailet.getMailetInfo());
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
