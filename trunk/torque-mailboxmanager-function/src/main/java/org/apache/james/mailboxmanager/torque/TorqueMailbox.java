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

package org.apache.james.mailboxmanager.torque;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.logging.Log;
import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.MailboxListener;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MailboxSession;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.SearchParameters;
import org.apache.james.mailboxmanager.UnsupportedCriteriaException;
import org.apache.james.mailboxmanager.impl.GeneralMessageSetImpl;
import org.apache.james.mailboxmanager.mailbox.AbstractImapMailbox;
import org.apache.james.mailboxmanager.mailbox.ImapMailbox;
import org.apache.james.mailboxmanager.torque.om.MailboxRow;
import org.apache.james.mailboxmanager.torque.om.MailboxRowPeer;
import org.apache.james.mailboxmanager.torque.om.MessageBody;
import org.apache.james.mailboxmanager.torque.om.MessageFlags;
import org.apache.james.mailboxmanager.torque.om.MessageFlagsPeer;
import org.apache.james.mailboxmanager.torque.om.MessageHeader;
import org.apache.james.mailboxmanager.torque.om.MessageRow;
import org.apache.james.mailboxmanager.torque.om.MessageRowPeer;
import org.apache.james.mailboxmanager.tracking.UidChangeTracker;
import org.apache.james.mailboxmanager.tracking.UidRange;
import org.apache.james.mailboxmanager.util.UidToKeyConverter;
import org.apache.james.mailboxmanager.util.UidToKeyConverterImpl;
import org.apache.mailet.RFC2822Headers;
import org.apache.torque.NoRowsException;
import org.apache.torque.TooManyRowsException;
import org.apache.torque.TorqueException;
import org.apache.torque.util.Criteria;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;

import com.sun.mail.util.CRLFOutputStream;
import com.workingdogs.village.DataSetException;

public class TorqueMailbox extends AbstractImapMailbox implements ImapMailbox {

    private boolean open = true;

    private MailboxRow mailboxRow;

    private UidChangeTracker tracker;

    private UidToKeyConverter uidToKeyConverter;
    
    private final ReadWriteLock lock;
    
    TorqueMailbox(final MailboxRow mailboxRow, final ReadWriteLock lock, final Log log) {
        setLog(log);
        this.mailboxRow = mailboxRow;
        this.tracker = new UidChangeTracker(mailboxRow.getLastUid());
        this.lock = lock;
        getUidToKeyConverter().setUidValidity(mailboxRow.getUidValidity());
    }

    public synchronized String getName() throws MailboxManagerException {
        checkAccess();
        return mailboxRow.getName();
    }

    public int getMessageCount(MailboxSession mailboxSession) throws MailboxManagerException {
        try {
            lock.readLock().acquire();
            try {
                checkAccess();
                try {
                    return getMailboxRow().countMessages();
                } catch (Exception e) {
                    throw new MailboxManagerException(e);
                }
            } finally {
                lock.readLock().release();
            }
        } catch (InterruptedException e) {
            throw new MailboxManagerException(e);
        }
    }

    public MessageResult appendMessage(MimeMessage message, Date internalDate,
            int result, MailboxSession mailboxSession) throws MailboxManagerException {

        try {
            checkAccess();

            final MailboxRow myMailboxRow = reserveNextUid();

            if (myMailboxRow != null) {
                try {
                    // To be thread safe, we first get our own copy and the
                    // exclusive
                    // Uid
                    // TODO create own message_id and assign uid later
                    // at the moment it could lead to the situation that uid 5 is
                    // inserted long before 4, when
                    // mail 4 is big and comes over a slow connection.
                    long uid = myMailboxRow.getLastUid();
                    this.mailboxRow = myMailboxRow;

                    MessageRow messageRow = new MessageRow();
                    messageRow.setMailboxId(getMailboxRow().getMailboxId());
                    messageRow.setUid(uid);
                    messageRow.setInternalDate(internalDate);

                    final int size = size(message);
                    messageRow.setSize(size);
                    populateFlags(message, messageRow);
                    
                    addHeaders(message, messageRow);

                    MessageBody mb = populateBody(message);
                    messageRow.addMessageBody(mb);

                    save(messageRow);
                    MessageResult messageResult = fillMessageResult(messageRow, result);
                    checkForScanGap(uid);
                    getUidChangeTracker().found(messageResult);
                    return messageResult;
                } catch (Exception e) {
                    throw new MailboxManagerException(e);
                }
            } else {
                // mailboxRow==null
                throw new MailboxManagerException("Mailbox has been deleted");
            }
        } catch (InterruptedException e) {
            throw new MailboxManagerException(e);
        }
    }

    private void populateFlags(MimeMessage message, MessageRow messageRow) throws MessagingException, TorqueException {
        MessageFlags messageFlags = new MessageFlags();
        messageFlags.setFlags(message.getFlags());
        messageRow.addMessageFlags(messageFlags);
    }

    private MessageBody populateBody(MimeMessage message) throws IOException, MessagingException {
        MessageBody mb = new MessageBody();

        InputStream is = message.getInputStream();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int read;
        while ((read = is.read(buf)) > 0) {
            baos.write(buf, 0, read);
        }

        final byte[] bytes = baos.toByteArray();
        mb.setBody(bytes);
        return mb;
    }

    private void addHeaders(MimeMessage message, MessageRow messageRow) throws MessagingException, TorqueException {
        int line_number = 0;

        for (Enumeration lines = message.getAllHeaderLines(); lines
        .hasMoreElements();) {
            String line = (String) lines.nextElement();
            int colon = line.indexOf(": ");
            if (colon > 0) {
                line_number++;
                MessageHeader mh = new MessageHeader();
                mh.setLineNumber(line_number);
                mh.setField(line.substring(0, colon));
                // TODO avoid unlikely IOOB Exception
                mh.setValue(line.substring(colon + 2));
                messageRow.addMessageHeader(mh);
            }
        }
    }

    private int size(MimeMessage message) throws IOException, MessagingException {
        // TODO very ugly size mesurement
        ByteArrayOutputStream sizeBos = new ByteArrayOutputStream();
        message.writeTo(new CRLFOutputStream(sizeBos));
        final int size = sizeBos.size();
        return size;
    }

    private void save(MessageRow messageRow) throws Exception {
        try {
            lock.writeLock().acquire();
            messageRow.save();
        } finally {
            lock.writeLock().release();
        }
    }

    private MailboxRow reserveNextUid() throws InterruptedException, MailboxManagerException {
        final MailboxRow myMailboxRow;
        try {
            lock.writeLock().acquire();
            myMailboxRow = getMailboxRow().consumeNextUid();
        } catch (TorqueException e) {
            throw new MailboxManagerException(e);
        } catch (SQLException e) {
            throw new MailboxManagerException(e);
        } finally {
            lock.writeLock().release();
        }
        return myMailboxRow;
    }

    private void checkForScanGap(long uid) throws MailboxManagerException, TorqueException, MessagingException {
        synchronized (getUidChangeTracker()) {
            long lastScannedUid=getUidChangeTracker().getLastScannedUid();
            if (uid>(lastScannedUid+1)) {
                GeneralMessageSet set=GeneralMessageSetImpl.uidRange(lastScannedUid+1, uid);
                Criteria criteria=criteriaForMessageSet(set);
                final List messageRows=mailboxRow.getMessageRows(criteria);
                getUidChangeTracker().found(uidRangeForMessageSet(set), 
                        MessageRowUtils.toMessageFlags(messageRows));
            }
        }
        
    }

    private Criteria criteriaForMessageSet(GeneralMessageSet set)
            throws MailboxManagerException {
        Criteria criteria = new Criteria();
        criteria.addAscendingOrderByColumn(MessageRowPeer.UID);
        if (set.getType() == GeneralMessageSet.TYPE_ALL) {
            // empty Criteria = everything
        } else if (set.getType() == GeneralMessageSet.TYPE_UID) {
            
            if (set.getUidFrom() == set.getUidTo()) {
                criteria.add(MessageRowPeer.UID, set.getUidFrom());
            } else {
                Criteria.Criterion criterion1=criteria.getNewCriterion(MessageRowPeer.UID, new Long(set.getUidFrom()),
                        Criteria.GREATER_EQUAL);
                if (set.getUidTo() > 0) {
                    Criteria.Criterion criterion2=criteria.getNewCriterion(MessageRowPeer.UID, new Long(set.getUidTo()),
                            Criteria.LESS_EQUAL);
                    criterion1.and(criterion2);
                }
                criteria.add(criterion1);
            }
        } else {
            throw new MailboxManagerException("Unsupported MessageSet: "
                    + set.getType());
        }
        return criteria;
    }

    public Iterator getMessages(GeneralMessageSet set, int result, MailboxSession mailboxSession)
            throws MailboxManagerException {
        try {
            lock.readLock().acquire();
            try {
                checkAccess();
                set=toUidSet(set);
                if (!set.isValid() || set.getType()==GeneralMessageSet.TYPE_NOTHING) {
                    return IteratorUtils.EMPTY_ITERATOR;
                }
                UidRange range = uidRangeForMessageSet(set);
                try {
                    Criteria c = criteriaForMessageSet(set);
                    c.add(MessageFlagsPeer.MAILBOX_ID,getMailboxRow().getMailboxId());
                    return getMessages(result, range, c);
                } catch (TorqueException e) {
                    throw new MailboxManagerException(e);
                } catch (MessagingException e) {
                    throw new MailboxManagerException(e);
                }
            } finally {
                lock.readLock().release();
            }
        } catch (InterruptedException e) {
            throw new MailboxManagerException(e);
        }
    }

    private TorqueResultIterator getMessages(int result, UidRange range, Criteria c) throws TorqueException, MessagingException, MailboxManagerException {
        List rows = MessageRowPeer.doSelectJoinMessageFlags(c);
        Collections.sort(rows, MessageRowUtils.getUidComparator());
        final TorqueResultIterator results = new TorqueResultIterator(rows, result, getUidToKeyConverter());
        checkForScanGap(range.getFromUid());
        getUidChangeTracker().found(range, results.getMessageFlags());
        return results;
    }

    private static UidRange uidRangeForMessageSet(GeneralMessageSet set)
            throws MailboxManagerException {
        if (set.getType() == GeneralMessageSet.TYPE_UID) {
            return new UidRange(set.getUidFrom(), set.getUidTo());
        } else if (set.getType() == GeneralMessageSet.TYPE_ALL) {
            return new UidRange(1, -1);
        } else {
            throw new MailboxManagerException("unsupported MessageSet: "
                    + set.getType());
        }
    }


    public MessageResult fillMessageResult(MessageRow messageRow, int result)
            throws TorqueException, MessagingException, MailboxManagerException {
        return MessageRowUtils.loadMessageResult(messageRow, result, getUidToKeyConverter());
    }
    
    public synchronized Flags getPermanentFlags() {
        Flags permanentFlags = new Flags();
        permanentFlags.add(Flags.Flag.ANSWERED);
        permanentFlags.add(Flags.Flag.DELETED);
        permanentFlags.add(Flags.Flag.DRAFT);
        permanentFlags.add(Flags.Flag.FLAGGED);
        permanentFlags.add(Flags.Flag.SEEN);
        return permanentFlags;
    }

    public int getRecentCount(boolean reset, MailboxSession mailboxSession) throws MailboxManagerException {
        try {
            lock.readLock().acquire();
            try {
                checkAccess();
                Flags flags = new Flags();
                flags.add(Flags.Flag.RECENT);
                try {
                    int count = getMailboxRow().countMessages(flags, true);
                    if (reset) {
                        getMailboxRow().resetRecent();
                    }
                    return count;
                } catch (TorqueException e) {
                    throw new MailboxManagerException(e);
                } catch (DataSetException e) {
                    throw new MailboxManagerException(e);
                }
            } finally {
                lock.readLock().release();
            }
        } catch (InterruptedException e) {
            throw new MailboxManagerException(e);
        }
    }

    public MessageResult getFirstUnseen(int result, MailboxSession mailboxSession)
            throws MailboxManagerException {
        try {
            lock.readLock().acquire();
            try {
                checkAccess();
                Criteria c = new Criteria();
                c.addAscendingOrderByColumn(MessageRowPeer.UID);
                c.setLimit(1);
                c.setSingleRecord(true);

                c.addJoin(MessageFlagsPeer.MAILBOX_ID, MessageRowPeer.MAILBOX_ID);
                c.addJoin(MessageRowPeer.UID, MessageFlagsPeer.UID);

                MessageFlagsPeer.addFlagsToCriteria(new Flags(Flags.Flag.SEEN), false,
                        c);

                try {
                    List messageRows = getMailboxRow().getMessageRows(c);
                    if (messageRows.size() > 0) {
                        MessageResult messageResult=fillMessageResult((MessageRow) messageRows.get(0), result);
                        if (messageResult!=null) {
                            checkForScanGap(messageResult.getUid());
                            getUidChangeTracker().found(messageResult);
                        }

                        return messageResult;
                    } else {
                        return null;
                    }
                } catch (TorqueException e) {
                    throw new MailboxManagerException(e);
                } catch (MessagingException e) {
                    throw new MailboxManagerException(e);
                }
            } finally {
                lock.readLock().release();
            }
        } catch (InterruptedException e) {
            throw new MailboxManagerException(e);
        }
    }

    public int getUnseenCount(MailboxSession mailboxSession) throws MailboxManagerException {
        try {
            lock.readLock().acquire();
            try {
                checkAccess();
                try {
                    final int count = getMailboxRow().countMessages(new Flags(Flags.Flag.SEEN), false);
                    return count;
                } catch (TorqueException e) {
                    throw new MailboxManagerException(e);
                } catch (DataSetException e) {
                    throw new MailboxManagerException(e);
                }
            } finally {
                lock.readLock().release();
            }
        } catch (InterruptedException e) {
            throw new MailboxManagerException(e);
        }
    }

    public Iterator expunge(GeneralMessageSet set, int result, MailboxSession mailboxSession)
            throws MailboxManagerException {
        try {
            lock.writeLock().acquire();
            try {
                return doExpunge(set, result);
            } finally {
                lock.writeLock().release();
            }

        } catch (InterruptedException e) {
            throw new MailboxManagerException(e);
        }
    }

    private Iterator doExpunge(GeneralMessageSet set, int result) throws MailboxManagerException {
        checkAccess();
        set=toUidSet(set);  
        if (!set.isValid() || set.getType()==GeneralMessageSet.TYPE_NOTHING) {
            return IteratorUtils.EMPTY_ITERATOR;
        }
        try {
            // TODO put this into a serializable transaction
            final Criteria c = criteriaForMessageSet(set);
            c.addJoin(MessageRowPeer.MAILBOX_ID, MessageFlagsPeer.MAILBOX_ID);
            c.addJoin(MessageRowPeer.UID, MessageFlagsPeer.UID);
            c.add(MessageRowPeer.MAILBOX_ID, getMailboxRow().getMailboxId());
            c.add(MessageFlagsPeer.DELETED, true);

            final List messageRows = getMailboxRow().getMessageRows(c);
            final long[] uids = uids(messageRows);
            final TorqueResultIterator resultIterator = new TorqueResultIterator(messageRows, result
                    | MessageResult.FLAGS, getUidToKeyConverter());
            // ensure all results are loaded before deletion
            Collection messageResults = IteratorUtils.toList(resultIterator);
            
            for (Iterator iter = messageRows.iterator(); iter.hasNext();) {
                MessageRow messageRow = (MessageRow) iter.next();
                Criteria todelc=new Criteria();
                todelc.add(MessageRowPeer.MAILBOX_ID,messageRow.getMailboxId());
                todelc.add(MessageRowPeer.UID,messageRow.getUid());
                MessageRowPeer.doDelete(todelc);
            }
            getUidChangeTracker().expunged(uids);
            return messageResults.iterator();
        } catch (Exception e) {
            throw new MailboxManagerException(e);
        }
    }

    private long[] uids(List messageRows) {
        final int size = messageRows.size();
        long[] results = new long[size];
        for (int i=0;i<size;i++) {
            final MessageRow messageRow = (MessageRow) messageRows.get(i);
            results[i] = (messageRow).getUid();
        }
        return results;
    }

    public Iterator setFlags(Flags flags, boolean value, boolean replace,
            GeneralMessageSet set, int result, MailboxSession mailboxSession)
            throws MailboxManagerException {
        try {
            lock.writeLock().acquire();
            try {
                return doSetFlags(flags, value, replace, set, result, mailboxSession);
            } finally {
                lock.writeLock().release();
            }

        } catch (InterruptedException e) {
            throw new MailboxManagerException(e);
        }
    }

    private Iterator doSetFlags(Flags flags, boolean value, boolean replace, 
            GeneralMessageSet set, int results, MailboxSession mailboxSession) throws MailboxManagerException {
        checkAccess();
        set=toUidSet(set);  
        if (!set.isValid() || set.getType()==GeneralMessageSet.TYPE_NOTHING) {
            return Collections.EMPTY_LIST.iterator();
        }        
        try {
            // TODO put this into a serializeable transaction
            final List messageRows = getMailboxRow()
                    .getMessageRows(criteriaForMessageSet(set));
            UidRange uidRange=uidRangeForMessageSet(set);
            checkForScanGap(uidRange.getFromUid());
            getUidChangeTracker().found(uidRange, MessageRowUtils.toMessageFlags(messageRows));
            for (Iterator iter = messageRows.iterator(); iter.hasNext();) {
                final MessageRow messageRow = (MessageRow) iter.next();
                final MessageFlags messageFlags = messageRow.getMessageFlags();
                if (messageFlags != null) {
                    if (replace) {
                        messageFlags.setFlags(flags);
                    } else {
                        Flags current = messageFlags.getFlagsObject();
                        if (value) {
                            current.add(flags);
                        } else {
                            current.remove(flags);
                        }
                        messageFlags.setFlags(current);
                    }
                    messageFlags.save();
                }
            }
            final TorqueResultIterator resultIterator = new TorqueResultIterator(messageRows,
                    results | MessageResult.FLAGS, getUidToKeyConverter());
            final org.apache.james.mailboxmanager.impl.MessageFlags[] messageFlags = resultIterator.getMessageFlags();
            tracker.flagsUpdated(messageFlags, mailboxSession.getSessionId());
            tracker.found(uidRange, messageFlags);
            return resultIterator;
        } catch (TorqueException e) {
            throw new MailboxManagerException(e);
        } catch (MessagingException e) {
            throw new MailboxManagerException(e);
        }
    }

    public void addListener(MailboxListener listener) throws MailboxManagerException {
        checkAccess();
        tracker.addMailboxListener(listener);
    }

    public void removeListener(MailboxListener mailboxListener) {
        if (!open) {
          throw new RuntimeException("mailbox not open");
        }
        tracker.removeMailboxListener(mailboxListener);
    }

    public long getUidValidity(MailboxSession mailboxSession) throws MailboxManagerException {
        try {
            lock.readLock().acquire();
            try {
                checkAccess();
                final long result = getMailboxRow().getUidValidity();
                return result;
            } finally {
                lock.readLock().release();
            }

        } catch (InterruptedException e) {
            throw new MailboxManagerException(e);
        }

    }

    public long getUidNext(MailboxSession mailboxSession) throws MailboxManagerException {
        try {
            lock.readLock().acquire();
            try {
                checkAccess();
                try {
                    MailboxRow myMailboxRow = MailboxRowPeer.retrieveByPK(mailboxRow.getPrimaryKey());
                    if (myMailboxRow != null) {
                        mailboxRow=myMailboxRow;
                        getUidChangeTracker().foundLastUid(mailboxRow.getLastUid());
                        return getUidChangeTracker().getLastUid() + 1;
                    } else {
                        throw new MailboxManagerException("Mailbox has been deleted");
                    }
                } catch (NoRowsException e) {
                    throw new MailboxManagerException(e);
                } catch (TooManyRowsException e) {
                    throw new MailboxManagerException(e);
                } catch (TorqueException e) {
                    throw new MailboxManagerException(e);
                }
            } finally {
                lock.readLock().release();
            }

        } catch (InterruptedException e) {
            throw new MailboxManagerException(e);
        }
    }

    private void checkAccess() throws MailboxManagerException {
        if (!open) {
            throw new RuntimeException("mailbox is closed");
        }
    }
    
    protected UidChangeTracker getUidChangeTracker() {
        return tracker;
    }
    
    protected MailboxRow getMailboxRow() {
        return mailboxRow;
    }

    protected void setMailboxRow(MailboxRow mailboxRow) {
        this.mailboxRow = mailboxRow;
    }

    public Iterator search(GeneralMessageSet set, SearchParameters parameters,
            int result, MailboxSession mailboxSession) throws MailboxManagerException {
        try {
            lock.readLock().acquire();
            try {
                checkAccess();
                set=toUidSet(set);
                if (!set.isValid() || set.getType()==GeneralMessageSet.TYPE_NOTHING) {
                    return IteratorUtils.EMPTY_ITERATOR;
                }
                
                TorqueCriteriaBuilder builder = new TorqueCriteriaBuilder();
                
                final List searchCriteria = parameters.getCriterias();
                for (Iterator it=searchCriteria.iterator();it.hasNext();) {
                    SearchParameters.SearchCriteria criterion = (SearchParameters.SearchCriteria) it.next();
                    final String name = criterion.getName();
                    if (SearchParameters.ALL.equals(name)) {
                        throw new UnsupportedCriteriaException("Search criterion '" + name + "' is not supported");
                    } else if (SearchParameters.ANSWERED.equals(name)) {
                        builder.andFlag(javax.mail.Flags.Flag.ANSWERED, true);
                    } else if (SearchParameters.BCC.equals(name)) {
                        SearchParameters.StringSearchCriteria stringSearchCriteria = (SearchParameters.StringSearchCriteria) criterion;
                        final String value = stringSearchCriteria.getValue();
                        builder.andHeaderContains(RFC2822Headers.BCC, value);
                    } else if (SearchParameters.BEFORE.equals(name)) {
                        throw new UnsupportedCriteriaException("Search criterion '" + name + "' is not supported");
                    } else if (SearchParameters.BODY.equals(name)) {
                        throw new UnsupportedCriteriaException("Search criterion '" + name + "' is not supported");
                    } else if (SearchParameters.CC.equals(name)) {
                        SearchParameters.StringSearchCriteria stringSearchCriteria = (SearchParameters.StringSearchCriteria) criterion;
                        final String value = stringSearchCriteria.getValue();
                        builder.andHeaderContains(RFC2822Headers.CC, value);
                    } else if (SearchParameters.DELETED.equals(name)) {
                        builder.andFlag(javax.mail.Flags.Flag.DELETED, true);
                    } else if (SearchParameters.DRAFT.equals(name)) {
                        builder.andFlag(javax.mail.Flags.Flag.DRAFT, true);
                    } else if (SearchParameters.FLAGGED.equals(name)) {
                        builder.andFlag(javax.mail.Flags.Flag.FLAGGED, true);
                    } else if (SearchParameters.FROM.equals(name)) {
                        SearchParameters.StringSearchCriteria stringSearchCriteria = (SearchParameters.StringSearchCriteria) criterion;
                        final String value = stringSearchCriteria.getValue();
                        builder.andHeaderContains(RFC2822Headers.FROM, value);
                    } else if (SearchParameters.HEADER.equals(name)) {
                        SearchParameters.HeaderSearchCriteria headerSearchCriteria = (SearchParameters.HeaderSearchCriteria) criterion;
                        final String value = headerSearchCriteria.getValue();
                        final String fieldName = headerSearchCriteria.getFieldName();
                        builder.andHeaderContains(fieldName, value);
                    } else if (SearchParameters.KEYWORD.equals(name)) {
                        throw new UnsupportedCriteriaException("Search criterion '" + name + "' is not supported");
                    } else if (SearchParameters.LARGER.equals(name)) {
                        throw new UnsupportedCriteriaException("Search criterion '" + name + "' is not supported");
                    } else if (SearchParameters.NEW.equals(name)) {
                        builder.andFlag(javax.mail.Flags.Flag.RECENT, true);
                        builder.andFlag(javax.mail.Flags.Flag.RECENT, false);
                    } else if (SearchParameters.NOT.equals(name)) {
                        throw new UnsupportedCriteriaException("Search criterion '" + name + "' is not supported");
                    } else if (SearchParameters.OLD.equals(name)) {
                        throw new UnsupportedCriteriaException("Search criterion '" + name + "' is not supported");
                    } else if (SearchParameters.ON.equals(name)) {
                        throw new UnsupportedCriteriaException("Search criterion '" + name + "' is not supported");
                    } else if (SearchParameters.OR.equals(name)) {
                        throw new UnsupportedCriteriaException("Search criterion '" + name + "' is not supported");
                    } else if (SearchParameters.RECENT.equals(name)) {
                        builder.andFlag(javax.mail.Flags.Flag.RECENT, true);
                    } else if (SearchParameters.SEEN.equals(name)) {
                        builder.andFlag(javax.mail.Flags.Flag.SEEN, true);
                    } else if (SearchParameters.SENTBEFORE.equals(name)) {
                        throw new UnsupportedCriteriaException("Search criterion '" + name + "' is not supported");
                    } else if (SearchParameters.SENTON.equals(name)) {
                        throw new UnsupportedCriteriaException("Search criterion '" + name + "' is not supported");
                    } else if (SearchParameters.SENTSINCE.equals(name)) {
                        throw new UnsupportedCriteriaException("Search criterion '" + name + "' is not supported");
                    } else if (SearchParameters.SINCE.equals(name)) {
                        throw new UnsupportedCriteriaException("Search criterion '" + name + "' is not supported");
                    } else if (SearchParameters.SMALLER.equals(name)) {
                        throw new UnsupportedCriteriaException("Search criterion '" + name + "' is not supported");
                    } else if (SearchParameters.SUBJECT.equals(name)) {
                        throw new UnsupportedCriteriaException("Search criterion '" + name + "' is not supported");
                    } else if (SearchParameters.TEXT.equals(name)) {
                        throw new UnsupportedCriteriaException("Search criterion '" + name + "' is not supported");
                    } else if (SearchParameters.TO.equals(name)) {
                        SearchParameters.StringSearchCriteria stringSearchCriteria = (SearchParameters.StringSearchCriteria) criterion;
                        final String value = stringSearchCriteria.getValue();
                        builder.andHeaderContains(RFC2822Headers.TO, value);
                    } else if (SearchParameters.UID.equals(name)) {
                        throw new UnsupportedCriteriaException("Search criterion '" + name + "' is not supported");
                    } else if (SearchParameters.UNANSWERED.equals(name)) {
                        builder.andFlag(javax.mail.Flags.Flag.ANSWERED, false);
                    } else if (SearchParameters.UNDELETED.equals(name)) {
                        builder.andFlag(javax.mail.Flags.Flag.DELETED, false);
                    } else if (SearchParameters.UNDRAFT.equals(name)) {
                        builder.andFlag(javax.mail.Flags.Flag.DRAFT, false);
                    } else if (SearchParameters.UNFLAGGED.equals(name)) {
                        builder.andFlag(javax.mail.Flags.Flag.FLAGGED, false);
                    } else if (SearchParameters.UNKEYWORD.equals(name)) {
                        throw new UnsupportedCriteriaException("Search criterion '" + name + "' is not supported");
                    } else if (SearchParameters.UNSEEN.equals(name)) {
                        builder.andFlag(javax.mail.Flags.Flag.SEEN, false);
                    } else {
                        throw new UnsupportedCriteriaException("Search criterion '" + name + "' is not supported");
                    }
                }
                
                final Iterator results = getMessages(result, new UidRange(1, -1), builder.getCriteria());
                return results;
            } catch (TorqueException e) {
                throw new MailboxManagerException(e);
            } catch (MessagingException e) {
                throw new MailboxManagerException(e);
            } finally {
                lock.readLock().release();
            }
        } catch (InterruptedException e) {
            throw new MailboxManagerException(e);
        }
    }
    
    protected UidToKeyConverter getUidToKeyConverter() {
        if (uidToKeyConverter == null) {
            uidToKeyConverter = new UidToKeyConverterImpl();
        }
        return uidToKeyConverter;
    }

    public void remove(GeneralMessageSet set, MailboxSession mailboxSession) throws MailboxManagerException {
        try {
            lock.writeLock().acquire();
            try {
                final Flags flags = new Flags(Flags.Flag.DELETED);
                doSetFlags(flags, true, false, set, MessageResult.MINIMAL, mailboxSession);
                doExpunge(set, MessageResult.MINIMAL);
            } finally {
                lock.writeLock().release();
            }

        } catch (InterruptedException e) {
            throw new MailboxManagerException(e);
        }
    }

    private GeneralMessageSet toUidSet(GeneralMessageSet set) {
        if (set.getType()==GeneralMessageSet.TYPE_KEY) {
            Long uid=getUidToKeyConverter().toUid(set.getKey());
            if (uid!=null) {
                set=GeneralMessageSetImpl.oneUid(uid.longValue());
            } else {
                set=GeneralMessageSetImpl.nothing();
            }
        }
        return set;
    }

    public boolean isWriteable() {
        return true;
    }    
}
