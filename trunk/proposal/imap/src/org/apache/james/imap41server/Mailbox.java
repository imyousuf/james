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
import org.apache.james.MailRepository;

/**
 * Interface for objects representing an IMAP4rev1 mailbox (folder). Contains
 * logical information and provides a simple API. There should be one instance
 * of this class for every open IMAP mailbox.
 * Implementations may choose to store this object or recreate it on access.
 * Storing is recommended.
 *
 * Mailbox Related Flags (rfc2060 name attributes)
 *     \Noinferiors   It is not possible for any child levels of hierarchy to
 * exist under this name; no child levels exist now and none can be created
 * in the future.
 *     \Noselect      It is not possible to use this name as a selectable
 * mailbox.
 *     \Marked        The mailbox has been marked "interesting" by the server;
 * the mailbox probably contains messages that have been added since the last
 * time the mailbox was selected.
 *      \Unmarked      The mailbox does not contain any additional messages
 * since the last time the mailbox was selected.
 *
 * Message related flags. 
 * The flags allowed per message are specific to each mailbox.
 * The minimum list (rfc2060 system flags) is:
 *  \Seen       Message has been read
 *  \Answered   Message has been answered
 *  \Flagged    Message is "flagged" for urgent/special attention
 *  \Deleted    Message is "deleted" for removal by later EXPUNGE
 *  \Draft      Message has not completed composition (marked as a draft).
 *  \Recent     Message is "recently" arrived in this mailbox.  This session
 * is the first session to have been notified about this message; subsequent
 * sessions will not see \Recent set for this message.  This flag can not be
 * altered by the client.
 *              If it is not possible to determine whether or not this session
 * is the first session to be notified about a message, then that message
 * SHOULD be considered recent.
 *
 * Reference: RFC 2060 
 * @author Charles Benett <charles@benett1.demon.co.uk>
 * @version 0.1
 */

public interface Mailbox extends MailRepository {

  
    /**
     * Returns name of this mailbox relative to its parent in the mailbox
     * hierarchy.
     * Example: 'NewIdeas'
     *
     * @returns String name of mailbox relative to its immeadiate parent in
     * the mailbox hierarchy.
     */
    public String getName();

    /**
     * Returns absolute, that is user-independent, hierarchical name of
     * mailbox (including namespace)
     * Example: '#mail.fred.flintstone.apache.James.NewIdeas'
     *
     * @returns String name of mailbox in absolute form
     */
    public String getAbsoluteName();

    /** Returns namespace starting with namespace token.
     * Example: '#mail'
     *
     * @returns String containing user-independent namespace of this mailbox.
     */
    public String getNamespace();

    /** Returns true if the argument is the relative or absolute name of
     * this mailbox
     *
     * @param name possible name for this Mailbox
     * @returns true if name matches either getName() or getAbsoluteName()
     */
    public boolean matchesName(String name);

    /**
     * Returns the current unique id validity value of this mailbox.
     *
     * @returns int current 32 bit unique id validity value of this mailbox
     */
    public int getUIDValidity();

    /**
     * Returns the 32 bit uid available for the next message.
     *
     * @returns int the next UID that would be used.
     */
    public int getNextUID();

    /**
     * Returns mailbox size in octets. Should only include actual messages
     * and not any implementation-specific data, such as message attributes.
     *
     * @returns int mailbox size in octets
     */
    public int getMailboxSize();

    /**
     * Indicates if child folders may be created. It does not indicate which
     * users can create child folders.
     *
     * @returns boolean TRUE if inferiors aree allowed
     */
    public boolean getInferiorsAllowed();

    /**
     * Indicates if this folder may be selected by the specified user. Requires
     * user to have at least read rights. It does not indicate whether user
     * can write to mailbox
     *
     * @param username String represnting user
     * @returns boolean TRUE if specified user can Select mailbox.
     */
    public boolean isSelectable(String username);

    /**
     * Indicates that messages have been added since this mailbox was last
     * selected by any user.
     *
     * @returns boolean TRUE if new messages since any user last selected
     * mailbox
     */
    public boolean isMarked();


    /**
     * Returns all flags supported by this mailbox.
     * e.g. \Answered \Deleted
     *
     * @returns String a space seperated list of message flags which are
     * supported by this mailbox.
     */
    public String getSupportedFlags();


    /**
     * Indicates if specified user can cahnge any flag on a permanent basis
     *
     * @param username String represnting user
     * @returns true if specified user can change all flags permanently.
     */
    public boolean allFlags(String username);

    /**
     * Indicates which flags this user can change permanently. If allFlags()
     * returns true for this user, then this method must have the same return
     * value as getSupportedFlags.
     *
     * @param username String represnting user
     * @returns String a space seperated list of message flags which this user
     * can set permanently
     */
    public String getPermanentFlags(String username);

    /**
     * Indicates number of messages in folder
     *
     * @returns int number of messages
     */
    public int getExists();

    /**
     * Indicates no of messages with \Recent flag set
     *
     * @returns int no of messages with \Recent flag set
     */
    public int getRecent();


    /**
     * Indicates if there are any unseen messages in this mailbox.
     *
     * @rReturns true if any of the messages has their \Unseen flag set
     */
    public boolean hasUnseenMessages();

    /** 
     * Indicates which is the first unseen message. 
     *
     * @returns int Message Sequence Number of first message with \Unseen
     * flag set
     */
    public int getUnseen();

    /**
     * Provides a reference to the access control list for this mailbox.
     *
     * @returns the AccessControlList for this Mailbox
     */
    public ACL getACL();

    /**
     * Indicates state in which  the mailbox will be opened by specified user.
     * A return value of true indicates Read Only, false indicates Read-Write
     * and an AccessControlException is thrown if user does not have read
     * rights.
     * <p>Implementations decide if Read Only means only lookup and read
     * rights (lr) or lookup, read and keep seen rights (lrs). This may even
     * vary between mailboxes.
     *
     * @param username String represnting user
     * @returns true if specified user can only open the mailbox Read-Only.
     * @throws AccessControlException if the user can not open this mailbox
     * at least Read-Only.
     */
    public boolean isReadOnly(String username) throws AccessControlException;


    /** Mailbox Events are used to inform registered listeners of events in the Mailbox.
     * E.g. if mail is delivered to an Inbox or if another user appends/ deletes a message.
     */
    public void addMailboxEventListener(MailboxEventListener mel) ;
    public void removeMailboxEventListener(MailboxEventListener mel);
}
 

