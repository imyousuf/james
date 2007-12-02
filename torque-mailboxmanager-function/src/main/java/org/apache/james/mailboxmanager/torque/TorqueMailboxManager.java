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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.SimpleLog;
import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.ListResult;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MailboxNotFoundException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.impl.ListResultImpl;
import org.apache.james.mailboxmanager.mailbox.GeneralMailbox;
import org.apache.james.mailboxmanager.mailbox.GeneralMailboxSession;
import org.apache.james.mailboxmanager.mailbox.ImapMailbox;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;
import org.apache.james.mailboxmanager.mailbox.MailboxSession;
import org.apache.james.mailboxmanager.manager.MailboxExpression;
import org.apache.james.mailboxmanager.manager.MailboxManager;
import org.apache.james.mailboxmanager.torque.om.MailboxRow;
import org.apache.james.mailboxmanager.torque.om.MailboxRowPeer;
import org.apache.james.mailboxmanager.tracking.MailboxCache;
import org.apache.james.mailboxmanager.tracking.UidChangeTracker;
import org.apache.james.mailboxmanager.wrapper.ImapMailboxSessionWrapper;
import org.apache.james.services.User;
import org.apache.torque.TorqueException;
import org.apache.torque.util.CountHelper;
import org.apache.torque.util.Criteria;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;

public class TorqueMailboxManager implements MailboxManager {

    private static final char SQL_WILDCARD_CHAR = '%';
    private final static Random random = new Random();
    private MailboxCache mailboxCache;
    
    protected Log log = LogFactory.getLog(TorqueMailboxManager.class);

    private final ReadWriteLock lock;
    
    public TorqueMailboxManager() {
        this.lock =  new WriterPreferenceReadWriteLock();
        mailboxCache = new MailboxCache();
    }
    
    public MailboxSession getMailboxSession(String mailboxName,
            boolean autoCreate) throws MailboxManagerException {
        if (autoCreate && !existsMailbox(mailboxName)) {
            getLog().info("autocreated mailbox  " + mailboxName);
            createMailbox(mailboxName);
        }
        return getImapMailboxSession(mailboxName);
    }
    


    public GeneralMailboxSession getGeneralMailboxSession(String mailboxName)
    throws MailboxManagerException {
        return getImapMailboxSession(mailboxName);
    }
    
    public boolean createInbox(User user) throws MailboxManagerException {
        // TODO should access getPersonalDefaultNameSpace...
        String userInbox=USER_NAMESPACE+HIERARCHY_DELIMITER+user.getUserName()+HIERARCHY_DELIMITER+"INBOX";
        if (!existsMailbox(userInbox)) {
            createMailbox(userInbox);
            getLog().info("autocreated Inbox  " + userInbox);
            return true;    
        }  else {
            getLog().debug("Inbox already exists " + userInbox);
            return false;    
        }
    }
    
    
    
    public ImapMailboxSession getImapMailboxSession(String mailboxName)
            throws MailboxManagerException {

        try {
            synchronized (getMailboxCache()) {
                MailboxRow mailboxRow = MailboxRowPeer
                        .retrieveByName(mailboxName);

                if (mailboxRow != null) {
                    UidChangeTracker tracker = (UidChangeTracker) getMailboxCache()
                            .getMailboxTracker(mailboxName,
                                    UidChangeTracker.class);
                    if (tracker == null) {
                        tracker = new UidChangeTracker(getMailboxCache(),
                                mailboxName, mailboxRow.getLastUid());
                        getMailboxCache().add(mailboxName, tracker);
                    }
                    getLog().info("created ImapMailboxSession "+mailboxName);
                    final ImapMailbox torqueMailbox = new TorqueMailbox(
                                                mailboxRow, tracker, lock, getLog(), random.nextLong());
                    final ImapMailboxSessionWrapper wrapper 
                        = new ImapMailboxSessionWrapper(torqueMailbox);
                    return wrapper;
                } else {
                    getLog().info("Mailbox '" + mailboxName + "' not found.");
                    getMailboxCache().notFound(mailboxName);
                    throw new MailboxNotFoundException(mailboxName);
                }
            }
        } catch (TorqueException e) {
            throw new MailboxManagerException(e);
        }
    }
    
    private MailboxCache getMailboxCache() {
       return mailboxCache;
    }
    

    public void createMailbox(String namespaceName)
            throws MailboxManagerException {
        getLog().info("createMailbox "+namespaceName);
        synchronized (getMailboxCache()) {
            MailboxRow mr = new MailboxRow();
            mr.setName(namespaceName);
            mr.setLastUid(0);
            mr.setUidValidity(Math.abs(random.nextInt()));
            try {
                mr.save();
            } catch (Exception e) {
                throw new MailboxManagerException(e);
            }
        }
    }

    public void deleteMailbox(String mailboxName)
            throws MailboxManagerException {
        getLog().info("deleteMailbox "+mailboxName);
        synchronized (getMailboxCache()) {
            try {
                // TODO put this into a serilizable transaction
                MailboxRow mr = MailboxRowPeer.retrieveByName(mailboxName);
                if (mr == null) {
                    throw new MailboxManagerException("Mailbox not found");
                }
                MailboxRowPeer.doDelete(mr);
                getMailboxCache().notFound(mailboxName);
            } catch (TorqueException e) {
                throw new MailboxManagerException(e);
            }
        }
    }

    public void renameMailbox(String from, String to)
            throws MailboxManagerException {
        try {
            getLog().info("renameMailbox "+from+" to "+to);
            synchronized (getMailboxCache()) {
                // TODO put this into a serilizable transaction
                MailboxRow mr = MailboxRowPeer.retrieveByName(from);
                if (mr == null) {
                    throw new MailboxManagerException("Mailbox not found");
                }
                mr.setName(to);
                mr.save();
                
                // rename submailbox
                
                Criteria c = new Criteria();
                c.add(MailboxRowPeer.NAME,
                        (Object) (from + HIERARCHY_DELIMITER + "%"),
                        Criteria.LIKE);
                List l = MailboxRowPeer.doSelect(c);
                for (Iterator iter = l.iterator(); iter.hasNext();) {
                    MailboxRow sub = (MailboxRow) iter.next();
                    String subOrigName=sub.getName();
                    String subNewName=to + subOrigName.substring(from.length());
                    sub.setName(to + sub.getName().substring(from.length()));
                    sub.save();
                    getLog().info("renameMailbox sub-mailbox "+subOrigName+" to "+subNewName);
                    getMailboxCache().renamed(subOrigName,subNewName);
                }
            }
        } catch (Exception e) {
            throw new MailboxManagerException(e);
        }

    }

    public void copyMessages(GeneralMailbox from, GeneralMessageSet set, String to) throws MailboxManagerException {
        GeneralMailboxSession toMailbox=(GeneralMailboxSession)getGeneralMailboxSession(to);
        
        Iterator it = from.getMessages(set, MessageResult.MIME_MESSAGE | MessageResult.INTERNAL_DATE);
        while (it.hasNext()) {
            final MessageResult result = (MessageResult) it.next();
            final MimeMessage mimeMessage = result.getMimeMessage();
            toMailbox.appendMessage(mimeMessage, result.getInternalDate(), MessageResult.NOTHING);
        }
       
        toMailbox.close();
    }

    public ListResult[] list(final MailboxExpression mailboxExpression) throws MailboxManagerException {
        final char localWildcard = mailboxExpression.getLocalWildcard();
        final char freeWildcard = mailboxExpression.getFreeWildcard();
        final String base = mailboxExpression.getBase();
        final int baseLength;
        if (base == null) {
            baseLength = 0;
        } else {
            baseLength = base.length();
        }
        
        final String search = mailboxExpression.getCombinedName(HIERARCHY_DELIMITER)
            .replace(freeWildcard, SQL_WILDCARD_CHAR).replace(localWildcard, SQL_WILDCARD_CHAR);
       
        Criteria criteria = new Criteria();
        criteria.add(MailboxRowPeer.NAME,(Object)(search),Criteria.LIKE);
        try {
            List mailboxRows=MailboxRowPeer.doSelect(criteria);
            List listResults=new ArrayList(mailboxRows.size());
            for (Iterator iter = mailboxRows.iterator(); iter.hasNext();) {
                final MailboxRow mailboxRow = (MailboxRow) iter.next();
                final String name = mailboxRow.getName();
                if (name.startsWith(base)) {
                    final String match = name.substring(baseLength);
                    if (mailboxExpression.isExpressionMatch(match, HIERARCHY_DELIMITER)) { 
                        listResults.add(new ListResultImpl(name,"."));
                    }
                }
            }
            return (ListResult[]) listResults.toArray(ListResult.EMPTY_ARRAY);    
        } catch (TorqueException e) {
            throw new MailboxManagerException(e);
        }
        
    }
    
    public void setSubscription(String mailboxName, boolean value) {
        // TODO implement subscriptions
    }
    
    public boolean existsMailbox(String mailboxName) throws MailboxManagerException {
        Criteria c=new Criteria();
        c.add(MailboxRowPeer.NAME,mailboxName);
        CountHelper countHelper=new CountHelper();
        int count;
        try {
            synchronized (getMailboxCache()) {
                count = countHelper.count(c);
                if (count == 0) {
                    getMailboxCache().notFound(mailboxName);
                    return false;
                } else {
                    if (count == 1) {
                        return true;
                    } else {
                        throw new MailboxManagerException("found " + count
                                + " mailboxes");
                    }
                }
            }
        } catch (TorqueException e) {
            throw new MailboxManagerException(e);
        }
    }

    public void close() {
    }

    public void deleteEverything() throws MailboxManagerException {
        try {
            MailboxRowPeer.doDelete(new Criteria().and(MailboxRowPeer.MAILBOX_ID,
                    new Integer(-1), Criteria.GREATER_THAN));
            mailboxCache = new MailboxCache();
        } catch (TorqueException e) {
            throw new MailboxManagerException(e);
        }
    }
    
    protected Log getLog() {
        if (log==null) {
            log=new SimpleLog("TorqueMailboxManager");
        }
        return log;
    }

}
