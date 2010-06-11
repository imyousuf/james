/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.mailrepository;

import org.apache.james.core.MailImpl;
import org.apache.james.services.SpoolRepository;
import org.apache.mailet.Mail;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

/**
 * Implementation of a MailRepository on a FileSystem.
 *
 * Requires a configuration element in the .conf.xml file of the form:
 *  <repository destinationURL="file://path-to-root-dir-for-repository"
 *              type="MAIL"
 *              model="SYNCHRONOUS"/>
 * Requires a logger called MailRepository.
 *
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 * @author Charles Benett <charles@benett1.demon.co.uk>
 */
public class AvalonSpoolRepository
    extends AvalonMailRepository
    implements SpoolRepository {

    /**
     * <p>Returns the key for an arbitrarily selected mail deposited in this Repository.
     * Usage: SpoolManager calls accept() to see if there are any unprocessed 
     * mails in the spool repository.</p>
     *
     * <p>Synchronized to ensure thread safe access to the underlying spool.</p>
     *
     * @return the key for the mail
     */
    public synchronized String accept() {
        if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
            getLogger().debug("Method accept() called");
        }
        while (true) {
            try {
                for(Iterator it = list(); it.hasNext(); ) {

                    String s = it.next().toString();                    
                    if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
                        StringBuffer logBuffer =
                            new StringBuffer(64)
                                    .append("Found item ")
                                    .append(s)
                                    .append(" in spool.");
                        getLogger().debug(logBuffer.toString());
                    }
                    if (lock(s)) {
                        if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
                            getLogger().debug("accept() has locked: " + s);
                        }
                        return s;
                    }
                }

                wait();
            } catch (InterruptedException ignored) {
            } catch (ConcurrentModificationException ignoredAlso) {
               // Should never get here now that list methods clones keyset for iterator
            }
        }
    }

    /**
     * <p>Returns the key for an arbitrarily selected mail deposited in this Repository that
     * is either ready immediately for delivery, or is younger than it's last_updated plus
     * the number of failed attempts times the delay time.
     * Usage: RemoteDeliverySpool calls accept() with some delay and should block until an
     * unprocessed mail is available.</p>
     *
     * <p>Synchronized to ensure thread safe access to the underlying spool.</p>
     *
     * @return the key for the mail
     */
    public synchronized String accept(long delay) {
        if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
            getLogger().debug("Method accept(delay) called");
        }
        while (true) {
            long youngest = 0;
            for (Iterator it = list(); it.hasNext(); ) {
                String s = it.next().toString();
                if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
                    StringBuffer logBuffer =
                        new StringBuffer(64)
                                .append("Found item ")
                                .append(s)
                                .append(" in spool.");
                    getLogger().debug(logBuffer.toString());
                }
                if (lock(s)) {
                    if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
                        getLogger().debug("accept(delay) has locked: " + s);
                    }
                    //We have a lock on this object... let's grab the message
                    //  and see if it's a valid time.

                    // Retrieve can return null if the mail is no longer in the store.
                    // In this case we simply continue to the next key
                    MailImpl mail = retrieve(s);
                    if (mail == null) {
                        continue;
                    }
                    if (mail.getState().equals(Mail.ERROR)) {
                        //Test the time...
                        long timeToProcess = delay + mail.getLastUpdated().getTime();
                        if (System.currentTimeMillis() > timeToProcess) {
                            //We're ready to process this again
                            return s;
                        } else {
                            //We're not ready to process this.
                            if (youngest == 0 || youngest > timeToProcess) {
                                //Mark this as the next most likely possible mail to process
                                youngest = timeToProcess;
                            }
                        }
                    } else {
                        //This mail is good to go... return the key
                        return s;
                    }
                }
            }
            //We did not find any... let's wait for a certain amount of time
            try {
                if (youngest == 0) {
                    wait();
                } else {
                    wait(youngest - System.currentTimeMillis());
                }
            } catch (InterruptedException ignored) {
            } catch (ConcurrentModificationException ignoredAlso) {
               // Should never get here now that list methods clones keyset for iterator
            }
        }
    }
}
