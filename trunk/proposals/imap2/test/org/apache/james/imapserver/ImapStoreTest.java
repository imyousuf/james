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
 * @version $Revision: 1.5 $
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

    /**
     * Tests creation of mailboxes in the Store.
     */
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

    /**
     * Tests deletion of mailboxes from the store.
     */
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

    /**
     * Tests the {@link ImapStore#listMailboxes} method.
     */
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

    /**
     * Asserts that the supplied collection contains exactly the mailboxes in the
     * array provided.
     */
    private void assertContents( Collection coll, ImapMailbox[] imapMailboxes )
            throws Exception
    {
        assertEquals( coll.size(), imapMailboxes.length );
        for ( int i = 0; i < imapMailboxes.length; i++ )
        {
            assertTrue( coll.contains( imapMailboxes[i] ) );
        }
    }


    /**
     * Checks that a mailbox with the supplied name exists, and that its
     * NoSelect flag matches that expected.
     */
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

    /**
     * Asserts that a mailbox with the supplied name doesn't exist.
     */
    private void assertNoMailbox( String name )
    {
        ImapMailbox mailbox = imapStore.getMailbox( prefixUserNamespace( name ));
        assertNull( "Mailbox <" + name + "> should not exist.",
                    mailbox );
    }

    /**
     * Creates a mailbox with the specified name in the root user namespace.
     */
    private ImapMailbox create( String name ) throws Exception
    {
        ImapMailbox root = imapStore.getMailbox( USER_NAMESPACE );
        return create( root, name );
    }

    /**
     * Creates a mailbox under the parent provided with the specified name.
     */
    private ImapMailbox create( ImapMailbox parent, String name )
            throws MailboxException
    {
        return imapStore.createMailbox( parent, name, true );
    }

    /**
     * Deletes a mailbox from the store.
     */
    private void delete( String name ) throws MailboxException
    {
        ImapMailbox mailbox = imapStore.getMailbox( prefixUserNamespace( name ) );
        imapStore.deleteMailbox( mailbox );
    }

    /**
     * Executes {@link ImapStore#listMailboxes} with the supplied pattern.
     */
    private Collection list( String pattern ) throws MailboxException
    {
        return imapStore.listMailboxes( prefixUserNamespace( pattern ) );
    }

    /**
     * Prefixes a mailbox name with the "#mail" namespace.
     */
    private String prefixUserNamespace( String name )
    {
        return USER_NAMESPACE + HIERARCHY_DELIMITER + name;
    }
}
