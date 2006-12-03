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


package org.apache.james.mailboxmanager.mailstore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.mailbox.MailboxSession;
import org.apache.james.mailboxmanager.repository.MailRepositoryMailboxSession;
import org.apache.james.services.MailRepository;

public class MailstoreMailboxCache extends AbstractLogEnabled {

    private String mountPoint;

    private String destinationURL;

    private Configuration repositoryConf;

    private Map repositoryCash = new HashMap();

    private Map sessionCash = new HashMap();

    private Store mailStore;

    public MailboxSession getMailboxSession(String mailboxName)
            throws MailboxManagerException {
        String url = buildUrl(mailboxName);
        MailRepository repository = (MailRepository) repositoryCash.get(url);
        if (repository == null) {
            try {
                DefaultConfiguration conf = new DefaultConfiguration(
                        repositoryConf);
                conf.setAttribute("destinationURL", url);
                repository = (MailRepository) getMailStore().select(conf);
                repositoryCash.put(url, repository);
                sessionCash.put(url, new HashSet());
                getLogger().debug("Added MailRepository "+url+ "to the cache");
            } catch (Exception e) {
                getLogger().error("Error optaining repository " + url);
                throw new MailboxManagerException(e);
            }
        } else {
            getLogger().debug("Optained MailRepository "+url+ "from the cache");
        }
        if (repository == null) {
            throw new MailboxManagerException("could not optain repository "
                    + url);
        }
        HashSet sessions = (HashSet) sessionCash.get(url);
        MailRepositoryMailboxSession mailboxSession = new MailRepositoryMailboxSession(
                this, repository, mailboxName);
        ContainerUtil.enableLogging(mailboxSession, getLogger().getChildLogger("session"));
        sessions.add(mailboxSession);

        return mailboxSession;
    }

    public synchronized void releaseSession(MailRepositoryMailboxSession session)
            throws MailboxManagerException {
        String mailboxName = session.getName();
        String url = buildUrl(mailboxName);
        Set sessions = (Set) sessionCash.get(url);
        if (sessions != null && sessions.remove(session)) {
            getLogger().debug("session closed for MailRepository "+url);
            if (sessions.isEmpty()) {
                repositoryCash.remove(url);
                getLogger().debug("MailRepository "+url+ " removed from cache");
            }
        } else {
            throw new MailboxManagerException("session not open");
        }
    }
    
    protected Store getMailStore() {
        return mailStore;
    }

    public void setMailStore(Store mailStore) {
        this.mailStore = mailStore;
    }
    
    String buildUrl(String mailboxName) {
        String url = destinationURL;

        if (!mailboxName.startsWith(mountPoint)) {
            throw new AssertionError("mailboxName does not start with "
                    + mountPoint);
        }
        url += mailboxName.substring(mountPoint.length() + 1);

        // TODO maybe INBOX treatment should only be done when in user
        // namespace
        if (url.toUpperCase().endsWith("INBOX")) {
            url = url.substring(0, url.length() - 5);
        }
        
        if (url.endsWith(".")) {
            url = url.substring(0, url.length() - 1);
        }

        return url;
    }
    
    public void setRepositoryConf(Configuration repositoryConf) {
        this.repositoryConf = repositoryConf;
    }

    public void setDestinationURL(String destinationURL) {
        this.destinationURL = destinationURL;
    }

    public String getMountPoint() {
        return mountPoint;
    }

    public void setMountPoint(String mountPoint) {
        this.mountPoint = mountPoint;
    }
}
