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
import org.apache.james.imapserver.store.InMemoryStore;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.userrepository.DefaultUser;
import org.apache.mailet.User;

import junit.framework.TestCase;

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
 * @version $Revision: 1.7 $
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
