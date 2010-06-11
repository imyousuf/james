/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;

import java.util.List;
import java.util.Map;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeMessage;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.james.AccessControlException;
import org.apache.james.AuthorizationException;
import org.apache.james.core.EnhancedMimeMessage;
import org.apache.mailet.Mail;

/**
 * Interface for objects representing an IMAP4rev1 mailbox (folder). Contains
 * logical information and provides a simple API. There should be one instance
 * of this class for every open IMAP mailbox.
 * Implementations may choose to store this object or recreate it on access.
 * Storing is recommended.
 * <p>Several methods throw AccessControlException. In normal use, these
 * shouldn't get thrown because the Host will have checked access before
 * returning a reference to this mailbox. However, having the methods here
 * throw this exception allows the acl to be changed while a mailbox is
 * selected. 
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
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1 on 14 Dec 2000
 */
public interface Mailbox 
    extends Configurable, Composable {

    String SYSTEM_FLAGS = "\\Seen \\Answered \\Flagged \\Deleted \\Draft";
    String RECENT_FLAG =  "\\Recent"; 

    /**
     * Returns name of this mailbox relative to its parent in the mailbox
     * hierarchy.
     * Example: 'NewIdeas'
     *
     * @returns String name of mailbox relative to its immeadiate parent in
     * the mailbox hierarchy.
     */
    String getName();

    /**
     * Returns absolute, that is user-independent, hierarchical name of
     * mailbox (including namespace)
     * Example: '#mail.fred.flintstone.apache.James.NewIdeas'
     *
     * @returns String name of mailbox in absolute form
     */
    String getAbsoluteName();

    /** Returns namespace starting with namespace token.
     * Example: '#mail'
     *
     * @returns String containing user-independent namespace of this mailbox.
     */
    //   public String getNamespace();

    /** Returns true if the argument is the relative or absolute name of
     * this mailbox
     *
     * @param name possible name for this Mailbox
     * @returns true if name matches either getName() or getAbsoluteName()
     */
    boolean matchesName(String name);

    /**
     * Returns the current unique id validity value of this mailbox.
     *
     * @returns int current 32 bit unique id validity value of this mailbox
     */
    int getUIDValidity();

    /**
     * Returns the 32 bit uid available for the next message.
     *
     * @returns int the next UID that would be used.
     */
    int getNextUID();

    /**
     * Returns mailbox size in octets. Should only include actual messages
     * and not any implementation-specific data, such as message attributes.
     *
     * @returns int mailbox size in octets
     */
    int getMailboxSize();

    /**
     * Indicates if child folders may be created. It does not indicate which
     * users can create child folders.
     *
     * @returns boolean TRUE if inferiors aree allowed
     */
    boolean getInferiorsAllowed();

    /**
     * Indicates if this folder may be selected by the specified user.
     * Requires both that the mailbox is not NotSelectableByAnyone and that the
     * user has at least read rights. It does not indicate whether user
     * can write to mailbox
     *
     * @param username String represnting user
     * @returns boolean TRUE if specified user can Select mailbox.
     * @throws AccessControlException if username does not have lookup rights
     */
    boolean isSelectable(String username) throws AccessControlException;

    /**
     * Indicates that messages have been added since this mailbox was last
     * selected by any user.
     *
     * @returns boolean TRUE if new messages since any user last selected
     * mailbox
     */
    boolean isMarked();

    /**
     * Returns all flags supported by this mailbox.
     * e.g. \Answered \Deleted
     *
     * @returns String a space seperated list of message flags which are
     * supported by this mailbox.
     */
    String getSupportedFlags();

    /**
     * Indicates if specified user can change any flag on a permanent basis,
     * except for \Recent which can never be changed by a user.
     *
     * @param username String represnting user
     * @returns true if specified user can change all flags permanently.
     * @throws AccessControlException if username does not have lookup rights
     */
    boolean allFlags(String username) throws AccessControlException;

    /**
     * Indicates which flags this user can change permanently. If allFlags()
     * returns true for this user, then this method must have the same return
     * value as getSupportedFlags.
     *
     * @param username String represnting user
     * @returns String a space seperated list of message flags which this user
     * can set permanently
     */
    String getPermanentFlags( String username )
        throws AccessControlException;

    /**
     * Indicates number of messages in folder
     *
     * @returns int number of messages
     */
    int getExists();

    /**
     * Indicates no of messages with \Recent flag set
     *
     * @returns int no of messages with \Recent flag set
     */
    int getRecent();


    /**
     * Remove \Recent flag from all messages in mailbox. Should be called
     * whenever a user session finishes.
     */
    void unsetRecent();

    /** 
     * Indicates the oldest unseen message for the specified user. 
     *
     * @returns int Message Sequence Number of first message without \Seen
     * flag set for this User.
     * <br> -1 means all messages have \Seen flag set for this user.
     * <br> 0 means no message (Seen or unseen) in this mailbox.
     */
    int getOldestUnseen( String user );

   /** 
     * Indicates the number of  unseen messages for the specified user. 
     *
     * @returns int number of messages without \Seen flag set for this User.
     */
    int getUnseen( String user );

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
    boolean isReadOnly( String username ) 
        throws AccessControlException;

    /**
     * Mailbox Events are used to inform registered listeners of events in the
     * Mailbox.
     * Example if mail is delivered to an Inbox or if another user appends/ 
     * deletes a message.
     */
    void addMailboxEventListener( MailboxEventListener mel );
    void removeMailboxEventListener( MailboxEventListener mel );

    /**
     * Stores a message in this mailbox. User must have insert rights.
     *
     * @param mail the message to be stored
     * @param username String represnting user
     * @returns boolean true if successful
     * @throws AccessControlException if username does not have lookup rights for this mailbox.
     * @throws AuthorizationException if username has lookup rights but does not have insert rights.
     */
    boolean store( MimeMessage message, String username )
        throws AccessControlException, AuthorizationException, IllegalArgumentException;

    /**
     * Stores a message in this mailbox, using passed MessageAttributes and
     * Flags. User must have insert rights.
     *
     * @param mail the message to be stored
     * @param username String represnting user
     * @param attrs non-null MessageAttributes for use with this Message
     * @param flags a Flags object for this message
     * @returns boolean true if successful
     * @throws AccessControlException if username does not have lookup rights
     * for this mailbox.
     * @throws AuthorizationException if username has lookup rights but does
     * not have insert rights.
     */
    boolean store( MimeMessage message, 
                   String username,
                   MessageAttributes attrs, 
                   Flags flags )
        throws AccessControlException, AuthorizationException, IllegalArgumentException;

    /**
     * Retrieves a message given a message sequence number.
     *
     * @param msn the message sequence number 
     * @param username String represnting user
     * @returns a Mail object containing the message, null if no message with
     * the given msn.
     * @throws AccessControlException if user does not have lookup rights for
     * this mailbox.
     * @throws AuthorizationException if user has lookup rights but does not
     * have read rights.
     */
    EnhancedMimeMessage retrieve( int msn, String user )
        throws AccessControlException, AuthorizationException;

    /**
     * Retrieves a message given a unique identifier.
     *
     * @param uid the unique identifier of a message
     * @param username String represnting user
     * @returns a Mail object containing the message, null if no message with
     * the given msn.
     * @throws AccessControlException if user does not have read rights for
     * this mailbox.
     * @throws AuthorizationException if user has lookup rights but does not
     * have read rights.
     */
    EnhancedMimeMessage retrieveUID( int uid, String user )
        throws AccessControlException, AuthorizationException;

    /**
     * Marks a message for deletion given a message sequence number.
     *
     * @param msn the message sequence number 
     * @param username String represnting user
     * @returns boolean true if successful.
     * @throws AccessControlException if user does not have read rights for
     * this mailbox.
     * @throws AuthorizationException if user has lookup rights but does not
     * have delete rights.
     */
    boolean markDeleted( int msn, String user )
        throws AccessControlException, AuthorizationException;

    /**
     * Marks a message for deletion given a unique identifier.
     *
     * @param uidunique identifier
     * @param username String represnting user
     * @returns boolean true if successful, false if failed including no
     * message with the given uid.
     * @throws AccessControlException if user does not have read rights for
     * this mailbox.
     * @throws AuthorizationException if user has lookup rights but does not
     * have delete rights.
     */
    boolean markDeletedUID( int uid, String user )
        throws AccessControlException, AuthorizationException;

    /**
     * Returns the message attributes for a message.
     *
     * @param msn message sequence number
     * @param username String represnting user
     * @returns MessageAttributes for message, null if no such message.
     * Changing the MessageAttributes object must not affect the actual
     * MessageAttributes.
     * @throws AccessControlException if user does not have read rights for
     * this mailbox.
     * @throws AuthorizationException if user has lookup rights but does not
     * have delete rights.
     */
    MessageAttributes getMessageAttributes( int msn, String user )
        throws AccessControlException, AuthorizationException;

    /**
     * Returns the message attributes for a message.
     *
     * @param uid unique identifier
     * @param username String represnting user
     * @returns MessageAttributes for message, null if no such message.
     * Changing the MessageAttributes object must not affect the actual
     * MessageAttributes.
     * @throws AccessControlException if user does not have read rights for
     * this mailbox.
     * @throws AuthorizationException if user has lookup rights but does not
     * have delete rights.
     */
    MessageAttributes getMessageAttributesUID( int uid, String user )
        throws AccessControlException, AuthorizationException;

    /**
     * Updates the attributes of a message.
     *
     * @param MessageAttributes of a message already in this Mailbox
     * @throws AccessControlException if user does not have read rights for
     * this mailbox.
     * @throws AuthorizationException if user has lookup rights but does not
     * have delete rights.
     */
    boolean updateMessageAttributes( MessageAttributes attrs, String user )
        throws AccessControlException, AuthorizationException;

    /**
     * Get the IMAP-formatted String of flags for specified message.
     *
     * @param msn message sequence number for a message in this mailbox
     * @param username String represnting user
     * @returns flags for this message and user, null if no such message.
     * @throws AccessControlException if user does not have lookup rights for
     * this mailbox.
     * @throws AuthorizationException if user has lookup rights but does not
     * have read rights.
     */
    String getFlags( int msn, String user )
        throws AccessControlException, AuthorizationException;

   /**
     * Get the IMAP-formatted String of flags for specified message.
     *
     * @param uid UniqueIdentifier for a message in this mailbox
     * @param username String represnting user
     * @returns flags for this message and user, null if no such message.
     * @throws AccessControlException if user does not have lookup rights for
     * this mailbox.
     * @throws AuthorizationException if user has lookup rights but does not
     * have read rights.
     */
    String getFlagsUID(int uid, String user)
        throws AccessControlException, AuthorizationException;
    
    /**
     * Updates the flags of a message. 
     *
     * @param msn message sequence number for a message in this mailbox
     * @param username String represnting user
     * @param request IMAP formatted string of flag request
     * @throws AccessControlException if user does not have read rights for
     * this mailbox.
     * @throws AuthorizationException if user has lookup rights but does not
     * have delete rights.
     */
    boolean setFlags( int msn, String user, String request )
        throws AccessControlException, AuthorizationException, IllegalArgumentException;

    /**
     * Updates the flags of a message. 
     *
     * @param uid UniqueIdentifier for a message in this mailbox
     * @param username String represnting user
     * @param request IMAP formatted string of flag request
     * @throws AccessControlException if user does not have read rights for
     * this mailbox.
     * @throws AuthorizationException if user has lookup rights but does not
     * have delete rights.
     */
    boolean setFlagsUID( int uid, String user, String request )
        throws AccessControlException, AuthorizationException, IllegalArgumentException;

    /**
     * Returns the Internet Headers for a message.  These are the top-level
     * headers only, ie not MIME part headers or headers of encapsulated
     * messages.
     *
     * @param msn message sequence number
     * @param username String represnting user
     * @returns InternetHeaders for message, null if no such message.
     * Changing the InternetHeaders object must not affect the actual
     * InternetHeaders of the underlying message.
     * @throws AccessControlException if user does not have read rights for
     * this mailbox.
     * @throws AuthorizationException if user has lookup rights but does not
     * have delete rights.
     */
    InternetHeaders getInternetHeaders( int msn, String user )
        throws AccessControlException, AuthorizationException;

    /**
     * Returns the Internet Headers for a message.  These are the top-level
     * headers only, ie not MIME part headers or headers of encapsulated
     * messages.
     *
     * @param uid UniqueIdentifier for a message in this mailbox
     * @param username String represnting user
     * @returns InternetHeaders for message, null if no such message.
     * Changing the InternetHeaders object must not affect the actual
     * InternetHeaders of the underlying message.
     * @throws AccessControlException if user does not have read rights for
     * this mailbox.
     * @throws AuthorizationException if user has lookup rights but does not
     * have delete rights.
     */
    InternetHeaders getInternetHeadersUID( int uid, String user )
        throws AccessControlException, AuthorizationException;

    /**
     * Removes all messages marked Deleted.  User must have delete rights.
     *
     * @param username String represnting user
     * @returns true if successful
     * @throws AccessControlException if user does not have read rights for
     * this mailbox.
     * @throws AuthorizationException if user has delete rights but does not
     * have delete rights.
     */
    boolean expunge( String user )
        throws AccessControlException, AuthorizationException, IllegalArgumentException;

    /**
     * Establishes if specified user has lookup rights for this mailbox.
     *
     * @param username String represnting user
     * @returns true if user has at least lookup rights
     */
    boolean hasLookupRights( String user );
        
    /**
     * Establishes if specified user has create rights for this mailbox.
     *
     * @param username String represnting user
     * @returns true if user has at create rights
     * @throws AccessControlException if user does not have lookup rights for
     * this mailbox.
     */
    boolean hasCreateRights( String user )
        throws AccessControlException;

    /**
     * Lists uids of messages in mailbox indexed by MSN.
     *
     * @param username String represnting user
     * @returns List of Integers wrapping uids of message
     * @throws AccessControlException if user does not have lookup rights for
     * this mailbox.
     */
    List listUIDs( String user );

    /**
     * Returns true once this Mailbox has been checkpointed.
     * This may include resolving in-memory state with disk state.
     * Implementations not requiring checkpointing should return immeadiately.
     *
     * @returns true if check completes normally, false otherwise.
     */
    boolean checkpoint();

    /**
     * Mark this mailbox as not selectable by anyone. 
     * Example folders at the roots of hierarchies, e. #mail for each user.
     *
     * @param state true if folder is not selectable by anyone
     */
    void setNotSelectableByAnyone( boolean state );

    boolean isNotSelectableByAnyone();

    /**
     * Gets map of users to number of unseen messages. User key will only be
     * present if getOldestUnseen() has been called, usually as a result of
     * an IMAP SELECT or EXAMINE.
     */
    Map getUnseenByUser();
}
 

