/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/


package org.apache.james.imapserver;

import junit.framework.TestCase;
import org.apache.james.imapserver.store.ImapMailbox;
import org.apache.james.imapserver.store.InMemoryStore;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.userrepository.DefaultUser;
import org.apache.mailet.User;

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
 *   - Copying messages - need to make sure that the copied message
 *                        is independent of the original
 *  
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision$
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

    /**
     * Tests creation of mailboxes in user's personal space. No namespaces are used.
     */
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

    /**
     * Tests deletion of mailboxes in user's personal space. No namespaces are used.
     * @throws Exception
     */
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

    /**
     * Checks that a mailbox with the supplied name exists, and that its
     * NoSelect flag matches that expected.
     */
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

    /**
     * Asserts that a mailbox with the supplied name doesn't exist.
     */
    private void assertNoMailbox( String name ) throws Exception
    {
        ImapMailbox mailbox = imapHost.getMailbox( user, name );
        assertNull( "Mailbox <" + name + "> should not exist.",
                    mailbox );
    }

    /**
     * Calls {@link ImapHost#createMailbox} with the specified name and the test user.
     */
    private ImapMailbox create( String name ) throws Exception
    {
        return imapHost.createMailbox( user, name );
    }

    /**
     * Calls {@link ImapHost#deleteMailbox} with the specified name and the test user.
     */
    private void delete( String name ) throws Exception
    {
        imapHost.deleteMailbox( user, name );
    }
}
