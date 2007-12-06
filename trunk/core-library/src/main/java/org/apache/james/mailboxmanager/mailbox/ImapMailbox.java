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

import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.SearchParameters;




/**
 * Provides access to a single Folder.<br />
 */
public interface ImapMailbox extends Mailbox, EventTriggerMailbox  {
    
    public static final long ANONYMOUS_SESSION = 0;
    
    /**
     * @param result
     *            which fields to be returned in MessageResult
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
    Iterator search(GeneralMessageSet set,SearchParameters searchTerm, int result) throws MailboxManagerException;
    
    
    long getUidValidity() throws MailboxManagerException;
    
    /**
     * 
     * @return the uid that will be assigned to the next appended message
     * @throws MailboxManagerException 
     */

    long getUidNext() throws MailboxManagerException;
    
    
    /**
     * @return Flags that can be stored
     */

    Flags getPermanentFlags();
    

    int getRecentCount(boolean reset) throws MailboxManagerException;
    
    int getUnseenCount() throws MailboxManagerException;
    
    MessageResult getFirstUnseen(int result) throws MailboxManagerException;
    
    /**
     * 
     * @param set
     *            <ul>
     *            <li> IMAP, Javamail: not required, always expunge all</li>
     *            <li> UIDPLUS: requires the possibility of defining a uid range</li>
     *            </ul>
     * 
     * @param result
     *            which fields to be returned in MessageResult
     * @return {@link MessageResult} <code>Iterator</code> with 
     * the fields defined by <b>result</b><br />
     *         <ul>
     *         <li> IMAP, UIDPLUS: nothing required </li>
     *         <li> Javamail Folder: requires the expunged Message[]</li>
     *         </ul>
     * @throws MailboxManagerException
     *             if anything went wrong
     */
    Iterator expunge(GeneralMessageSet set, int result)
            throws MailboxManagerException;
    

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
     * @param result fetch group for results 
     * @return {@link MessageResult} <code>Iterator</code> containing messages
     * whose flags have been updated, not null
     * @throws MailboxManagerException
     */
    Iterator setFlags(Flags flags, boolean value, boolean replace, 
            GeneralMessageSet set, int result) throws MailboxManagerException;
    
    
    
    public long getSessionId(); 

    /**
     * @param internalDate
     *            <p>IMAP defines this as the time when the message has arrived to
     *            this server (by smtp). Clients are also allowed to set the
     *            internalDate on apppend.</p><p>Is this Mail.getLastUpdates() for 
     *            James delivery? Should we use MimeMessage.getReceivedDate()?
     * @param result
     *            which fields to be returned in MessageResult
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
            int result) throws MailboxManagerException;
    
    /**
     * 
     * @param set
     * @return MessageResult with the fields defined by <b>result</b>
     *         <ul>
     *         <li> IMAP: a set of msn, uid, Flags, header lines, content, mime
     *         parts...</li>
     *         <li> Javamail Folder: Message[]</li>
     *         </ul>
     * @throws MailboxManagerException 
     */

    Iterator getMessages(GeneralMessageSet set, int result) throws MailboxManagerException;
    
    void remove(GeneralMessageSet set) throws MailboxManagerException;
}
