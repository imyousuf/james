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

package org.apache.james.services;

import org.apache.mailet.Mail;

/**
 * Interface for a Repository for Spooling Mails.
 * A spool repository is a transitory repository which should empty itself 
 * if inbound deliveries stop.
 *
 * @version 1.0.0, 24/04/1999
 */
public interface SpoolRepository 
    extends MailRepository {

    /**
     * Implementations of AcceptFilter can be used to select which mails a SpoolRepository
     * implementation returns from its accept (AcceptFilter) method
     **/
    public static interface AcceptFilter
    {
        /**
         * This method is called by accept(Filter) to determine if the message is
         * ready for delivery.
         *
         * @param key message key
         * @param state the state of the message
         * @param lastUpdated the last time the message was written to the spool
         * @param errorMessage the current errorMessage
         * @return true if the message is ready for delivery
         **/
        boolean accept (String key, String state, long lastUpdated, String errorMessage) ;


        /**
         * This method allows the filter to determine how long the thread should wait for a
         * message to get ready for delivery, when currently there are none.
         * @return the time to wait for a message to get ready for delivery
         **/
        long getWaitTime ();
    }
    
    /**
     * Define a STREAM repository. Streams are stored in the specified
     * destination.
     */
    String SPOOL = "SPOOL";

    /**
     * Returns an arbitrarily selected mail deposited in this Repository.
     * Usage: SpoolManager calls accept() to see if there are any unprocessed 
     * mails in the spool repository.
     *
     * @return the mail
     */
    Mail accept() throws InterruptedException;

    /**
     * Returns an arbitrarily select mail deposited in this Repository that
     * is either ready immediately for delivery, or is younger than it's last_updated plus
     * the number of failed attempts times the delay time.
     * Usage: RemoteDeliverySpool calls accept() with some delay and should block until an
     * unprocessed mail is available.
     *
     * @return the mail
     */
    Mail accept(long delay) throws InterruptedException;

    /**
     * Returns an arbitrarily select mail deposited in this Repository for
     * which the supplied filter's accept method returns true.
     * Usage: RemoteDeliverySpool calls accept(filter) with some a filter which determines
     * based on number of retries if the mail is ready for processing.
     * If no message is ready the method will block until one is, the amount of time to block is
     * determined by calling the filters getWaitTime method.
     *
     * @return the mail
     */
    Mail accept(AcceptFilter filter) throws InterruptedException;

}
