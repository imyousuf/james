/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.mailrepository;

import java.util.Iterator;
import javax.mail.MessagingException;
import org.apache.james.util.Lock;
import org.apache.james.core.MailImpl;
import org.apache.james.services.MailStore;
import org.apache.james.services.SpoolRepository;
import org.apache.mailet.Mail;

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

    public synchronized String accept() {
	if (DEEP_DEBUG) getLogger().debug("Method accept() called");
        while (true) {
            for(Iterator it = list(); it.hasNext(); ) {
                
                String s = it.next().toString();
		if (DEEP_DEBUG) getLogger().debug("Found item " + s
                                                  + " in spool.");
                if (lock.lock(s)) {
		    if (DEEP_DEBUG) getLogger().debug("accept() has locked: "
                                                      + s);
                    return s;
                }
                //  Object o = it.next();
                //if (lock.lock(o)) {
                //  return o.toString();
                //}
            }
            try {
                wait();
            } catch (InterruptedException ignored) {
            }
        }
    }

    public synchronized String accept(long delay) {
	if (DEEP_DEBUG) getLogger().debug("Method accept(delay) called");
        while (true) {
            long youngest = 0;
            for (Iterator it = list(); it.hasNext(); ) {
                String s = it.next().toString();
		if (DEEP_DEBUG) getLogger().debug("Found item " + s
                                                  + " in spool.");
                if (lock.lock(s)) {
		    if (DEEP_DEBUG) getLogger().debug("accept(delay) has"
                                                      + " locked: "  + s);
                    //We have a lock on this object... let's grab the message
                    //  and see if it's a valid time.
                    MailImpl mail = retrieve(s);
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
            }
        }
    }
}
