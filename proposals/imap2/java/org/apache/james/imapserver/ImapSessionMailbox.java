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
import org.apache.james.imapserver.store.MailboxListener;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.imapserver.store.MessageFlags;
import org.apache.james.imapserver.store.SimpleImapMessage;
import org.apache.james.core.MailImpl;

import javax.mail.search.SearchTerm;
import javax.mail.internet.MimeMessage;
import java.util.List;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Date;

public class ImapSessionMailbox implements ImapMailbox, MailboxListener {
    private ImapMailbox _mailbox;
    private boolean _readonly;
    // TODO encapsulate
    public boolean _sizeChanged;
    private List _expungedMsns = Collections.synchronizedList(new LinkedList());

    public ImapSessionMailbox(ImapMailbox mailbox, boolean readonly) {
        _mailbox = mailbox;
        _readonly = readonly;
        // TODO make this a weak reference (or make sure deselect() is *always* called).
        _mailbox.addExpungeListener(this);
    }

    public void deselect() {
        _mailbox.removeExpungeListener(this);
        _mailbox = null;
    }

    public int getMsn( long uid ) throws MailboxException
    {
        long[] uids = _mailbox.getMessageUids();
        for (int i = 0; i < uids.length; i++) {
            long messageUid = uids[i];
            if (uid == messageUid) {
                return i+1;
            }
        }
        throw new MailboxException( "No such message." );
    }

    public boolean isReadonly() {
        return _readonly;
    }

    public int[] getExpunged() {
        synchronized (_expungedMsns) {
            int[] expungedMsns = new int[_expungedMsns.size()];
            for (int i = 0; i < expungedMsns.length; i++) {
                Integer msn = (Integer) _expungedMsns.get(i);
                expungedMsns[i] = msn.intValue();
            }
            _expungedMsns.clear();
            return expungedMsns;
        }
    }

    public void expunged(long uid) throws MailboxException {
        synchronized (_expungedMsns) {
            int msn = getMsn(uid);
            _expungedMsns.add(new Integer(msn));
        }
    }

    public void added(long uid) {
        _sizeChanged = true;
    }

    public String getName() {
        return _mailbox.getName();
    }

    public String getFullName() {
        return _mailbox.getFullName();
    }

    public MessageFlags getAllowedFlags() {
        return _mailbox.getAllowedFlags();
    }

    public MessageFlags getPermanentFlags() {
        return _mailbox.getPermanentFlags();
    }

    public int getMessageCount() {
        return _mailbox.getMessageCount();
    }

    public int getRecentCount() {
        return _mailbox.getRecentCount();
    }

    public long getUidValidity() {
        return _mailbox.getUidValidity();
    }

    public long getFirstUnseen() {
        return _mailbox.getFirstUnseen();
    }

    public boolean isSelectable() {
        return _mailbox.isSelectable();
    }

    public long getUidNext() {
        return _mailbox.getUidNext();
    }

    public int getUnseenCount() {
        return _mailbox.getUnseenCount();
    }

    public SimpleImapMessage createMessage( MimeMessage message, MessageFlags flags, Date internalDate ) {
        return _mailbox.createMessage(message, flags, internalDate);
    }

    public void updateMessage( SimpleImapMessage message ) throws MailboxException {
        _mailbox.updateMessage(message);
    }

    public void store( MailImpl mail) throws Exception {
        _mailbox.store(mail);
    }

    public SimpleImapMessage getMessage( long uid ) {
        return _mailbox.getMessage(uid);
    }

    public long[] getMessageUids() {
        return _mailbox.getMessageUids();
    }

    public void deleteMessage( long uid ) {
        _mailbox.deleteMessage(uid);
    }

    public void expunge() throws MailboxException {
        _mailbox.expunge();
    }

    public long[] search( SearchTerm searchTerm ) {
        return _mailbox.search( searchTerm);
    }

    public void copyMessage(long uid, ImapMailbox toMailbox) throws MailboxException {
        _mailbox.copyMessage(uid, toMailbox);
    }

    public void addExpungeListener(MailboxListener listener) {
        _mailbox.addExpungeListener(listener);
    }

    public void removeExpungeListener(MailboxListener listener) {
        _mailbox.removeExpungeListener(listener);
    }

}
