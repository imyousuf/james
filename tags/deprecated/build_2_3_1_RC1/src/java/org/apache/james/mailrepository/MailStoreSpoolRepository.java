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

package org.apache.james.mailrepository;

import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.services.SpoolRepository;
import org.apache.mailet.Mail;

import javax.mail.MessagingException;

import java.util.Collection;
import java.util.Iterator;

/**
 * This is a wrapper for the various implementations of SpoolRepositories.
 * This does select the real spool repository via the select method of the
 * provided Store.
 *
 * <p>The select method requires a configuration object with the form:
 *  <br>&lt;spoolrepository destinationURL="file://path-to-root-dir-for-repository"
 *  <br>            type="SPOOL"&gt;
 *  <br>&lt;/spoolrepository&gt;
 *
 * @version This is $Revision: 165416 $
 */
public class MailStoreSpoolRepository
    extends AbstractLogEnabled
    implements Serviceable, Initializable, Configurable, SpoolRepository {

    /**
     * The wrapped spoolRepository
     */
    private SpoolRepository spoolRep;
    
    /**
     * The providing mailStore
     */
    private Store mailStore;

    /**
     * The repository configuration
     */
    private Configuration configuration;
    
    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(org.apache.avalon.framework.service.ServiceManager)
     */
    public void service(ServiceManager serviceManager) throws ServiceException {
        mailStore = (Store) serviceManager.lookup(Store.ROLE);
    }


    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(org.apache.avalon.framework.configuration.Configuration)
     */
    public void configure(Configuration conf) throws ConfigurationException {
        this.configuration = conf;
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {
        try {
            spoolRep  = (SpoolRepository) mailStore.select(configuration);
        } catch (Exception e) {
            getLogger().error("Cannot open private SpoolRepository");
            throw e;
        }
        if (getLogger().isInfoEnabled()) {
            getLogger().info("SpoolRepository opened: "
                      + spoolRep.hashCode());
        }
    }

    /**
     * @see org.apache.james.services.SpoolRepository#accept()
     */
    public Mail accept() throws InterruptedException {
        return spoolRep.accept();
    }

    /**
     * @see org.apache.james.services.SpoolRepository#accept(long)
     */
    public Mail accept(final long delay) throws InterruptedException {
        return spoolRep.accept(delay); 
    }

    /**
     * @see org.apache.james.services.SpoolRepository#accept(org.apache.james.services.SpoolRepository.AcceptFilter)
     */
    public Mail accept(SpoolRepository.AcceptFilter filter) throws InterruptedException {
        return spoolRep.accept(filter);
    }

    /**
     * @see org.apache.james.services.MailRepository#store(org.apache.james.core.MailImpl)
     */
    public void store(Mail mc) throws MessagingException {
        spoolRep.store(mc);
    }

    /**
     * @see org.apache.james.services.MailRepository#list()
     */
    public Iterator list() throws MessagingException {
        return spoolRep.list();
    }

    /**
     * @see org.apache.james.services.MailRepository#retrieve(java.lang.String)
     */
    public Mail retrieve(String key) throws MessagingException {
        return spoolRep.retrieve(key);
    }

    /**
     * @see org.apache.james.services.MailRepository#remove(org.apache.james.core.MailImpl)
     */
    public void remove(Mail mail) throws MessagingException {
        spoolRep.remove(mail);
    }

    /**
     * @see org.apache.james.services.MailRepository#remove(java.util.Collection)
     */
    public void remove(Collection mails) throws MessagingException {
        spoolRep.remove(mails);
    }

    /**
     * @see org.apache.james.services.MailRepository#remove(java.lang.String)
     */
    public void remove(String key) throws MessagingException {
        spoolRep.remove(key);
    }

    /**
     * @see org.apache.james.services.MailRepository#lock(java.lang.String)
     */
    public boolean lock(String key) throws MessagingException {
        return spoolRep.lock(key);
    }

    /**
     * @see org.apache.james.services.MailRepository#unlock(java.lang.String)
     */
    public boolean unlock(String key) throws MessagingException {
        return spoolRep.unlock(key);
    }
    
}
