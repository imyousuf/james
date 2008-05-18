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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.SimpleLog;
import org.apache.james.mailboxmanager.MessageRange;
import org.apache.james.mailboxmanager.ListResult;
import org.apache.james.mailboxmanager.MailboxExistsException;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MailboxNotFoundException;
import org.apache.james.mailboxmanager.MailboxSession;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.MessageResult.FetchGroup;
import org.apache.james.mailboxmanager.impl.FetchGroupImpl;
import org.apache.james.mailboxmanager.impl.ListResultImpl;
import org.apache.james.mailboxmanager.mailbox.Mailbox;
import org.apache.james.mailboxmanager.manager.MailboxExpression;
import org.apache.james.mailboxmanager.manager.MailboxManager;
import org.apache.james.mailboxmanager.torque.om.MailboxRow;
import org.apache.james.mailboxmanager.torque.om.MailboxRowPeer;
import org.apache.torque.TorqueException;
import org.apache.torque.util.CountHelper;
import org.apache.torque.util.Criteria;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;

public class TorqueMailboxManager implements MailboxManager {

    private static final FetchGroupImpl FROM_FETCH_GROUP = new FetchGroupImpl(FetchGroup.MIME_MESSAGE | FetchGroup.INTERNAL_DATE);
    private static final char SQL_WILDCARD_CHAR = '%';
    private final static Random random = new Random();
    
    protected Log log = LogFactory.getLog(TorqueMailboxManager.class);

    private final ReadWriteLock lock;
    
    private final Map managers;
    
    public TorqueMailboxManager() {
        this.lock =  new WriterPreferenceReadWriteLock();
        managers = new HashMap();
    }
    
    public Mailbox getMailbox(String mailboxName, boolean autoCreate)
            throws MailboxManagerException {
        if (autoCreate && !existsMailbox(mailboxName)) {
            getLog().info("autocreated mailbox  " + mailboxName);
            createMailbox(mailboxName);
        }
        try {
            synchronized (managers) {
                MailboxRow mailboxRow = MailboxRowPeer
                        .retrieveByName(mailboxName);

                if (mailboxRow != null) {
                    getLog().debug("Loaded mailbox "+mailboxName);
                    
                    Mailbox torqueMailbox = (Mailbox) managers.get(mailboxName);
                    if (torqueMailbox == null) {
                        torqueMailbox = new TorqueMailbox(mailboxRow, lock, getLog());
                        managers.put(mailboxName, torqueMailbox);
                    }
                    
                    return torqueMailbox;
                } else {
                    getLog().info("Mailbox '" + mailboxName + "' not found.");
                    throw new MailboxNotFoundException(mailboxName);
                }
            }
        } catch (TorqueException e) {
            throw new MailboxManagerException(e);
        }
    }
    
    public void createMailbox(String namespaceName)
            throws MailboxManagerException {
        getLog().debug("createMailbox "+namespaceName);
        final int length = namespaceName.length();
        if (length == 0) {
            getLog().warn("Ignoring mailbox with empty name");
        } else if (namespaceName.charAt(length - 1) == HIERARCHY_DELIMITER) {
            createMailbox(namespaceName.substring(0, length - 1));
        } else {
            synchronized (managers) {
                // Create root first
                // If any creation fails then mailbox will not be created
                // TODO: transaction
                int index = namespaceName.indexOf(HIERARCHY_DELIMITER);
                int count = 0;
                while (index>=0) {
                    // Until explicit namespace support is added,
                    // this workaround prevents the namespaced elements being created
                    // TODO: add explicit support for namespaces
                    if (index > 0 && count++ > 1) {
                        final String mailbox = namespaceName.substring(0, index);
                        if (!existsMailbox(mailbox)) {
                            doCreate(mailbox);
                        }
                    }
                    index = namespaceName.indexOf(HIERARCHY_DELIMITER, ++index);
                }
                doCreate(namespaceName);
            }
        }
    }

    private void doCreate(String namespaceName) throws MailboxManagerException {
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

    public void deleteMailbox(String mailboxName)
            throws MailboxManagerException {
        getLog().info("deleteMailbox "+mailboxName);
        synchronized (managers) {
            try {
                // TODO put this into a serilizable transaction
                MailboxRow mr = MailboxRowPeer.retrieveByName(mailboxName);
                if (mr == null) {
                    throw new MailboxNotFoundException("Mailbox not found");
                }
                MailboxRowPeer.doDelete(mr);
                managers.remove(mailboxName);
            } catch (TorqueException e) {
                throw new MailboxManagerException(e);
            }
        }
    }

    public void renameMailbox(String from, String to)
    throws MailboxManagerException {
        getLog().debug("renameMailbox "+from+" to "+to);
        try {
            synchronized (managers) {
                if (existsMailbox(to)) {
                    throw new MailboxExistsException(to);
                }
                // TODO put this into a serilizable transaction
                final MailboxRow mr;

                mr = MailboxRowPeer.retrieveByName(from);

                if (mr == null) {
                    throw new MailboxNotFoundException(from);
                }
                mr.setName(to);
                mr.save();

                managers.remove(from);

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
                }
            }
        } catch (TorqueException e) {
            throw new MailboxManagerException(e);
        }
    }

    public void copyMessages(MessageRange set, String from, String to, MailboxSession session) throws MailboxManagerException {
        Mailbox toMailbox= getMailbox(to, false);
        Mailbox fromMailbox = getMailbox(from, false);
        Iterator it = fromMailbox.getMessages(set, FROM_FETCH_GROUP, session);
        while (it.hasNext()) {
            final MessageResult result = (MessageResult) it.next();
            final MimeMessage mimeMessage = result.getMimeMessage();
            toMailbox.appendMessage(mimeMessage, result.getInternalDate(), FetchGroupImpl.MINIMAL, session);
        }
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
            synchronized (managers) {
                count = countHelper.count(c);
                if (count == 0) {
                    managers.remove(mailboxName);
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

    public void deleteEverything() throws MailboxManagerException {
        try {
            MailboxRowPeer.doDelete(new Criteria().and(MailboxRowPeer.MAILBOX_ID,
                    new Integer(-1), Criteria.GREATER_THAN));
            managers.clear();
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

    public MailboxSession createSession() {
        return new TorqueMailboxSession(random.nextLong());
    }

    public String resolve(final String userName, String mailboxPath) {
        if (mailboxPath.charAt(0) != HIERARCHY_DELIMITER) {
            mailboxPath = HIERARCHY_DELIMITER + mailboxPath ;
        } 
        final String result = USER_NAMESPACE + HIERARCHY_DELIMITER + userName + mailboxPath;
        return result;
    }

}
