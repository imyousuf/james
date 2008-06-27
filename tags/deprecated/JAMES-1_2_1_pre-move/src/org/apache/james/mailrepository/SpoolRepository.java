/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.mailrepository;

import org.apache.avalon.blocks.*;
import org.apache.avalon.*;
import org.apache.avalon.utils.*;
import java.util.*;
import java.io.*;
import org.apache.mailet.Mail;
import javax.mail.internet.*;
import javax.mail.MessagingException;

/**
 * Interface for a Repository for Spooling Mails.
 * A spool repository is a transitory repository which should empty itself if inbound deliveries stop.
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public interface SpoolRepository extends MailRepository {

    /**
     * Define a STREAM repository. Streams are stored in the specified
     * destination.
     */
    public final static String SPOOL = "SPOOL";

    /**
     * Returns the key for an arbitrarily selected mail deposited in this Repository.
     * Useage: SpoolManager calls accept() to see if there are any unprocessed mails in the spool repository.
     */
    public String accept();

    /**
     * Returns the key for an arbitrarily select mail depository in this Repositry that
     * is either ready immediately for delivery, or is younger than it's last_updated plus
     * the number of failed attempts times the delay time.
     * Useage: RemoteDeliverySpool calls accept() with some delay and should block until an
     * unprocessed mail is available.
     */
    public String accept(long delay);
}
