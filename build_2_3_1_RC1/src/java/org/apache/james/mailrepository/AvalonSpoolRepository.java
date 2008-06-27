/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mailrepository;

import org.apache.james.services.SpoolRepository;
import org.apache.mailet.Mail;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

/**
 * Implementation of a MailRepository on a FileSystem.
 *
 * Requires a configuration element in the .conf.xml file of the form:
 *  &lt;repository destinationURL="file://path-to-root-dir-for-repository"
 *              type="MAIL"
 *              model="SYNCHRONOUS"/&gt;
 * Requires a logger called MailRepository.
 *
 * @version 1.0.0, 24/04/1999
 */
public class AvalonSpoolRepository
    extends AvalonMailRepository
    implements SpoolRepository {

    /**
     * <p>Returns an arbitrarily selected mail deposited in this Repository.
     * Usage: SpoolManager calls accept() to see if there are any unprocessed 
     * mails in the spool repository.</p>
     *
     * <p>Synchronized to ensure thread safe access to the underlying spool.</p>
     *
     * @return the mail
     */
    public synchronized Mail accept() throws InterruptedException {
        if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
            getLogger().debug("Method accept() called");
        }
        return accept(new SpoolRepository.AcceptFilter () {
            public boolean accept (String _, String __, long ___, String ____) {
                return true;
            }

            public long getWaitTime () {
                return 0;
            }
        });
    }

    /**
     * <p>Returns an arbitrarily selected mail deposited in this Repository that
     * is either ready immediately for delivery, or is younger than it's last_updated plus
     * the number of failed attempts times the delay time.
     * Usage: RemoteDeliverySpool calls accept() with some delay and should block until an
     * unprocessed mail is available.</p>
     *
     * <p>Synchronized to ensure thread safe access to the underlying spool.</p>
     *
     * @return the mail
     */
    public synchronized Mail accept(final long delay) throws InterruptedException
    {
        if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
            getLogger().debug("Method accept(delay) called");
        }
        return accept(new SpoolRepository.AcceptFilter () {
            long youngest = 0;
                
                public boolean accept (String key, String state, long lastUpdated, String errorMessage) {
                    if (state.equals(Mail.ERROR)) {
                        //Test the time...
                        long timeToProcess = delay + lastUpdated;
                
                        if (System.currentTimeMillis() > timeToProcess) {
                            //We're ready to process this again
                            return true;
                        } else {
                            //We're not ready to process this.
                            if (youngest == 0 || youngest > timeToProcess) {
                                //Mark this as the next most likely possible mail to process
                                youngest = timeToProcess;
                            }
                            return false;
                        }
                    } else {
                        //This mail is good to go... return the key
                        return true;
                    }
                }
        
                public long getWaitTime () {
                    if (youngest == 0) {
                        return 0;
                    } else {
                        long duration = youngest - System.currentTimeMillis();
                        youngest = 0; //get ready for next round
                        return duration <= 0 ? 1 : duration;
                    }
                }
            });
    }


    /**
     * Returns an arbitrarily select mail deposited in this Repository for
     * which the supplied filter's accept method returns true.
     * Usage: RemoteDeliverySpool calls accept(filter) with some a filter which determines
     * based on number of retries if the mail is ready for processing.
     * If no message is ready the method will block until one is, the amount of time to block is
     * determined by calling the filters getWaitTime method.
     *
     * <p>Synchronized to ensure thread safe access to the underlying spool.</p>
     *
     * @return  the mail
     */
    public synchronized Mail accept(SpoolRepository.AcceptFilter filter) throws InterruptedException {
        if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
            getLogger().debug("Method accept(Filter) called");
        }
        while (!Thread.currentThread().isInterrupted()) try {
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
                        getLogger().debug("accept(Filter) has locked: " + s);
                    }
                    try {
                        Mail mail = retrieve(s);
                        // Retrieve can return null if the mail is no longer on the spool
                        // (i.e. another thread has gotten to it first).
                        // In this case we simply continue to the next key
                        if (mail == null || !filter.accept (mail.getName(),
                                                            mail.getState(),
                                                            mail.getLastUpdated().getTime(),
                                                            mail.getErrorMessage())) {
                            unlock(s);
                            continue;
                        }
                        return mail;
                    } catch (javax.mail.MessagingException e) {
                        unlock(s);
                        getLogger().error("Exception during retrieve -- skipping item " + s, e);
                    }
                }
            }

            //We did not find any... let's wait for a certain amount of time
            wait (filter.getWaitTime());
        } catch (InterruptedException ex) {
            throw ex;
        } catch (ConcurrentModificationException cme) {
            // Should never get here now that list methods clones keyset for iterator
            getLogger().error("CME in spooler - please report to http://james.apache.org", cme);
        }
        throw new InterruptedException();
    }
    
}
