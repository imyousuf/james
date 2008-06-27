/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.services;

/**
 * Interface for a Repository for Spooling Mails.
 * A spool repository is a transitory repository which should empty itself 
 * if inbound deliveries stop.
 *
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
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
    String accept();

    /**
     * Returns the key for an arbitrarily select mail deposited in this Repository that
     * is either ready immediately for delivery, or is younger than it's last_updated plus
     * the number of failed attempts times the delay time.
     * Usage: RemoteDeliverySpool calls accept() with some delay and should block until an
     * unprocessed mail is available.
     *
     * @return the key for the mail
     */
    String accept(long delay);
}
