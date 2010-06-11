/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.imapserver;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

import org.apache.avalon.*;

/**
 * A host machine that has an IMAP4rev1 messaging server. There should be one
 * instance of this class per instance of James.
 * An IMAP messaging system may span more than one host.
 * <p><code>String</code> parameters representing mailbox names must be the
 * full hierarchical name of the target, with namespace, as used by the
 * specified user. Examples: 
 * '#mail.Inbox' or '#shared.finance.Q2Earnings'.
 * <p>An imap Host must keep track of existing and deleted mailboxes. 
 *
 * References: rfc 2060, rfc 2193, rfc 2221
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1 on 14 Dec 2000
 * @see FolderRecord
 * @see RecordRepository
 */

public interface Host extends Configurable, Composer, Contextualizable,
			      org.apache.avalon.services.Service {
   
    public static final String IMAP_HOST = "IMAP_HOST";


    /**
     * Establishes whether this host is the Home Server for the specified user.
     * Used during login to decide whether a LOGIN_REFERRAL has to be sent to
     * the client.
     *
     * @param username an email address
     * @returns true if inbox (and private mailfolders) are accessible through
     * this host. 
     */
    public boolean isHomeServer (String username);


   /**
    * Establishes if the specified user can access any mailboxes on this host.
    * Used during login process to decide what sort of LOGIN-REFERRAL must be
    * sent to client.
    *
    * @param username an email address
    * @returns true if the specified user has at least read access to any
    * mailboxes on this host.
     */
    public boolean hasLocalAccess (String username);


    /**
     * Returns a reference to an existing Mailbox. The requested mailbox
     * must already exists on this server and the requesting user must have at
     * least lookup rights.
     *
     * @param user email address on whose behalf the request is made.
     * @param mailboxName String name of the target.
     * @returns an Mailbox reference.
     * @throws AccessControlException if the user does not have at least
     * lookup rights.
     * @throws MailboxException if mailbox does not exist locally.
     */
    public ACLMailbox getMailbox(String user, String mailboxName)
	throws AccessControlException, MailboxException;


    /**
     * Returns a reference to a newly created Mailbox. The request should
     * specify a mailbox that does not already exist on this server, that
     * could exist on this server and that the user has rights to create.
     * If a system allocates different namespaces to different hosts then a
     * request to create a mailbox in a namespace not served by this host would
     * be an error.
     * It is an error to create a mailbox with the name of a mailbox that has
     * been deleted, if that name is still in use. 
     *
     * @param user email address on whose behalf the request is made.
     * @param mailboxName String name of the target
     * @returns an Mailbox reference.
     * @throws MailboxException if mailbox already exists, locally or remotely,
     * or if mailbox cannot be created locally.
     * @throws AccessControlException if the user does not have lookup rights
     * for parent or any needed ancestor folder
     * lookup rights.
     * @throws AuthorizationException if mailbox could be created locally but
     * user does not have create rights.
     * @see FolderRecord
     */
    public ACLMailbox createMailbox(String user, String mailboxName)
	throws AccessControlException, AuthorizationException,
	       MailboxException;


    /**
     * Deletes an existing MailBox. Specified mailbox must already exist on
     * this server, and the user must have rights to delete it. (Mailbox delete
     * rights are implementation defined, one way is if the user would have the
     * right to create it).
     * Implementations must track deleted mailboxes
     *
     * @param user email address on whose behalf the request is made.
     * @param mailboxName String name of the target
     * @returns true if mailbox deleted successfully
     * @throws MailboxException if mailbox does not exist locally or is any
     * identities INBOX.
     * @throws AuthorizationException if mailbox exists locally but user does
     * not have rights to delete it.
     * @see FolderRecord
     */
    public boolean deleteMailbox(String user, String mailboxName)
	throws MailboxException, AuthorizationException;


    /**
     * Renames an existing MailBox. The specified mailbox must already
     * exist locally, the requested name must not exist locally already but
     * must be able to be created locally and the user must have rights to
     * delete the existing mailbox and create a mailbox with the new name.
     * Any inferior hierarchical names must also be renamed.
     * If INBOX is renamed, the contents of INBOX are transferred to a new
     * folder with the new name, but INBOX is not deleted. If INBOX has
     * inferior mailboxes these are not renamed.
     * It is an error to create a mailbox with the name of a mailbox that has
     * been deleted, if that name is still in use. 
     * Implementations must track deleted mailboxes
     *
     * @param user email address on whose behalf the request is made.
     * @param oldMailboxName String name of the existing mailbox
     * @param newMailboxName String target new name
     * @returns true if rename completed successfully
     * @throws MailboxException if mailbox does not exist locally, or there
     * is an existing mailbox with the new name. 
     * @throws AuthorizationException if user does not have rights to delete
     * the existing mailbox or create the new mailbox.
     * @see FolderRecord
     */
    public boolean renameMailbox(String user, String oldMailboxName,
				 String newMailboxName)
	throws MailboxException, AuthorizationException;

    /**
     * Releases a reference to a mailbox, allowing Host to do any housekeeping.
     *
     * @param username String user who has finished with this mailbox
     * @param mbox a non-null reference to an ACL Mailbox.
     */
    public void releaseMailbox(String user, ACLMailbox mbox);
 
    /**
     * Returns the namespace which should be used for this user unless they
     * expicitly request another.
     * 
     * @param username String an email address
     * @returns a String of a namespace
     */
    public String getDefaultNamespace(String username);


    /**
     * Return UIDValidity for named mailbox. Implementations should track
     * existing and deleted folders. 
     *
     * @param mailbox String name of the existing mailbox
     * @returns  an integer containing the current UID Validity value.
     */
    //  public int getUIDValidity(String mailbox);


    /**
     * Returns an iterator over an unmodifiable collection of Strings
     * representing mailboxes on this host and their attributes. The specified
     * user must have at least lookup rights for each mailbox returned.
     * If the subscribedOnly flag is set, only mailboxes to which the
     * specified user is currently subscribed should be returned.
     * Implementations that may export circular hierarchies SHOULD restrict the
     * levels of hierarchy returned. The depth suggested by rfc 2683 is 20
     * hierarchy levels.
     * <p>The reference name must be non-empty. If the mailbox name is empty,
     * implementations must not throw either exception but must return a single
     * String (described below) if the reference name specifies a local mailbox
     * accessible to the user and a one-character String containing the
     * hierarchy delimiter of the referenced namespace, otherwise. 
     * <p>Each String returned should be a space seperated triple of name
     * attributes, hierarchy delimiter and full mailbox name.   The mailbox
     * name should include the namespace and be relative to the specified user.
     * <p> RFC comments: Implementations SHOULD return quickly. They SHOULD
     * NOT go to excess trouble to calculate\Marked or \Unmarked status.
     * <p>JAMES comment: By elimination, implementations should usually include
     * \Noinferiors or \Noselect, if appropriate. Also, if the reference name
     * and mailbox name resolve to a single local mailbox, implementations
     * should establish all attributes.
     * <p> Note that servers cannot unilaterally remove mailboxes from the
     * subscribed list. A request with the subscribedOnly flag set that
     * attempts to list a deleted mailbox must return that mailbox with the
     * \Noselect attribute.
     *
     * @param username String non-empty email address of requester
     * @param referenceName String non-empty name, including namespace, of a
     * mailbox or level of mailbox hierarchy, relative to user.
     * @param mailboxName String name of a mailbox possible including a
     * wildcard.
     * @param subscribedOnly only return mailboxes currently subscribed.
     * @returns Collection of strings representing a set of mailboxes.
     * @throws AccessControlException if the user does not have at least
     * lookup rights to at least one mailbox in the set requested.
     * @throws MailboxException if the referenceName is not local or if
     * referenceName and mailbox name resolve to a single mailbox which does
     * not exist locally.
     */
    public Collection listMailboxes(String username, String referenceName,
				  String mailboxName, boolean subscribedOnly)
	throws MailboxException, AccessControlException;

    /**
     * Subscribes a user to a mailbox. The mailbox must exist locally and the
     * user must have at least lookup rights to it.
     *
     * @param username String representation of an email address
     * @param mailbox String representation of a mailbox name.
     * @returns true if subscribe completes successfully
     * @throws AccessControlException if the mailbox exists but the user does
     * not have lookup rights.
     * @throws MailboxException if the mailbox does not exist locally.
     */
    public boolean subscribe(String username, String mailbox)
	throws MailboxException, AccessControlException;

    /**
     * Unsubscribes from a given mailbox. 
     *
     * @param username String representation of an email address
     * @param mailbox String representation of a mailbox name.
     * @returns true if unsubscribe completes successfully
     */
    public boolean unsubscribe(String username, String mailbox)
	throws MailboxException, AccessControlException;


    /**
     * Returns a string giving the status of a mailbox on requested criteria.
     * Currently defined staus items are:
     * MESSAGES - Nummber of messages in mailbox
     * RECENT - Number of messages with \Recent flag set
     * UIDNEXT - The UID that will be assigned to the next message entering
     * the mailbox
     * UIDVALIDITY - The current UIDValidity value for the mailbox
     * UNSEEN - The number of messages which do not have the \Seen flag set.
     *
     * @param username String non-empty email address of requester
     * @param mailboxName String name of a mailbox (no wildcards allowed).
     * @param dataItems Vector of one or more Strings each of a single
     * status item.
     * @returns String consisting of space seperated pairs:
     * dataItem-space-number.
     * @throws AccessControlException if the user does not have at least
     * lookup rights to the mailbox requested.
     * @throws MailboxException if the mailboxName does not exist locally. 
     */
    public String getMailboxStatus(String username, String mailboxName,
				   List dataItems)
	throws MailboxException, AccessControlException;




}

