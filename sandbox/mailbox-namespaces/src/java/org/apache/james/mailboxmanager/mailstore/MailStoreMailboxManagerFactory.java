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

import java.util.Map;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.manager.MailboxManager;
import org.apache.james.mailboxmanager.manager.MailboxManagerFactory;
import org.apache.james.services.User;

public class MailStoreMailboxManagerFactory extends AbstractLogEnabled implements MailboxManagerFactory  {
    
    private MailstoreMailboxCache mailstoreMailboxCache;

    public void deleteEverything() throws MailboxManagerException {
    }

    public MailboxManager getMailboxManagerInstance(User user) throws MailboxManagerException {
        MailStoreMailboxManager mailboxManager=new MailStoreMailboxManager();
        mailboxManager.setMailstoreMailboxCache(getMailstoreMailboxCache());
        mailboxManager.setUser(user);
        return mailboxManager;
    }
    
    
    public void configure(Configuration conf) throws ConfigurationException {
        getMailstoreMailboxCache().setRepositoryConf(conf.getChild("target").getChild("repository"));
        String destinationURL = conf.getChild("repository").getAttribute(
                "destinationURL");
        getLogger().info("destinationURL:" + destinationURL);
        getMailstoreMailboxCache().setDestinationURL(destinationURL);
    }



    public void addMountPoint(String mountPoint) {
        if (getMailstoreMailboxCache().getMountPoint() == null) {
            getLogger().info("set mount point: " + mountPoint);
            getMailstoreMailboxCache().setMountPoint(mountPoint);
        } else {
            throw new RuntimeException("only one mountpoint supported");
        }
    }
    
    protected MailstoreMailboxCache getMailstoreMailboxCache() {
        if (mailstoreMailboxCache == null) {
            mailstoreMailboxCache = new MailstoreMailboxCache();
            ContainerUtil.enableLogging(mailstoreMailboxCache, getLogger());
        }
        return mailstoreMailboxCache;
    }

    public Map getOpenMailboxSessionCountMap() {
        // TODO Auto-generated method stub
        return null;
    }
}
