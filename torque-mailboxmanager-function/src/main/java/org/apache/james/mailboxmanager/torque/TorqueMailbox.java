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
import java.util.ArrayList;
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
import org.apache.james.mailboxmanager.SearchQuery;
import org.apache.james.mailboxmanager.MessageResult.FetchGroup;
import org.apache.james.mailboxmanager.SearchQuery.Criterion;
import org.apache.james.mailboxmanager.SearchQuery.NumericRange;
import org.apache.james.mailboxmanager.impl.FetchGroupImpl;
import org.apache.james.mailboxmanager.mailbox.Mailbox;
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
import org.apache.james.mailboxmanager.util.AbstractLogFactoryAware;
import org.apache.torque.NoRowsException;
import org.apache.torque.TooManyRowsException;
import org.apache.torque.TorqueException;
import org.apache.torque.util.Criteria;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;

import com.sun.mail.util.CRLFOutputStream;
import com.workingdogs.village.DataSetException;

public class TorqueMailbox extends AbstractLogFactoryAware implements Mailbox {

    private boolean open = true;

    private MailboxRow mailboxRow;

    private UidChangeTracker tracker;

    private final ReadWriteLock lock;
    
    private final MessageSearches searches;
    
    TorqueMailbox(final MailboxRow mailboxRow, final ReadWriteLock lock, final Log log) {
        this.searches = new MessageSearches();
        setLog(log);
        this.mailboxRow = mailboxRow;
        this.tracker = new UidChangeTracker(mailboxRow.getLastUid());
        this.lock = lock;
    }

    public synchronized String getName() {
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
            FetchGroup fetchGroup, MailboxSession mailboxSession) throws MailboxManagerException {

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
                    MessageResult messageResult = fillMessageResult(messageRow, fetchGroup);
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

        final byte[] bytes = MessageUtils.toByteArray(is);
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

    public Iterator getMessages(final GeneralMessageSet set, FetchGroup fetchGroup, MailboxSession mailboxSession)
            throws MailboxManagerException {
        try {
            lock.readLock().acquire();
            try {
                checkAccess();
                UidRange range = uidRangeForMessageSet(set);
                try {
                    Criteria c = criteriaForMessageSet(set);
                    c.add(MessageFlagsPeer.MAILBOX_ID,getMailboxRow().getMailboxId());
                    return getMessages(fetchGroup, range, c);
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

    private TorqueResultIterator getMessages(FetchGroup result, UidRange range, Criteria c) throws TorqueException, MessagingException, MailboxManagerException {
        List rows = MessageRowPeer.doSelectJoinMessageFlags(c);
        final TorqueResultIterator results = getResults(result, rows);
        getUidChangeTracker().found(range, results.getMessageFlags());
        return results;
    }

    private TorqueResultIterator getResults(FetchGroup result, List rows) throws TorqueException {
        Collections.sort(rows, MessageRowUtils.getUidComparator());
        final TorqueResultIterator results = new TorqueResultIterator(rows, result);
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


    public MessageResult fillMessageResult(MessageRow messageRow, FetchGroup result)
            throws TorqueException, MessagingException, MailboxManagerException {
        return MessageRowUtils.loadMessageResult(messageRow, result);
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

    public long[] recent(boolean reset, MailboxSession mailboxSession) throws MailboxManagerException {
        try {
            lock.readLock().acquire();
            try {
                checkAccess();
                final Criteria criterion = queryRecentFlagSet();
                final List messageRows = getMailboxRow().getMessageRows(criterion);
                final long[] results = new long[messageRows.size()];
                int count = 0;
                for (Iterator it = messageRows.iterator(); it.hasNext();) {
                    final MessageRow row = (MessageRow) it.next();
                    results[count++] = row.getUid();
                }

                if (reset) {
                    getMailboxRow().resetRecent();
                }
                return results;
            } catch (TorqueException e) {
                throw new MailboxManagerException(e);
            } finally {
                lock.readLock().release();
            }
        } catch (InterruptedException e) {
            throw new MailboxManagerException(e);
        }
      
    }

    private Criteria queryRecentFlagSet() {
        final Criteria criterion = new Criteria();
        criterion.addJoin(MessageFlagsPeer.MAILBOX_ID,
                MessageRowPeer.MAILBOX_ID);
        criterion.addJoin(MessageRowPeer.UID, MessageFlagsPeer.UID);

        MessageFlagsPeer.addFlagsToCriteria(new Flags(Flags.Flag.RECENT), true, criterion);
        return criterion;
    }

    public MessageResult getFirstUnseen(FetchGroup fetchGroup, MailboxSession mailboxSession)
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
                        MessageResult messageResult=fillMessageResult((MessageRow) messageRows.get(0), fetchGroup);
                        if (messageResult!=null) {
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

    public Iterator expunge(GeneralMessageSet set, FetchGroup fetchGroup, MailboxSession mailboxSession)
            throws MailboxManagerException {
        try {
            lock.writeLock().acquire();
            try {
                return doExpunge(set, fetchGroup);
            } finally {
                lock.writeLock().release();
            }

        } catch (InterruptedException e) {
            throw new MailboxManagerException(e);
        }
    }

    private Iterator doExpunge(final GeneralMessageSet set, FetchGroup fetchGroup) throws MailboxManagerException {
        checkAccess();
        try {
            // TODO put this into a serializable transaction
            final Criteria c = criteriaForMessageSet(set);
            c.addJoin(MessageRowPeer.MAILBOX_ID, MessageFlagsPeer.MAILBOX_ID);
            c.addJoin(MessageRowPeer.UID, MessageFlagsPeer.UID);
            c.add(MessageRowPeer.MAILBOX_ID, getMailboxRow().getMailboxId());
            c.add(MessageFlagsPeer.DELETED, true);

            final List messageRows = getMailboxRow().getMessageRows(c);
            final long[] uids = uids(messageRows);
            final OrFetchGroup orFetchGroup = new OrFetchGroup(fetchGroup, FetchGroup.FLAGS);
            final TorqueResultIterator resultIterator = new TorqueResultIterator(messageRows, orFetchGroup);
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
            GeneralMessageSet set, FetchGroup fetchGroup, MailboxSession mailboxSession)
            throws MailboxManagerException {
        try {
            lock.writeLock().acquire();
            try {
                return doSetFlags(flags, value, replace, set, fetchGroup, mailboxSession);
            } finally {
                lock.writeLock().release();
            }

        } catch (InterruptedException e) {
            throw new MailboxManagerException(e);
        }
    }

    private Iterator doSetFlags(Flags flags, boolean value, boolean replace, 
            final GeneralMessageSet set, FetchGroup fetchGroup, MailboxSession mailboxSession) throws MailboxManagerException {
        checkAccess();         
        try {
            // TODO put this into a serializeable transaction
            final List messageRows = getMailboxRow()
                    .getMessageRows(criteriaForMessageSet(set));
            UidRange uidRange=uidRangeForMessageSet(set);
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
            final OrFetchGroup orFetchGroup = new OrFetchGroup(fetchGroup, FetchGroup.FLAGS);
            final TorqueResultIterator resultIterator = new TorqueResultIterator(messageRows,
                    orFetchGroup);
            final org.apache.james.mailboxmanager.impl.MessageFlags[] messageFlags = resultIterator.getMessageFlags();
            tracker.flagsUpdated(messageFlags, mailboxSession.getSessionId());
            tracker.found(uidRange, messageFlags);
            return resultIterator;
        } catch (Exception e) {
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

    private void checkAccess() {
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

    public Iterator search(SearchQuery query, FetchGroup fetchGroup,
            MailboxSession mailboxSession) throws MailboxManagerException {
        try {
            lock.readLock().acquire();
            try {
                checkAccess();
                
                final Criteria criterion = preSelect(query);
                final List rows = MessageRowPeer.doSelectJoinMessageFlags(criterion);
                final List filteredMessages = new ArrayList();
                for (Iterator it = rows.iterator(); it
                        .hasNext();) {
                    final MessageRow row = (MessageRow) it.next();
                    try {
                        if (searches.isMatch(query, row)) {
                            filteredMessages.add(row);
                        }
                    } catch (TorqueException e) {
                        getLog().info("Cannot test message against search criteria. Will continue to test other messages.", e);
                    }
                }
                
                return getResults(fetchGroup, filteredMessages);
            } catch (TorqueException e) {
                throw new MailboxManagerException(e);
            } finally {
                lock.readLock().release();
            }
        } catch (InterruptedException e) {
            throw new MailboxManagerException(e);
        }
    }
    
    private Criteria preSelect(SearchQuery query) {
        final Criteria results = new Criteria();
        final List criteria = query.getCriterias();
        if (criteria.size() == 1) {
            final Criterion criterion = (Criterion) criteria.get(0);
            if (criterion instanceof SearchQuery.UidCriterion) {
                final SearchQuery.UidCriterion uidCriterion = (SearchQuery.UidCriterion) criterion;
                preSelectUid(results, uidCriterion);
            }
        }
        return results;
    }

    private void preSelectUid(final Criteria results, final SearchQuery.UidCriterion uidCriterion) {
        final NumericRange[] ranges = uidCriterion.getOperator().getRange();
        for (int i = 0; i < ranges.length; i++) {
            final long low = ranges[i].getLowValue();
            final long high = ranges[i].getHighValue();
            if (low == Long.MAX_VALUE) {
                results.add(MessageRowPeer.UID, high, Criteria.LESS_EQUAL);
            } else if (low == high) {
                results.add(MessageRowPeer.UID, low);
            } else {
                final Criteria.Criterion fromCriterion 
                    = results.getNewCriterion(MessageRowPeer.UID, new Long(low),
                        Criteria.GREATER_EQUAL);
                if (high > 0 && high < Long.MAX_VALUE) {
                    final Criteria.Criterion toCriterion 
                        = results.getNewCriterion(MessageRowPeer.UID, new Long(high),
                            Criteria.LESS_EQUAL);
                    fromCriterion.and(toCriterion);
                }
                results.add(fromCriterion);
            }
        }
    }
    
    public void remove(GeneralMessageSet set, MailboxSession mailboxSession) throws MailboxManagerException {
        try {
            lock.writeLock().acquire();
            try {
                final Flags flags = new Flags(Flags.Flag.DELETED);
                doSetFlags(flags, true, false, set, FetchGroupImpl.MINIMAL, mailboxSession);
                doExpunge(set, FetchGroupImpl.MINIMAL);
            } finally {
                lock.writeLock().release();
            }

        } catch (InterruptedException e) {
            throw new MailboxManagerException(e);
        }
    }

    public boolean isWriteable() {
        return true;
    }

    public void setLog(Log log) {
        super.setLog(log);
        searches.setLog(log);
    }    
}
