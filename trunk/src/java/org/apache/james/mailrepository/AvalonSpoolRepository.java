/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.james.mailrepository;

import org.apache.james.core.MailImpl;
import org.apache.mailet.Mail;
import org.apache.mailet.SpoolRepository;

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
     * <p>Returns the key for an arbitrarily selected mail deposited in this Repository.
     * Usage: SpoolManager calls accept() to see if there are any unprocessed
     * mails in the spool repository.</p>
     *
     * <p>Synchronized to ensure thread safe access to the underlying spool.</p>
     *
     * @return the key for the mail
     */
    public synchronized String accept() throws InterruptedException {
        if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
            getLogger().debug("Method accept() called");
        }
        while (!Thread.currentThread().isInterrupted()) {
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
            } catch (InterruptedException ex) {
                throw ex;
            } catch (ConcurrentModificationException cme) {
               // Should never get here now that list methods clones keyset for iterator
                getLogger().error("CME in spooler - please report to http://james.apache.org", cme);
            }
        }
        throw new InterruptedException();
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
    public synchronized String accept(long delay) throws InterruptedException {
        if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
            getLogger().debug("Method accept(delay) called");
        }
        while (!Thread.currentThread().isInterrupted()) {
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
                    Mail mail = retrieve(s);
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
            } catch (InterruptedException ex) {
                throw ex;
            } catch (ConcurrentModificationException cme) {
               // Should never get here now that list methods clones keyset for iterator
                getLogger().error("CME in spooler - please report to http://james.apache.org", cme);
            }
        }
        throw new InterruptedException();
    }
}
