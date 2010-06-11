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

package org.apache.james.imapserver.store;

import java.util.Collection;

/**
 * Represents the complete mail store for an IMAP server, providing access to
 * and manipulation of all {@link org.apache.james.imapserver.store.ImapMailbox Mailboxes} stored on this server.
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.1.2.2 $
 */
public interface ImapStore
{
    /**
     * Retrieves a mailbox based on a fully qualified name.
     * @param qualifiedMailboxName
     * @return The mailbox if present, or <code>null</code> if not.
     */
    ImapMailbox getMailbox( String qualifiedMailboxName );

    /**
     * Looks up a child mailbox of the supplied parent with the name given.
     * @param parent The parent mailbox
     * @param mailboxName The name of the child to lookup
     * @return The child mailbox, or <code>null</code> if not found.
     */
    ImapMailbox getMailbox( ImapMailbox parent, String mailboxName );

    /**
     * @param parent A mailbox from this store.
     * @return A read-only collection of {@link ImapMailbox} instances, which
     *         are the children of the supplied parent.
     */
    Collection getChildren( ImapMailbox parent );

    /**
     * Creates a mailbox under the supplied parent with the given name.
     * If specified, the mailbox created will be made selectable (able to store messages).
     * @param parent A mailbox from this store.
     * @param mailboxName The name of the mailbox to create.
     * @param selectable If <code>true</code>, the mailbox will be created to store messages.
     * @return The created mailbox
     * @throws MailboxException If the mailbox couldn't be created.
     */
    ImapMailbox createMailbox( ImapMailbox parent,
                               String mailboxName,
                               boolean selectable )
            throws MailboxException;

    /**
     * Tells the store to make the supplied mailbox selectable or not (able to store
     * messages). The returned mailbox may be a new instance, and the supplied mailbox
     * may no longer be valid.
     * @param mailbox The mailbox to modify.
     * @param selectable Whether this mailbox should be able to store messages.
     * @return The modified mailbox
     */
    ImapMailbox setSelectable( ImapMailbox mailbox, boolean selectable );

    /**
     * Deletes the supplied mailbox from the store. To be deleted, mailboxes
     * must be empty of messages, and not have any children.
     * @param mailbox A mailbox from this store.
     * @throws MailboxException If the mailbox couldn't be deleted.
     */
    void deleteMailbox( ImapMailbox mailbox ) throws MailboxException;

    /**
     * Renames the mailbox with the new name.
     * @param existingMailbox A mailbox from this store.
     * @param newName The new name for the mailbox.
     * @throws MailboxException If the mailbox couldn't be renamed
     */
    void renameMailbox( ImapMailbox existingMailbox, String newName )
            throws MailboxException;

    /**
     * Lists all of the mailboxes in the store which have a name
     *  matching the supplied search pattern.
     * <pre>
     * Valid wildcards are:
     *          '*' - matches any number of characters, including the hierarchy delimiter
     *          '%' - matches any number of characters, but not the hierarchy delimiter
     *
     * @param searchPattern The pattern to match mailboxes
     * @return A read-only collection of mailboxes which match this pattern
     * @throws MailboxException If the list operation failed
     */
    Collection listMailboxes( String searchPattern ) throws MailboxException;

}
