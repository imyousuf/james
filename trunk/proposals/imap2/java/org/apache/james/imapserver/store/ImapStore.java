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

package org.apache.james.imapserver.store;

import java.util.Collection;

/**
 * Represents the complete mail store for an IMAP server, providing access to
 * and manipulation of all {@link org.apache.james.imapserver.store.ImapMailbox Mailboxes} stored on this server.
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.4 $
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
