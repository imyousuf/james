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

package org.apache.james.mailboxmanager.mailbox;

import java.util.Date;
import java.util.Iterator;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;

import org.apache.james.mailboxmanager.MessageRange;
import org.apache.james.mailboxmanager.MailboxListener;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MailboxSession;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.SearchQuery;
import org.apache.james.mailboxmanager.UnsupportedCriteriaException;
import org.apache.james.mailboxmanager.MessageResult.FetchGroup;

public interface Mailbox {

    public static final long ANONYMOUS_SESSION = 0;
    
    /**
     * Example #mail.paul.lists.apache.james-dev (3rd level folder of user paul)
     * 
     * @return Full folder name with namespace
     * @throws MailboxManagerException
     */
    String getName();

    int getMessageCount(MailboxSession mailboxSession) throws MailboxManagerException;

    boolean isWriteable();
    
    
    /**
     * @param fetchGroup
     *            which fields to be returned in MessageResult
     * @param mailboxSession TODO
     * @return MessageResult with the fields defined by <b>result</b>
     *         <ul>
     *         <li> IMAP: msn or (msn and uid)</li>
     *         <li> Javamail Folder: Message[]</li>
     *         </ul>
     * @throws MailboxManagerException
     *             if anything went wrong
     * @throws UnsupportedCriteriaException when any of the search parameters are 
     * not supported by this mailbox
     */
    Iterator search(SearchQuery searchQuery, FetchGroup fetchGroup, MailboxSession mailboxSession) throws MailboxManagerException;
    
    
    long getUidValidity(MailboxSession mailboxSession) throws MailboxManagerException;
    
    /**
     * 
     * @param mailboxSession TODO
     * @return the uid that will be assigned to the next appended message
     * @throws MailboxManagerException 
     */

    long getUidNext(MailboxSession mailboxSession) throws MailboxManagerException;
    
    
    /**
     * @return Flags that can be stored
     */
    Flags getPermanentFlags();
    

    long[] recent(boolean reset, MailboxSession mailboxSession) throws MailboxManagerException;
    
    int getUnseenCount(MailboxSession mailboxSession) throws MailboxManagerException;
    
    MessageResult getFirstUnseen(FetchGroup fetchGroup, MailboxSession mailboxSession) throws MailboxManagerException;
    
    /**
     * 
     * @param set
     *            <ul>
     *            <li> IMAP, Javamail: not required, always expunge all</li>
     *            <li> UIDPLUS: requires the possibility of defining a uid range</li>
     *            </ul>
     * @param fetchGroup
     *            which fields to be returned in MessageResult
     * @param mailboxSession TODO
     * 
     * @return {@link MessageResult} <code>Iterator</code> with 
     * the fields defined by <b>result</b><br />
     *         <ul>
     *         <li> IMAP, UIDPLUS: nothing required </li>
     *         <li> Javamail Folder: requires the expunged Message[]</li>
     *         </ul>
     * @throws MailboxManagerException
     *             if anything went wrong
     */
    Iterator expunge(MessageRange set, FetchGroup fetchGroup, MailboxSession mailboxSession) throws MailboxManagerException;
    

    /**
     * this is much more straight forward for IMAP instead of setting Flags of
     * an array of lazy-loading MimeMessages. <br />
     * required by IMAP
     * 
     * @param flags
     *            Flags to be set
     * @param value
     *            true = set, false = unset
     * @param replace
     *            replace all Flags with this flags, value has to be true
     * @param set
     *            the range of messages
     * @param fetchGroup fetch group for results 
     * @param mailboxSession TODO
     * @return {@link MessageResult} <code>Iterator</code> containing messages
     * whose flags have been updated, not null
     * @throws MailboxManagerException
     */
    Iterator setFlags(Flags flags, boolean value, boolean replace, 
            MessageRange set, FetchGroup fetchGroup, MailboxSession mailboxSession) throws MailboxManagerException;
    
    /**
     * @param internalDate
     *            <p>IMAP defines this as the time when the message has arrived to
     *            this server (by smtp). Clients are also allowed to set the
     *            internalDate on apppend.</p><p>Is this Mail.getLastUpdates() for 
     *            James delivery? Should we use MimeMessage.getReceivedDate()?
     * @param fetchGroup
     *            which fields to be returned in MessageResult
     *            or null for minimal fetch group only
     * @param mailboxSession TODO
     * @return MessageResult with the fields defined by <b>result</b>
     *         <ul>
     *         <li> IMAP, Javamail Folder: nothing required </li>
     *         <li> UIDPlusFolder: requires to return appended Message or uid</li>
     *         <li> UIDPLUS: requires to return appended uid</li>
     *         </ul>
     * @throws MailboxManagerException
     *             if anything went wrong
     */
    MessageResult appendMessage(MimeMessage message, Date internalDate,
            FetchGroup fetchGroup, MailboxSession mailboxSession) throws MailboxManagerException;
    
    /**
     * TODO: consolidate search and getMessages into a single method
     * @param set
     * @param mailboxSession TODO
     * @return MessageResult with the fields defined by <b>result</b>
     *         <ul>
     *         <li> IMAP: a set of msn, uid, Flags, header lines, content, mime
     *         parts...</li>
     *         <li> Javamail Folder: Message[]</li>
     *         </ul>
     * @throws MailboxManagerException 
     */

    Iterator getMessages(MessageRange set, FetchGroup fetchGroup, MailboxSession mailboxSession) 
        throws MailboxManagerException;

    /**
     * Implementations of Mailbox may interpret the fact that someone is
     * listening and do some caching and even postpone persistence until
     * everyone has removed itself.
     * 
     * @param listener
     * @throws MailboxManagerException 
     */
    void addListener(MailboxListener listener) throws MailboxManagerException;

    void removeListener(MailboxListener listener);

}