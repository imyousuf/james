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
     * The component role used by components implementing this service
     */
    String ROLE = "org.apache.james.services.SpoolRepository";

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
