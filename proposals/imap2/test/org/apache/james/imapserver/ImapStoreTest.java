/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;

import org.apache.james.imapserver.store.ImapMailbox;
import org.apache.james.imapserver.store.ImapStore;
import org.apache.james.imapserver.store.InMemoryStore;
import org.apache.james.imapserver.store.MailboxException;

import junit.framework.TestCase;

import java.util.Collection;

/**
 * A test for implementations of the {@link org.apache.james.imapserver.store.ImapStore} interface.
 *
 * TODO tests to write
 *   - Rename
 *   - List (various combinations)
 *   - 
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.2 $
 */
public class ImapStoreTest extends TestCase
        implements ImapConstants
{
    private ImapStore imapStore;

    public ImapStoreTest( String s )
    {
        super( s );
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        imapStore = getStoreImplementation();
    }

    protected ImapStore getStoreImplementation()
    {
        return new InMemoryStore();
    }

    public void testCreate() throws Exception
    {
        // Get the user namespace.
        ImapMailbox root = imapStore.getMailbox( ImapConstants.USER_NAMESPACE );

        // Create a single mailbox.
        ImapMailbox test = create( root, "test" );
        assertMailbox( "test", true );

        // Create a single mailbox as noselect.
        ImapMailbox noSelect = imapStore.createMailbox( root, "noSelect", false );
        assertMailbox( "noSelect", false );

        // Create children for these mailboxes.
        create( test, "child" );
        create( noSelect, "child" );

        assertMailbox( "test", true );
        assertMailbox( "test.child", true );
        assertMailbox( "noSelect", false );
        assertMailbox( "noSelect.child", true );

        try {
            imapStore.createMailbox( root, "bad.name", true );
            fail( "Shouldn't create mailboxes with compound names." );
        }
        catch ( MailboxException e ) {}
    }

    public void testDelete() throws Exception
    {
        // Simple create/delete
        create("test" );
        assertMailbox( "test", true );

        delete( "test" );
        assertNoMailbox( "test");

        // Create a chain and attempt to delete the parent.
        ImapMailbox one = create( "one" );
        create( one, "two" );
        assertMailbox( "one", true );
        assertMailbox( "one.two", true );

        try {
            delete( "one");
            fail( "Delete of mailbox with children should fail." );
        }
        catch ( MailboxException e ) {
        }
        assertMailbox( "one", true );
        assertMailbox( "one.two", true );

        // Delete the child, then the parent
        delete( "one.two");
        delete( "one");
        assertNoMailbox( "one" );
        assertNoMailbox( "one.two" );
    }

    public void testListMailboxes() throws Exception
    {
        Collection coll;
        coll = list("*");
        assertTrue( coll.isEmpty() );
        coll = list("%");
        assertTrue( coll.isEmpty() );

        ImapMailbox test = create( "test" );
        ImapMailbox testOne = create( test, "one" );
        ImapMailbox testTwo = create( test, "two" );
        ImapMailbox testTwoAaa = create( testTwo, "aaa" );
        ImapMailbox different = create( "different" );
        ImapMailbox differentOne = create( different, "one" );
        ImapMailbox differentTwo = create( different, "two" );

        coll = list("*");
        assertContents( coll, new ImapMailbox[]{test, testOne, testTwo, testTwoAaa, different, differentOne, differentTwo});

        coll = list("%");
        assertContents( coll, new ImapMailbox[]{test, different});

        coll = list("te*");
        assertContents( coll, new ImapMailbox[]{test, testOne, testTwo, testTwoAaa});

        coll = list("te%");
        assertContents( coll, new ImapMailbox[]{test});

        coll = list("test*");
        assertContents( coll, new ImapMailbox[]{test, testOne, testTwo, testTwoAaa});

        // TODO - should this return children?
        coll = list("test%");
        assertContents( coll, new ImapMailbox[]{test});

        coll = list("test.*");
        assertContents( coll, new ImapMailbox[]{testOne, testTwo, testTwoAaa});

        coll = list( "test.%" );
        assertContents( coll, new ImapMailbox[]{testOne, testTwo});

    }

    private void assertContents( Collection coll, ImapMailbox[] imapMailboxes )
            throws Exception
    {
        assertEquals( coll.size(), imapMailboxes.length );
        for ( int i = 0; i < imapMailboxes.length; i++ )
        {
            assertTrue( coll.contains( imapMailboxes[i] ) );
        }
    }

    private void assertMailbox( String name, boolean selectable )
    {
        ImapMailbox mailbox = imapStore.getMailbox( prefixUserNamespace( name ) );
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

    private void assertNoMailbox( String name )
    {
        ImapMailbox mailbox = imapStore.getMailbox( prefixUserNamespace( name ));
        assertNull( "Mailbox <" + name + "> should not exist.",
                    mailbox );
    }

    private ImapMailbox create( String name ) throws Exception
    {
        ImapMailbox root = imapStore.getMailbox( USER_NAMESPACE );
        return create( root, name );
    }

    private ImapMailbox create( ImapMailbox parent, String name )
            throws MailboxException
    {
        return imapStore.createMailbox( parent, name, true );
    }

    private void delete( String name ) throws MailboxException
    {
        ImapMailbox mailbox = imapStore.getMailbox( prefixUserNamespace( name ) );
        imapStore.deleteMailbox( mailbox );
    }

    private Collection list( String pattern ) throws MailboxException
    {
        return imapStore.listMailboxes( prefixUserNamespace( pattern ) );
    }

    private String prefixUserNamespace( String name )
    {
        return USER_NAMESPACE + HIERARCHY_DELIMITER + name;
    }
}
