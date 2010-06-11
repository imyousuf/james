/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.imap41server;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

/**
 * Interface for objects representing the record of a folder on an IMAP host.
 * 
 * @author Charles Benett <charles@benett1.demon.co.uk>
 * @version 0.1
 */

public interface FolderRecord {
	
    /**
     * Records the full name, including namespace, of a mailbox relative to
     * a specified user. 
     *
     * @param mailboxName String mailbox hierarchical name including namespace
     * @param user String a user. An empty user parameter indicates that the 
     * mailbox name is absolute.
     */
    public void setFullName(String mailboxName, String user) ;
	
    /**
     * Returns the full name, including namespace, of this mailbox. 
     *
     * @returns String mailbox hierarchical name including namespace
     */
    public String getFullName();

    /**
     * Returns the user in whose namespace the mailbox existed.
     *
     * @param user String a user.An empty string indicates that the 
     * mailbox name is absolute.
     */
    public String getUser();

    /**
     * Records if this mailbox name is currently in use. The mailbox name is
     * in use when a mailbox with this name has been created. Implementations
     * that allow shared mailboxes may encounter a sate where the mailbox has
     * been deleted but there are clients who were already connected to the
     * mailbox. In this case the name remains in use until all clients have
     * either de-selected the mailbox or been disconnected from the server. 
     *
     * @param state boolean true when mailbox created, false when name no
     * longer in use.
     */
    public void setNameInUse(boolean state);

    /**
     * Returns unavailability of name for a new mailbox.
     *
     * @returns true if this name is in use. Must return true if isDeleted
     * returns false.
     */
    public boolean isNameInUse() ;

    /**
     *  Records if the corresponding mailbox has been deleted.
     *
     * @param state boolean true when mailbox deleted, false when created
     */
    public void setDeleted(boolean state) ;

    /**
     * Returns whether mailbox has been deleted. A deleted mailbox is an
     * invalid argument to any IMAP command..
     *
     * @returns boolean true if mailbox does not exist
    public boolean isDeleted() ;

    /**
     * Records the Unique Identifier Validity Value for this mailbox.
     *
     * @param uidValidity int the uid validity value must be incremented if
     * the current uid values overlap uid valuse of this are a previous
     * incarnation of the mailbox.
     */
    public void setUidValidity(int uidValidity) ;

    /**
     * Returns current uid validity value
     *
     * @returns int uid validity value
     */
    public int getUidValidity() ;

   /**
     * Records the highest assigned Unique Identifier Value for this mailbox.
     *
     * @param uid int the highest uid assigned to a message in this mailbox.
     */
    public void setHighestUid(int uid) ;

    /**
     * Returns current highest assigned uid value
     *
     * @returns int uid  value
     */
    public int getHighestUid() ;

}
