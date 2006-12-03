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

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.SimpleLog;
import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.ListResult;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.Namespace;
import org.apache.james.mailboxmanager.Namespaces;
import org.apache.james.mailboxmanager.impl.ListResultImpl;
import org.apache.james.mailboxmanager.impl.NamespaceImpl;
import org.apache.james.mailboxmanager.impl.NamespacesImpl;
import org.apache.james.mailboxmanager.mailbox.GeneralMailbox;
import org.apache.james.mailboxmanager.mailbox.GeneralMailboxSession;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;
import org.apache.james.mailboxmanager.mailbox.MailboxSession;
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

public class TorqueMailboxManager implements MailboxManager {
    
    public static final char HIERARCHY_DELIMITER='.';
    
    public static final String USER_NAMESPACE="#mail";

    private static Random random;
    private MailboxCache mailboxCache;

    private User authUser;
    
    protected Log log;

    public TorqueMailboxManager(User authUser, MailboxCache mailboxCache, Log log) {
        this.mailboxCache=mailboxCache;
        this.authUser=authUser;
        this.log=log;
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
                    return new ImapMailboxSessionWrapper(new TorqueMailbox(
                            mailboxRow, tracker,getLog()));
                } else {
                    getLog().info("Mailbox '" + mailboxName + "' not found.");
                    getMailboxCache().notFound(mailboxName);
                    throw new MailboxManagerException("Mailbox '" + mailboxName
                            + "' not found.");
                }
            }
        } catch (TorqueException e) {
            throw new MailboxManagerException(e);
        }
    }
    
    private MailboxCache getMailboxCache() {
       return mailboxCache;
    }
    
    public Namespaces getNamespaces(User forUser) {
        NamespacesImpl nameSpaces=new NamespacesImpl();
        nameSpaces.setShared(new Namespace[0]);
        Namespace userNamespace=new NamespaceImpl(""+HIERARCHY_DELIMITER,USER_NAMESPACE);
        nameSpaces.setUser(new Namespace[] {userNamespace}); 
        Namespace personalDefault = getPersonalDefaultNamespace(forUser);
        nameSpaces.setPersonal(new Namespace[] {personalDefault}); 
        nameSpaces.setPersonalDefault(personalDefault);
        return nameSpaces;
    }

    public Namespace getPersonalDefaultNamespace(User forUser) {
        return new NamespaceImpl("" + HIERARCHY_DELIMITER,USER_NAMESPACE+HIERARCHY_DELIMITER+forUser.getUserName());
    }

    public void createMailbox(String namespaceName)
            throws MailboxManagerException {
        getLog().info("createMailbox "+namespaceName);
        synchronized (getMailboxCache()) {
            MailboxRow mr = new MailboxRow();
            mr.setName(namespaceName);
            mr.setLastUid(0);
            mr.setUidValidity(Math.abs(getRandom().nextInt()));
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
        
        MessageResult[] mr=from.getMessages(set, MessageResult.MIME_MESSAGE | MessageResult.INTERNAL_DATE);
        for (int i = 0; i < mr.length; i++) {
            toMailbox.appendMessage(mr[i].getMimeMessage(), mr[i].getInternalDate(), MessageResult.NOTHING);
        }
       
        toMailbox.close();
    }

    public ListResult[] list(String base, String expression, boolean subscribed) throws MailboxManagerException {
        Criteria c=new Criteria();
        if (base.length()>0) {
            if (base.charAt(base.length()-1)==HIERARCHY_DELIMITER) {
                base=base.substring(0, base.length()-1);
            }
            if (expression.length()>0) {
                if (expression.charAt(0)==HIERARCHY_DELIMITER) {
                    expression=base+expression;
                } else {
                    expression=base+HIERARCHY_DELIMITER+expression;
                }
            }
        }
        
        // TODO handling of expressions is limited
        
        expression.replaceAll("\\*","%");
        c.add(MailboxRowPeer.NAME,(Object)(expression),Criteria.LIKE);
        try {
            List mailboxRows=MailboxRowPeer.doSelect(c);
            ListResult[] listResults=new ListResult[mailboxRows.size()];
            int i=0;
            for (Iterator iter = mailboxRows.iterator(); iter.hasNext();) {
                MailboxRow mailboxRow = (MailboxRow) iter.next();
                listResults[i++]=new ListResultImpl(mailboxRow.getName(),".");
            }
            return listResults;    
        } catch (TorqueException e) {
            throw new MailboxManagerException(e);
        }
        
    }
    
    public void setSubscription(String mailboxName, boolean value) {
        // TODO implement subscriptions
    }

    protected static Random getRandom() {
        if (random == null) {
            random = new Random();
        }
        return random;
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
