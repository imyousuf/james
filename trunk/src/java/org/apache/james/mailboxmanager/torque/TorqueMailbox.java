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
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;

import org.apache.commons.logging.Log;
import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.MailboxListener;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
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
import org.apache.torque.NoRowsException;
import org.apache.torque.TooManyRowsException;
import org.apache.torque.TorqueException;
import org.apache.torque.util.Criteria;

import com.sun.mail.util.CRLFOutputStream;
import com.workingdogs.village.DataSetException;

public class TorqueMailbox extends AbstractGeneralMailbox implements ImapMailbox {

    private boolean open = true;

    private MailboxRow mailboxRow;

    private Flags permanentFlags;

    private UidChangeTracker tracker;

    private MailboxEventDispatcher eventDispatcher = new MailboxEventDispatcher();
    
    TorqueMailbox(MailboxRow mailboxRow, UidChangeTracker tracker, Log log) {
        setLog(log);
        this.mailboxRow = mailboxRow;
        this.tracker = tracker;
        tracker.addMailboxListener(getEventDispatcher());
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
        checkAccess();
        try {
            return getMailboxRow().countMessages();
        } catch (Exception e) {
            throw new MailboxManagerException(e);
        }
    }

    public MessageResult appendMessage(MimeMessage message, Date internalDate,
            int result) throws MailboxManagerException {
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
        checkAccess();
        if (!set.isValid()) {
            return new MessageResult[0];
        }
        Criteria c = criteriaForMessageSet(set);
        UidRange range = uidRangeForMessageSet(set);
        try {
            List l = getMailboxRow().getMessageRows(c);

            MessageResult[] messageResults = fillMessageResult(l, result
                    | MessageResult.UID | MessageResult.FLAGS);
            checkForScanGap(range.getFromUid());
            getUidChangeTracker().found(range, messageResults, null);
            return messageResults;
        } catch (TorqueException e) {
            throw new MailboxManagerException(e);
        } catch (MessagingException e) {
            throw new MailboxManagerException(e);
        }

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
            MessageRow messageRow = (MessageRow) iter.next();
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
        if (result != 0) {
            throw new RuntimeException("Unsupportet result: " + result);
        }
        
        return messageResult;
    }



    public synchronized Flags getPermanentFlags() {
        if (permanentFlags == null) {
            permanentFlags = new Flags();
            permanentFlags.add(Flags.Flag.ANSWERED);
            permanentFlags.add(Flags.Flag.DELETED);
            permanentFlags.add(Flags.Flag.DRAFT);
            permanentFlags.add(Flags.Flag.FLAGGED);
            permanentFlags.add(Flags.Flag.RECENT);
            permanentFlags.add(Flags.Flag.SEEN);
        }
        return permanentFlags;
    }

    public int getRecentCount(boolean reset) throws MailboxManagerException {
        checkAccess();
        Flags flags = new Flags();
        flags.add(Flags.Flag.RECENT);
        try {
            int count = getMailboxRow().countMessages(flags, true);
            return count;
        } catch (TorqueException e) {
            throw new MailboxManagerException(e);
        } catch (DataSetException e) {
            throw new MailboxManagerException(e);
        }

    }

    public MessageResult getFirstUnseen(int result)
            throws MailboxManagerException {
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

    }

    public int getUnseenCount() throws MailboxManagerException {
        checkAccess();
        try {
            final int count = getMailboxRow().countMessages(new Flags(Flags.Flag.SEEN), false);
            return count;
        } catch (TorqueException e) {
            throw new MailboxManagerException(e);
        } catch (DataSetException e) {
            throw new MailboxManagerException(e);
        }
    }

    public MessageResult[] expunge(GeneralMessageSet set, int result)
            throws MailboxManagerException {
        checkAccess();
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
        checkAccess();
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
        checkAccess();
        return getMailboxRow().getUidValidity();
    }

    public synchronized long getUidNext() throws MailboxManagerException {
        checkAccess();
        try {
            MailboxRow myMailboxRow = MailboxRowPeer.retrieveByPK(mailboxRow.getLastUid());
            if (myMailboxRow != null) {
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

    public MessageResult[] search(GeneralMessageSet set, SearchTerm searchTerm, int result) {
        final Log log = getLog();
        // TODO implementation
        if (log.isWarnEnabled()) {
            log.warn("Search is not yet implemented. Sorry.");
        }
        MessageResult[] results = {};
        return results;
    }

}
