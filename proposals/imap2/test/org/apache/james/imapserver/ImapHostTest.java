/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;

import org.apache.james.imapserver.store.ImapStore;
import org.apache.james.imapserver.store.InMemoryStore;
import org.apache.james.imapserver.store.ImapMailbox;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.imapserver.ImapConstants;
import org.apache.james.services.User;
import org.apache.james.userrepository.DefaultUser;
import junit.framework.TestCase;

import java.util.Collection;

/**
 * A test for implementations of the {@link ImapHost} interface.
 *
 * TODO Tests to write:
 *   - Creating and accessing mailboxes with qualified names
 *   - Create existing mailbox
 *   - Delete Inbox
 *   - Rename
 *   - Rename Inbox
 *   - ListMailboxes
 *  
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.1 $
 */
public class ImapHostTest extends TestCase
        implements ImapConstants
{
    private ImapHost imapHost;
    private User user;

    public ImapHostTest( String s )
    {
        super( s );
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        user = new DefaultUser( "user", null );

        imapHost = getHostImplementation();
        imapHost.createPrivateMailAccount( user );
    }

    protected ImapHost getHostImplementation()
    {
        return new JamesImapHost( new InMemoryStore() );
    }

    public void testCreatePersonal() throws Exception
    {
        // Create a single mailbox.
        create( "test" );
        assertMailbox( "test", true );

        // Create a child of an existing mailbox.
        create("test.another" );
        assertMailbox( "test", true );
        assertMailbox( "test.another", true );

        // A multi-level create, which creates intervening mailboxes,
        // with the \NoSelect attribute set.
        create( "this.is.another.mailbox");
        assertMailbox( "this", false );
        assertMailbox( "this.is", false );
        assertMailbox( "this.is.another", false );
        assertMailbox( "this.is.another.mailbox", true );

        // Create a child of an existing, no-select mailbox.
        create( "this.is.yet.another.mailbox");
        assertMailbox( "this", false );
        assertMailbox( "this.is", false );
        assertMailbox( "this.is.yet", false );
        assertMailbox( "this.is.yet.another", false );
        assertMailbox( "this.is.yet.another.mailbox", true );
    }

    public void testDelete() throws Exception
    {
        // Simple create/delete
        create( "test" );
        assertMailbox( "test", true );
        delete( "test" );
        assertNoMailbox( "test");

        // Create a chain and delete the parent.
        // Child should remain, and parent be switched to NoSelect.
        create( "one" );
        create( "one.two" );
        assertMailbox( "one", true );
        assertMailbox( "one.two", true );
        delete( "one");
        assertMailbox( "one", false);
        assertMailbox( "one.two", true );

        // Can't delete mailbox with NoSelect attribute and children.
        try
        {
            delete( "one" );
            fail( "Should not be able to delete a non-selectabl mailbox which has children." );
        }
        catch( MailboxException e )
        {
            // TODO check for correct exception.
        }

        // Delete the child, then the non-selectable parent
        delete( "one.two");
        delete( "one");
        assertNoMailbox( "one.two" );
        assertNoMailbox( "one" );
    }

    private void assertMailbox( String name, boolean selectable ) throws MailboxException
    {
        ImapMailbox mailbox = imapHost.getMailbox( user, name );
        assertNotNull( "Mailbox <" + name + "> expected to exist in store.",
                       mailbox );
        if ( selectable )
        {
            assertTrue( "Mailbox <" + name + "> not selectable.",
                        mailbox.isSelectable() );
        }
        else
        {
            assertTrue( "Mailbox <" + name + "> should not be selectable.",
                        ! mailbox.isSelectable() );
        }
    }

    private void assertNoMailbox( String name ) throws Exception
    {
        ImapMailbox mailbox = imapHost.getMailbox( user, name );
        assertNull( "Mailbox <" + name + "> should not exist.",
                    mailbox );
    }

    private ImapMailbox create( String name ) throws Exception
    {
        return imapHost.createMailbox( user, name );
    }

    private void delete( String name ) throws Exception
    {
        imapHost.deleteMailbox( user, name );
    }
}
