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

package org.apache.james.imapserver;

import org.apache.james.imapserver.store.ImapMailbox;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.mailet.User;

import java.util.Collection;

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
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 1.9 $
 */
public interface ImapHost
{

    String ROLE = "org.apache.james.imapserver.ImapHost";

    String IMAP_HOST = "IMAP_HOST";

    /**
     * Returns the hierarchy delimiter for mailboxes on this host.
     * @return The hierarchy delimiter character.
     */
    char getHierarchyDelimiter();

    /**
     * Returns a reference to an existing Mailbox. The requested mailbox
     * must already exists on this server and the requesting user must have at
     * least lookup rights.
     *
     * TODO: should default behaviour be to return null?
     *
     * @param user User making the request.
     * @param mailboxName String name of the target.
     * @return an Mailbox reference.
     */
    ImapMailbox getMailbox( User user, String mailboxName );

    /**
     * Returns a reference to an existing Mailbox.
     * If mustExist == true, an exception is thrown if the requested mailbox
     * doesn't exists on this server or the requesting user doesn't have at
     * least lookup rights. If mustExist == false, simply returns <code>null</code> in
     * this case.
     *
     * @param user User making the request.
     * @param mailboxName String name of the target.
     * @param mustExist Specified behaviour where a mailbox is missing or non-viewable.
     * @return an Mailbox reference.
     * @throws org.apache.james.imapserver.store.MailboxException if mailbox does not exist locally, and mustExist is true.
     */
    ImapMailbox getMailbox( User user, String mailboxName, boolean mustExist )
            throws MailboxException;

    /**
     * Returns a reference to the user's INBOX. The user must have been
     * registered on the server by the {@link #createPrivateMailAccount} method.
     * @param user The user making the request.
     * @return The user's Inbox.
     * @throws org.apache.james.imapserver.store.MailboxException if the user doesn't have an inbox on this server.
     */
    ImapMailbox getInbox( User user ) throws MailboxException;

    /**
     * Registers a user with the ImapHost, creating a personal mail space and
     * INBOX for that user.
     * @param user The user to register with the Host.
     * @throws org.apache.james.imapserver.store.MailboxException if an error occurred creating the user account.
     */
    void createPrivateMailAccount( User user ) throws MailboxException;

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
     * @param user User making the request.
     * @param mailboxName String name of the target
     * @return an Mailbox reference.
     * @throws org.apache.james.imapserver.store.MailboxException if mailbox already exists, locally or remotely,
     * or if mailbox cannot be created locally.
     * @throws AuthorizationException if mailbox could be created locally but
     * user does not have create rights.
     */
    ImapMailbox createMailbox( User user, String mailboxName )
            throws AuthorizationException, MailboxException;


    /**
     * Deletes an existing MailBox. Specified mailbox must already exist on
     * this server, and the user must have rights to delete it. (Mailbox delete
     * rights are implementation defined, one way is if the user would have the
     * right to create it).
     *
     * @param user User making the request.
     * @param mailboxName String name of the target
     * @throws org.apache.james.imapserver.store.MailboxException if mailbox does not exist locally or is any
     * identities INBOX.
     * @throws AuthorizationException if mailbox exists locally but user does
     * not have rights to delete it.
     */
    void deleteMailbox( User user, String mailboxName )
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
     *
     * @param user User making the request.
     * @param oldMailboxName String name of the existing mailbox
     * @param newMailboxName String target new name
     * @throws org.apache.james.imapserver.store.MailboxException if mailbox does not exist locally, or there
     * is an existing mailbox with the new name.
     * @throws AuthorizationException if user does not have rights to delete
     * the existing mailbox or create the new mailbox.
     */
    void renameMailbox( User user,
                        String oldMailboxName,
                        String newMailboxName )
            throws MailboxException, AuthorizationException;

    /**
     * Returns an collection of mailboxes on this host. The specified
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
     *
     * @param user User making the request
     * @param mailboxPattern String name of a mailbox possible including a
     * wildcard.
     * @return Collection of mailboxes matching the pattern.
     * @throws org.apache.james.imapserver.store.MailboxException if the referenceName is not local or if
     * referenceName and mailbox name resolve to a single mailbox which does
     * not exist locally.
     */
    Collection listMailboxes( User user,
                              String mailboxPattern )
            throws MailboxException;

    /**
     * Returns an collection of mailboxes on this host. The specified
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
     * subscribed list. A request that attempts to list a deleted, but subscribed,
     * mailbox must return that mailbox with the \Noselect attribute.
     *
     * @param user User making the request
     * @param mailboxPattern String name of a mailbox possible including a
     * wildcard.
     * @return Collection of mailboxes matching the pattern.
     * @throws org.apache.james.imapserver.store.MailboxException if the referenceName is not local or if
     * referenceName and mailbox name resolve to a single mailbox which does
     * not exist locally.
     */
    Collection listSubscribedMailboxes( User user,
                                        String mailboxPattern)
            throws MailboxException;

    /**
     * Subscribes a user to a mailbox. The mailbox must exist locally and the
     * user must have at least lookup rights to it.
     *
     * @param user User making the request
     * @param mailbox String representation of a mailbox name.
     * @throws org.apache.james.imapserver.store.MailboxException if the mailbox does not exist locally (for the user).
     */
    void subscribe( User user, String mailbox )
            throws MailboxException;

    /**
     * Unsubscribes from a given mailbox.
     *
     * @param user String representation of an email address
     * @param mailbox String representation of a mailbox name.
     */
    void unsubscribe( User user, String mailbox )
            throws MailboxException;

}

