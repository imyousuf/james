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
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.MailboxListener;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.SearchParameters;
import org.apache.james.mailboxmanager.UnsupportedCriteriaException;
import org.apache.james.mailboxmanager.MessageResult.Content;
import org.apache.james.mailboxmanager.impl.GeneralMessageSetImpl;
import org.apache.james.mailboxmanager.impl.MailboxEventDispatcher;
import org.apache.james.mailboxmanager.impl.MessageResultImpl;
import org.apache.james.mailboxmanager.mailbox.AbstractGeneralMailbox;
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

public class TorqueMailbox extends AbstractGeneralMailbox implements ImapMailbox {

    private boolean open = true;

    private MailboxRow mailboxRow;

    private UidChangeTracker tracker;

    private MailboxEventDispatcher eventDispatcher = new MailboxEventDispatcher();

    private UidToKeyConverter uidToKeyConverter;
    
    private final ReadWriteLock lock;
    
    TorqueMailbox(final MailboxRow mailboxRow, final UidChangeTracker tracker, final ReadWriteLock lock, final Log log) {
        setLog(log);
        this.mailboxRow = mailboxRow;
        this.tracker = tracker;
        this.lock = lock;
        tracker.addMailboxListener(getEventDispatcher());
        getUidToKeyConverter().setUidValidity(mailboxRow.getUidValidity());
    }

    public int getMessageResultTypes() {
        return MessageResult.FLAGS + MessageResult.INTERNAL_DATE
                + MessageResult.KEY + MessageResult.MIME_MESSAGE
                + MessageResult.SIZE + MessageResult.UID;
    }

    public int getMessageSetTypes() {
        return GeneralMessageSet.TYPE_ALL + GeneralMessageSet.TYPE_KEY
                + GeneralMessageSet.TYPE_UID + GeneralMessageSet.TYPE_MESSAGE;
    }

    public synchronized String getName() throws MailboxManagerException {
        checkAccess();
        return getUidChangeTracker().getMailboxName();
    }

    public int getMessageCount() throws MailboxManagerException {
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
            int result) throws MailboxManagerException {
        try {
            lock.readLock().acquire();
            try {
                checkAccess();
                final MailboxRow myMailboxRow;
                try {
                    myMailboxRow = getMailboxRow().consumeNextUid();
                } catch (TorqueException e) {
                    throw new MailboxManagerException(e);
                } catch (SQLException e) {
                    throw new MailboxManagerException(e);
                }
                if (myMailboxRow != null) {
                    try {
                        // To be thread safe, we first get our own copy and the
                        // exclusive
                        // Uid
                        // TODO create own message_id and assign uid later
                        // at the moment it could lead to the situation that uid 5 is
                        // insertet long before 4, when
                        // mail 4 is big and comes over a slow connection.
                        long uid = myMailboxRow.getLastUid();
                        this.mailboxRow = myMailboxRow;

                        MessageRow messageRow = new MessageRow();
                        messageRow.setMailboxId(getMailboxRow().getMailboxId());
                        messageRow.setUid(uid);
                        messageRow.setInternalDate(internalDate);

                        // TODO very ugly size mesurement
                        ByteArrayOutputStream sizeBos = new ByteArrayOutputStream();
                        message.writeTo(new CRLFOutputStream(sizeBos));
                        messageRow.setSize(sizeBos.size());
                        MessageFlags messageFlags = new MessageFlags();
                        messageFlags.setFlags(message.getFlags());
                        messageRow.addMessageFlags(messageFlags);

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

                        MessageBody mb = new MessageBody();

                        InputStream is = message.getInputStream();

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buf = new byte[4096];
                        int read;
                        while ((read = is.read(buf)) > 0) {
                            baos.write(buf, 0, read);
                        }

                        mb.setBody(baos.toByteArray());
                        messageRow.addMessageBody(mb);

                        messageRow.save();
                        MessageResult messageResult = fillMessageResult(messageRow,
                                result | MessageResult.UID);
                        checkForScanGap(uid);
                        getUidChangeTracker().found(messageResult, null);
                        return messageResult;
                    } catch (Exception e) {
                        throw new MailboxManagerException(e);
                    }
                } else {
                    // mailboxRow==null
                    getUidChangeTracker().mailboxNotFound();
                    throw new MailboxManagerException("Mailbox has been deleted");
                }
            } finally {
                lock.readLock().release();
            }
        } catch (InterruptedException e) {
            throw new MailboxManagerException(e);
        }
    }

    private void checkForScanGap(long uid) throws MailboxManagerException, TorqueException, MessagingException {
        synchronized (getUidChangeTracker()) {
            long lastScannedUid=getUidChangeTracker().getLastScannedUid();
            if (uid>(lastScannedUid+1)) {
                GeneralMessageSet set=GeneralMessageSetImpl.uidRange(lastScannedUid+1, uid);
                Criteria criteria=criteriaForMessageSet(set);
                List messageRows=mailboxRow.getMessageRows(criteria);
                MessageResult[] messageResults=fillMessageResult(messageRows, MessageResult.UID);
                getUidChangeTracker().found(uidRangeForMessageSet(set), messageResults, null);
            }
        }
        
    }

    public MessageResult updateMessage(GeneralMessageSet messageSet, MimeMessage message, int result) {
        throw new RuntimeException("not yet implemented");
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
            throw new MailboxManagerException("unsupported MessageSet: "
                    + set.getType());
        }
        return criteria;
    }

    public MessageResult[] getMessages(GeneralMessageSet set, int result)
            throws MailboxManagerException {
        try {
            lock.readLock().acquire();
            try {
                checkAccess();
                set=toUidSet(set);
                if (!set.isValid() || set.getType()==GeneralMessageSet.TYPE_NOTHING) {
                    return new MessageResult[0];
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

    private MessageResult[] getMessages(int result, UidRange range, Criteria c) throws TorqueException, MessagingException, MailboxManagerException {
        List l = MessageRowPeer.doSelectJoinMessageFlags(c);
        MessageResult[] messageResults = fillMessageResult(l, result
                | MessageResult.UID | MessageResult.FLAGS);
        checkForScanGap(range.getFromUid());
        getUidChangeTracker().found(range, messageResults, null);
        return messageResults;
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

    private MessageResult[] fillMessageResult(List messageRows, int result)
            throws TorqueException, MessagingException, MailboxManagerException {
        MessageResult[] messageResults = new MessageResult[messageRows.size()];
        int i = 0;
        for (Iterator iter = messageRows.iterator(); iter.hasNext();) {
            MessageRow messageRow=(MessageRow)iter.next();
            messageResults[i] = fillMessageResult(messageRow, result);
            i++;
        }
        return messageResults;
    }

    private MessageResult fillMessageResult(MessageRow messageRow, int result)
            throws TorqueException, MessagingException, MailboxManagerException {
        MessageResultImpl messageResult = new MessageResultImpl();
        if ((result & MessageResult.MIME_MESSAGE) > 0) {
            messageResult.setMimeMessage(TorqueMimeMessage.createMessage(messageRow,getLog()));
            result -= MessageResult.MIME_MESSAGE;
        }
        if ((result & MessageResult.UID) > 0) {
            messageResult.setUid(messageRow.getUid());
            result -= MessageResult.UID;
        }
        if ((result & MessageResult.FLAGS) > 0) {
            MessageFlags messageFlags=messageRow.getMessageFlags();
            if (messageFlags!=null) {
                messageResult.setFlags(messageFlags.getFlagsObject());  
            }
            result -= MessageResult.FLAGS;
        }
        if ((result & MessageResult.SIZE) > 0) {
            messageResult.setSize(messageRow.getSize());
            result -= MessageResult.SIZE;
        }
        if ((result & MessageResult.INTERNAL_DATE) > 0) {
            messageResult.setInternalDate(messageRow.getInternalDate());
            result -= MessageResult.INTERNAL_DATE;
        }
        if ((result & MessageResult.KEY) > 0) {
            messageResult.setKey(getUidToKeyConverter().toKey(messageRow.getUid()));
            result -= MessageResult.KEY;
        }
        if ((result & MessageResult.HEADERS) > 0) {
            messageResult.setHeaders(createHeaders(messageRow));
            result -= MessageResult.HEADERS;
        }
        if ((result & MessageResult.BODY_CONTENT) > 0) {
            messageResult.setMessageBody(createBodyContent(messageRow));
            result -= MessageResult.BODY_CONTENT;
        }
        if ((result & MessageResult.FULL_CONTENT) > 0) {
            messageResult.setFullMessage(createFullContent(messageRow, messageResult.getHeaders()));
            result -= MessageResult.FULL_CONTENT;
        }
        if (result != 0) {
            throw new RuntimeException("Unsupported result: " + result);
        }
        
        return messageResult;
    }
    
    private final static class FullContent implements MessageResult.Content {
        private final byte[] contents;
        private final List headers;
        private final long size;
        
        public FullContent(final byte[] contents, final List headers) throws MessagingException {
            this.contents =  contents;
            this.headers = headers;
            this.size = caculateSize();
        }

        private long caculateSize() throws MessagingException{
            long result = contents.length + MessageUtils.countUnnormalLines(contents);
            result += 2;
            for (final Iterator it=headers.iterator(); it.hasNext();) {
                final MessageResult.Header header = (MessageResult.Header) it.next();
                if (header != null) {
                    result += header.size();
                    result += 2;
                }
            }
            return result;
        }

        public void writeTo(StringBuffer buffer) throws MessagingException {
            for (final Iterator it=headers.iterator(); it.hasNext();) {
                final MessageResult.Header header = (MessageResult.Header) it.next();
                if (header != null) {
                    header.writeTo(buffer);
                }
                buffer.append('\r');
                buffer.append('\n');
            }
            buffer.append('\r');
            buffer.append('\n');
            MessageUtils.normalisedWriteTo(contents, buffer);
        }

        public long size() throws MessagingException {
            return size;
        }
    }

    private Content createFullContent(final MessageRow messageRow, List headers) throws TorqueException, MessagingException {
        if (headers == null) {
            headers = createHeaders(messageRow);
        }
        final MessageBody body = (MessageBody) messageRow.getMessageBodys().get(0);
        final byte[] bytes = body.getBody();
        final FullContent results = new FullContent(bytes, headers);
        return results;
    }
    
    private Content createBodyContent(MessageRow messageRow) throws TorqueException {
        final MessageBody body = (MessageBody) messageRow.getMessageBodys().get(0);
        final byte[] bytes = body.getBody();
        final ByteContent result = new ByteContent(bytes);
        return result;
    }
    
    private final static class ByteContent implements MessageResult.Content {
       
        private final byte[] contents;
        private final long size;
        public ByteContent(final byte[] contents) {
            this.contents = contents;
            size = contents.length + MessageUtils.countUnnormalLines(contents);
        }
        
        public long size() throws MessagingException {
            return size;
        }
        
        public void writeTo(StringBuffer buffer) throws MessagingException {
            MessageUtils.normalisedWriteTo(contents, buffer);
        }
    } 
    
    private List createHeaders(MessageRow messageRow) throws TorqueException {
        final List headers=messageRow.getMessageHeaders();
        Collections.sort(headers, new Comparator() {

            public int compare(Object one, Object two) {
                return ((MessageHeader) one).getLineNumber() - ((MessageHeader)two).getLineNumber();
            }
            
        });
        
        final List results = new ArrayList(headers.size());
        for (Iterator it=headers.iterator();it.hasNext();) {
            final MessageHeader messageHeader = (MessageHeader) it.next();
            final Header header = new Header(messageHeader);
            results.add(header);
        }
        return results;
    }
    
    private static final class Header implements MessageResult.Header, MessageResult.Content {
        private final String name;
        private final String value;
        private final long size;
        
        public Header(final MessageHeader header) {
            this.name = header.getField();
            this.value = header.getValue();
            size = name.length() + value.length() + 2;
        }
        
        public Content getContent() throws MessagingException {
            return this;
        }

        public String getName() throws MessagingException {
            return name;
        }

        public String getValue() throws MessagingException {
            return value;
        }

        public long size() throws MessagingException {
            return size;
        }

        public void writeTo(StringBuffer buffer) throws MessagingException {
// TODO: sort out encoding
            for (int i=0; i<name.length();i++) {
                buffer.append((char)(byte) name.charAt(i));
            }
            buffer.append(':');
            buffer.append(' ');
            for (int i=0; i<value.length();i++) {
                buffer.append((char)(byte) value.charAt(i));
            }
        }
        
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

    public int getRecentCount(boolean reset) throws MailboxManagerException {
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

    public MessageResult getFirstUnseen(int result)
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
                        MessageResult messageResult=fillMessageResult((MessageRow) messageRows.get(0), result | MessageResult.UID);
                        if (messageResult!=null) {
                            checkForScanGap(messageResult.getUid());
                            getUidChangeTracker().found(messageResult,null);
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

    public int getUnseenCount() throws MailboxManagerException {
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

    public MessageResult[] expunge(GeneralMessageSet set, int result)
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

    private MessageResult[] doExpunge(GeneralMessageSet set, int result) throws MailboxManagerException {
        checkAccess();
        set=toUidSet(set);  
        if (!set.isValid() || set.getType()==GeneralMessageSet.TYPE_NOTHING) {
            return new MessageResult[0];
        }
        try {
            // TODO put this into a serializable transaction
            final Criteria c = criteriaForMessageSet(set);
            c.addJoin(MessageRowPeer.MAILBOX_ID, MessageFlagsPeer.MAILBOX_ID);
            c.addJoin(MessageRowPeer.UID, MessageFlagsPeer.UID);
            c.add(MessageRowPeer.MAILBOX_ID, getMailboxRow().getMailboxId());
            c.add(MessageFlagsPeer.DELETED, true);

            final List messageRows = getMailboxRow().getMessageRows(c);
            final MessageResult[] messageResults = fillMessageResult(messageRows, result
                    | MessageResult.UID | MessageResult.FLAGS);

            for (Iterator iter = messageRows.iterator(); iter.hasNext();) {
                MessageRow messageRow = (MessageRow) iter.next();
                Criteria todelc=new Criteria();
                todelc.add(MessageRowPeer.MAILBOX_ID,messageRow.getMailboxId());
                todelc.add(MessageRowPeer.UID,messageRow.getUid());
                MessageRowPeer.doDelete(todelc);
            }
            getUidChangeTracker().expunged(messageResults);
            return messageResults;
        } catch (Exception e) {
            throw new MailboxManagerException(e);
        }
    }

    public void setFlags(Flags flags, boolean value, boolean replace,
            GeneralMessageSet set, MailboxListener silentListener)
            throws MailboxManagerException {
        try {
            lock.writeLock().acquire();
            try {
                doSetFlags(flags, value, replace, set, silentListener);
            } finally {
                lock.writeLock().release();
            }

        } catch (InterruptedException e) {
            throw new MailboxManagerException(e);
        }
    }

    private void doSetFlags(Flags flags, boolean value, boolean replace, GeneralMessageSet set, MailboxListener silentListener) throws MailboxManagerException {
        checkAccess();
        set=toUidSet(set);  
        if (!set.isValid() || set.getType()==GeneralMessageSet.TYPE_NOTHING) {
            return;
        }        
        try {
            // TODO put this into a serializeable transaction
            final List messageRows = getMailboxRow()
                    .getMessageRows(criteriaForMessageSet(set));
            final MessageResult[] beforeResults = fillMessageResult(messageRows,
                    MessageResult.UID | MessageResult.FLAGS);
            UidRange uidRange=uidRangeForMessageSet(set);
            checkForScanGap(uidRange.getFromUid());
            getUidChangeTracker().found(uidRange, beforeResults, null);
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
            final MessageResult[] afterResults = fillMessageResult(messageRows,
                    MessageResult.UID | MessageResult.FLAGS);
            tracker.found(uidRange, afterResults, silentListener);
        } catch (Exception e) {
            throw new MailboxManagerException(e);
        }
    }

    public void addListener(MailboxListener listener, int result) throws MailboxManagerException {
        getEventDispatcher().addMailboxListener(listener);
        checkAccess();
    }

    public void removeListener(MailboxListener mailboxListener) {
        if (!open || getEventDispatcher().size() == 0) {
          throw new RuntimeException("mailbox not open");
        }
        getEventDispatcher().removeMailboxListener(mailboxListener);
        if (getEventDispatcher().size() == 0) {
            open = false;
            getUidChangeTracker().removeMailboxListener(getEventDispatcher());
        }
    }

    public synchronized long getUidValidity() throws MailboxManagerException {
        try {
            lock.writeLock().acquire();
            try {
                checkAccess();
                final long result = getMailboxRow().getUidValidity();
                return result;
            } finally {
                lock.writeLock().release();
            }

        } catch (InterruptedException e) {
            throw new MailboxManagerException(e);
        }

    }

    public synchronized long getUidNext() throws MailboxManagerException {
        try {
            lock.writeLock().acquire();
            try {
                checkAccess();
                try {
                    MailboxRow myMailboxRow = MailboxRowPeer.retrieveByPK(mailboxRow.getPrimaryKey());
                    if (myMailboxRow != null) {
                        mailboxRow=myMailboxRow;
                        getUidChangeTracker().foundLastUid(mailboxRow.getLastUid());
                        return getUidChangeTracker().getLastUid() + 1;
                    } else {
                        getUidChangeTracker().mailboxNotFound();
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
                lock.writeLock().release();
            }

        } catch (InterruptedException e) {
            throw new MailboxManagerException(e);
        }
    }

    private void checkAccess() throws MailboxManagerException {
        if (!open) {
            throw new RuntimeException("mailbox is closed");
        } else if (getEventDispatcher().size() == 0) {
            throw new RuntimeException("mailbox has not been opened");
        } else if (getUidChangeTracker().isExisting()) {
            throw new MailboxManagerException("Mailbox is not existing");
        }
    }
    

    protected MailboxEventDispatcher getEventDispatcher() {
        if (eventDispatcher == null) {
            eventDispatcher = new MailboxEventDispatcher();
        }
        return eventDispatcher;
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

    public MessageResult[] search(GeneralMessageSet set, SearchParameters parameters,
            int result) throws MailboxManagerException {
        try {
            lock.readLock().acquire();
            try {
                checkAccess();
                set=toUidSet(set);
                if (!set.isValid() || set.getType()==GeneralMessageSet.TYPE_NOTHING) {
                    return new MessageResult[0];
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
                
                final MessageResult[] results = getMessages(result, new UidRange(1, -1), builder.getCriteria());
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

    public void remove(GeneralMessageSet set) throws MailboxManagerException {
        try {
            lock.writeLock().acquire();
            try {
                final Flags flags = new Flags(Flags.Flag.DELETED);
                doSetFlags(flags, true, false, set, null);
                doExpunge(set, MessageResult.NOTHING);
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
    
}
