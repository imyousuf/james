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
import org.apache.james.imapserver.store.MessageFlags;
import org.apache.mailet.User;
import org.apache.mailet.UsersRepository;

import javax.mail.Flags;
import java.util.Map;
import java.util.Iterator;

/**
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.7 $
 */
public final class ImapSessionImpl implements ImapSession
{
    private ImapSessionState state = ImapSessionState.NON_AUTHENTICATED;
    private User user = null;
    private ImapSessionMailbox selectedMailbox = null;

    private String clientHostName;
    private String clientAddress;

    // TODO these shouldn't be in here - they can be provided directly to command components.
    private ImapHandler handler;
    private ImapHost imapHost;
    private UsersRepository users;

    public ImapSessionImpl( ImapHost imapHost,
                            UsersRepository users,
                            ImapHandler handler,
                            String clientHostName,
                            String clientAddress )
    {
        this.imapHost = imapHost;
        this.users = users;
        this.handler = handler;
        this.clientHostName = clientHostName;
        this.clientAddress = clientAddress;
    }

    public ImapHost getHost()
    {
        return imapHost;
    }

    public void unsolicitedResponses( ImapResponse request ) throws MailboxException {
        unsolicitedResponses(request, false);
    }

    public void unsolicitedResponses( ImapResponse request, boolean omitExpunged ) throws MailboxException {
        ImapSessionMailbox selected = getSelected();
        if (selected != null) {
            // New message response
            // TODO: need RECENT...
            if (selected._sizeChanged) {
                request.existsResponse(selected.getMessageCount());
                selected._sizeChanged = false;
            }

            Map flagUpdates = selected.getFlagUpdates();
            Iterator iter = flagUpdates.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                int msn = ((Integer) entry.getKey()).intValue();
                Flags updatedFlags = (Flags) entry.getValue();
                StringBuffer out = new StringBuffer( "FLAGS " );
                out.append( MessageFlags.format(updatedFlags) );
                request.fetchResponse(msn, out.toString());

            }

            if (! omitExpunged) {
                // Expunge response - TODO can't send on certain commands
                int[] expunged = selected.getExpunged();
                for (int i = 0; i < expunged.length; i++) {
                    int msn = expunged[i];
                    request.expungeResponse(msn);
                }
            }
        }
    }

    public void closeConnection()
    {
        handler.resetHandler();
    }

    public UsersRepository getUsers()
    {
        return users;
    }

    public String getClientHostname()
    {
        return clientHostName;
    }

    public String getClientIP()
    {
        return clientAddress;
    }

    public void setAuthenticated( User user )
    {
        this.state = ImapSessionState.AUTHENTICATED;
        this.user = user;
    }

    public User getUser()
    {
        return this.user;
    }

    public void deselect()
    {
        this.state = ImapSessionState.AUTHENTICATED;
        // TODO is there more to do here, to cleanup the mailbox.
        this.selectedMailbox = null;
    }

    public void setSelected( ImapMailbox mailbox, boolean readOnly )
    {
        ImapSessionMailbox sessionMailbox = new ImapSessionMailbox(mailbox, readOnly);
        this.state = ImapSessionState.SELECTED;
        this.selectedMailbox = sessionMailbox;
    }

    public ImapSessionMailbox getSelected()
    {
        return this.selectedMailbox;
    }

    public boolean selectedIsReadOnly()
    {
        return selectedMailbox.isReadonly();
    }

    public ImapSessionState getState()
    {
        return this.state;
    }

}
