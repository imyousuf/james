/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.mailet;

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
     * Define a STREAM repository. Streams are stored in the specified
     * destination.
     */
    String SPOOL = "SPOOL";

    /**
     * Returns the key for an arbitrarily selected mail deposited in this Repository.
     * Usage: SpoolManager calls accept() to see if there are any unprocessed
     * mails in the spool repository.
     *
     * @return the key for the mail
     */
    String accept() throws InterruptedException;

    /**
     * Returns the key for an arbitrarily select mail deposited in this Repository that
     * is either ready immediately for delivery, or is younger than it's last_updated plus
     * the number of failed attempts times the delay time.
     * Usage: RemoteDeliverySpool calls accept() with some delay and should block until an
     * unprocessed mail is available.
     *
     * @return the key for the mail
     */
    String accept(long delay) throws InterruptedException;
}
