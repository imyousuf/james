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

import org.apache.james.core.MailImpl;
import org.apache.james.imapserver.commands.IdRange;
import org.apache.james.imapserver.store.ImapMailbox;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.imapserver.store.MailboxListener;
import org.apache.james.imapserver.store.SimpleImapMessage;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ImapSessionMailbox implements ImapMailbox, MailboxListener {
    private ImapMailbox _mailbox;
    private ImapSession _session;
    private boolean _readonly;
    private boolean _sizeChanged;
    private List _expungedMsns = Collections.synchronizedList(new LinkedList());
    private Map _modifiedFlags = Collections.synchronizedMap(new TreeMap());

    public ImapSessionMailbox(ImapMailbox mailbox, ImapSession session, boolean readonly) {
        _mailbox = mailbox;
        _session = session;
        _readonly = readonly;
        // TODO make this a weak reference (or make sure deselect() is *always* called).
        _mailbox.addListener(this);
    }

    public void deselect() {
        _mailbox.removeListener(this);
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

    public void signalDeletion() {
        _mailbox.signalDeletion();
    }

    public boolean isReadonly() {
        return _readonly;
    }

    public int[] getExpunged() throws MailboxException {
        synchronized (_expungedMsns) {
            int[] expungedMsns = new int[_expungedMsns.size()];
            for (int i = 0; i < expungedMsns.length; i++) {
                int msn = ((Integer) _expungedMsns.get(i)).intValue();
                expungedMsns[i] = msn;
            }
            _expungedMsns.clear();

            // TODO - renumber any cached ids (for now we assume the _modifiedFlags has been cleared)\
            if (! (_modifiedFlags.isEmpty() && ! _sizeChanged ) ) {
                throw new IllegalStateException("Need to do this properly...");
            }
            return expungedMsns;
        }
    }

    public List getFlagUpdates() throws MailboxException {
        if (_modifiedFlags.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        List retVal = new ArrayList();
        retVal.addAll(_modifiedFlags.values());
        _modifiedFlags.clear();
        return retVal;
    }

    public void expunged(int msn) {
        synchronized (_expungedMsns) {
            _expungedMsns.add(new Integer(msn));
        }
    }

    public void added(int msn) {
        _sizeChanged = true;
    }

    public void flagsUpdated(int msn, Flags flags, Long uid) {
        // This will overwrite any earlier changes
        _modifiedFlags.put(new Integer(msn), new FlagUpdate(msn, uid, flags));
    }

    public void mailboxDeleted() {
        _session.closeConnection("Mailbox " + _mailbox.getName() + " has been deleted");
    }

    public String getName() {
        return _mailbox.getName();
    }

    public String getFullName() {
        return _mailbox.getFullName();
    }

    public Flags getPermanentFlags() {
        return _mailbox.getPermanentFlags();
    }

    public int getMessageCount() {
        return _mailbox.getMessageCount();
    }

    public int getRecentCount(boolean reset) {
        return _mailbox.getRecentCount(reset);
    }

    public long getUidValidity() {
        return _mailbox.getUidValidity();
    }

    public int getFirstUnseen() {
        return correctForExpungedMessages(_mailbox.getFirstUnseen());
    }

    /**
     * Adjust an actual mailbox msn for the expunged messages in this mailbox that have not
     * yet been notified.
     * TODO - need a test for this
     */ 
    private int correctForExpungedMessages(int absoluteMsn) {
        int correctedMsn = absoluteMsn;
        // Loop throught the expunged list backwards, adjusting the msn as we go.
        for (int i = (_expungedMsns.size() - 1); i >= 0; i--) {
            Integer expunged = (Integer) _expungedMsns.get(i);
            if (expunged.intValue() <= absoluteMsn) {
                correctedMsn++;
            }
        }
        return correctedMsn;
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

    public long appendMessage( MimeMessage message, Flags flags, Date internalDate ) {
        return _mailbox.appendMessage(message, flags, internalDate);
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

    public void expunge() throws MailboxException {
        _mailbox.expunge();
    }

    public long[] search( SearchTerm searchTerm ) {
        return _mailbox.search( searchTerm);
    }

    public void copyMessage(long uid, ImapMailbox toMailbox) throws MailboxException {
        _mailbox.copyMessage(uid, toMailbox);
    }

    public void addListener(MailboxListener listener) {
        _mailbox.addListener(listener);
    }

    public void removeListener(MailboxListener listener) {
        _mailbox.removeListener(listener);
    }

    public IdRange[] msnsToUids(IdRange[] idSet) {
        return new IdRange[0];  //To change body of created methods use Options | File Templates.
    }

    public void setFlags(Flags flags, boolean value, long uid, MailboxListener silentListener, boolean addUid) throws MailboxException {
        _mailbox.setFlags(flags, value, uid, silentListener, addUid);
    }

    public void replaceFlags(Flags flags, long uid, MailboxListener silentListener, boolean addUid) throws MailboxException {
        _mailbox.replaceFlags(flags, uid, silentListener, addUid);
    }

    public void deleteAllMessages() {
        _mailbox.deleteAllMessages();
    }

    public boolean isSizeChanged() {
        return _sizeChanged;
    }

    public void setSizeChanged(boolean sizeChanged) {
        _sizeChanged = sizeChanged;
    }
    
    static final class FlagUpdate {
        private int msn;
        private Long uid;
        private Flags flags;

        public FlagUpdate(int msn, Long uid, Flags flags) {
            this.msn = msn;
            this.uid = uid;
            this.flags = flags;
        }

        public int getMsn() {
            return msn;
        }

        public Long getUid() {
            return uid;
        }

        public Flags getFlags() {
            return flags;
        }
    }

}
