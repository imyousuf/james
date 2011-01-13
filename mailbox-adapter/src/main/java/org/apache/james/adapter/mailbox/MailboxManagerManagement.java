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
package org.apache.james.adapter.mailbox;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.commons.logging.Log;
import org.apache.james.lifecycle.api.LogEnabled;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxMetaData;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxQuery;
import org.apache.james.mailbox.MailboxSession;

/**
 * JMX managmenent for Mailboxes
 * 
 *
 */
public class MailboxManagerManagement extends StandardMBean implements MailboxManagerManagementMBean, LogEnabled {

    private MailboxManager mailboxManager;
    private Log log;
    
    @Resource(name="mailboxmanager")
    public void setMailboxManager(MailboxManager mailboxManager) {
        this.mailboxManager = mailboxManager;
    }
    
    public MailboxManagerManagement() throws NotCompliantMBeanException {
        super(MailboxManagerManagementMBean.class);
    }
    
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.adapter.mailbox.MailboxManagerManagementMBean#deleteMailboxes(java.lang.String)
     */
    public boolean deleteMailboxes(String username) {
        MailboxSession session = null;
        try {
            session = mailboxManager.createSystemSession(username, log);
            mailboxManager.startProcessingRequest(session);
            List<MailboxMetaData> mList = mailboxManager.search(new MailboxQuery(MailboxPath.inbox(username), "", '*', '%', session.getPathDelimiter()), session);
            for (int i = 0; i < mList.size(); i++) {
                mailboxManager.deleteMailbox(mList.get(i).getPath(),session);
            }
            return true;
        } catch (MailboxException e) {
            log.error("Error while remove mailboxes for user " + username, e);
        } finally {
            if (session != null) {
                mailboxManager.endProcessingRequest(session);
                try {
                    mailboxManager.logout(session, true);
                } catch (MailboxException e) {
                    // ignore here
                }
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.LogEnabled#setLog(org.apache.commons.logging.Log)
     */
    public void setLog(Log log) {
        this.log = log;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.adapter.mailbox.MailboxManagerManagementMBean#listMailboxes(java.lang.String)
     */
    public List<String> listMailboxes(String username) {
        List<String> boxes = new ArrayList<String>();
        MailboxSession session = null;
        try {
            session = mailboxManager.createSystemSession(username, log);
            mailboxManager.startProcessingRequest(session);
            List<MailboxMetaData> mList = mailboxManager.search(new MailboxQuery(MailboxPath.inbox(username), "", '*', '%', session.getPathDelimiter()), session);
            for (int i = 0; i < mList.size(); i++) {
                boxes.add(mList.get(i).getPath().getName());
            }
            Collections.sort(boxes);
        } catch (MailboxException e) {
            log.error("Error list mailboxes for user " + username, e);
        } finally {
            if (session != null) {
                mailboxManager.endProcessingRequest(session);
                try {
                    mailboxManager.logout(session, true);
                } catch (MailboxException e) {
                    // ignore here
                }
            }
        }
        return boxes;
    }
}