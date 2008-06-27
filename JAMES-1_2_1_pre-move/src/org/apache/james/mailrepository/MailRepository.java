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
import org.apache.mailet.*;
import org.apache.james.core.*;
import javax.mail.internet.*;
import javax.mail.MessagingException;

/**
 * Interface for a Repository to store Mails.
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public interface MailRepository extends Store.Repository {

    /**
     * Define a STREAM repository. Streams are stored in the specified
     * destination.
     */
    public final static String MAIL = "MAIL";


    /**
     * Stores a message in this repository. Shouldn't this return the key
     * under which it is stored?
     */
    public void store(MailImpl mc) ;

    /**
     * Retrieves a message given a key. At the moment, keys can be obtained
     * from list() in superinterface Store.Repository
     */
    public MailImpl retrieve(String key);

    /**
     * Removes a specified message
     */
    public void remove(MailImpl mail);

    /**
     * Removes a message identifed by key.
     */
    public void remove(String key) ;

}
