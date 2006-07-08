/***********************************************************************
 * Copyright (c) 2006 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/


package org.apache.james.management;

import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.services.SpoolManagementService;
import org.apache.james.services.SpoolRepository;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import javax.mail.MessagingException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

public class SpoolManagement implements Serviceable, SpoolManagementService, SpoolManagementMBean {

    private Store mailStore;

    public void setStore(Store mailStore) {
        this.mailStore = mailStore;
    }

    public void service(ServiceManager serviceManager) throws ServiceException {
        Store mailStore = (Store)serviceManager.lookup("org.apache.avalon.cornerstone.services.store.Store" );
        setStore(mailStore);
    }

    public String[] listSpoolItems(String spoolRepositoryURL) throws SpoolManagementException {
        List spoolItems;
        try {
            spoolItems = getSpoolItems(spoolRepositoryURL);
        } catch (Exception e) {
             throw new SpoolManagementException(e);
        }
        return (String[]) spoolItems.toArray(new String[]{});
    }

    public List getSpoolItems(String spoolRepositoryURL) throws ServiceException, MessagingException {
        SpoolRepository spoolRepository = getSpoolRepository(spoolRepositoryURL);

        List items = new ArrayList();

        // get an iterator of all keys
        Iterator spoolR = spoolRepository.list();
        while (spoolR.hasNext()) {
            String key = spoolR.next().toString();
            Mail m = spoolRepository.retrieve(key);

            // Only show email if its in error state.
            if (m.getState().equals(Mail.ERROR)) {
                StringBuffer itemInfo = new StringBuffer();
                itemInfo.append("key: ").append(key).append(" sender: ").append(m.getSender()).append(" recipient:");
                Collection recipients = m.getRecipients();
                for (Iterator iterator = recipients.iterator(); iterator.hasNext();) {
                    MailAddress mailAddress = (MailAddress) iterator.next();
                    itemInfo.append(" ").append(mailAddress);
                }
                items.add(itemInfo.toString());
            }
        }

        return items;
    }

    public int removeSpoolItems(String spoolRepositoryURL, String key) throws SpoolManagementException {
        try {
            return removeSpoolItems(spoolRepositoryURL, key, null);
        } catch (Exception e) {
            throw new SpoolManagementException(e);
        }
    }

    public int removeSpoolItems(String spoolRepositoryURL, String key, List lockingFailures) throws ServiceException, MessagingException {
        int count = 0;
        SpoolRepository spoolRepository = getSpoolRepository(spoolRepositoryURL);

        if (key != null) {
            count = removeMail(spoolRepository, key, count, lockingFailures);
        } else {
            Iterator spoolR = spoolRepository.list();

            while (spoolR.hasNext()) {
                key = (String)spoolR.next();
                count = removeMail(spoolRepository, key, count, lockingFailures);
            }
        }
        return count;
    }

    private int removeMail(SpoolRepository spoolRepository, String key, int count, List lockingFailures) throws MessagingException {
        try {
            if (removeMail(spoolRepository, key)) count++;
        } catch (IllegalStateException e) {
            lockingFailures.add(key);
        } catch (SpoolManagementException e) {
            return count;
        }
        return count;
    }

    public int resendSpoolItems(String spoolRepositoryURL, String key) throws SpoolManagementException {
        try {
            return resendSpoolItems(spoolRepositoryURL, key, null);
        } catch (Exception e) {
            throw new SpoolManagementException(e);
        }
    }

    public int resendSpoolItems(String spoolRepositoryURL, String key, List lockingFailures) throws ServiceException, MessagingException {
        int count = 0;
        SpoolRepository spoolRepository = getSpoolRepository(spoolRepositoryURL);

        // check if an key was given as argument
        if (key != null) {
            try {
                if (resendErrorMail(spoolRepository, key)) count++;
            } catch (IllegalStateException e) {
                if (lockingFailures != null) lockingFailures.add(key);
            }
        } else {
            // get an iterator of all keys
            Iterator spoolR = spoolRepository.list();

            while (spoolR.hasNext()) {
                key = spoolR.next().toString();
                try {
                    if (resendErrorMail(spoolRepository, key)) count++;
                } catch (IllegalStateException e) {
                    if (lockingFailures != null) lockingFailures.add(key);
                }
            }
        }
        return count;
    }

    /**
     * Resent the mail that belongs to the given key and spoolRepository 
     * 
     * @param spoolRepository The spoolRepository
     * @param key The message key
     * @return true orf false
     * @throws MessagingException Get thrown if there happen an error on modify the mail
     */
    private boolean resendErrorMail(SpoolRepository spoolRepository, String key)
            throws MessagingException, IllegalStateException {
        if (!spoolRepository.lock(key)) throw new IllegalStateException("locking failure");

        // get the mail and set the error_message to "0" that will force the spoolmanager to try to deliver it now!
        Mail m = spoolRepository.retrieve(key);

        if (m.getState().equals(Mail.ERROR)) {

            // this will force Remotedelivery to try deliver the mail now!
            m.setLastUpdated(new Date(0));

            // store changes
            spoolRepository.store(m);
            spoolRepository.unlock(key);

            synchronized (spoolRepository) {
                spoolRepository.notify();
            }
            return true;
        } else {
            spoolRepository.unlock(key);
            return false;
        }
    }

    /**
     * Remove the mail that belongs to the given key and spoolRepository 
     * @param spoolRepository The spoolRepository
     * @param key The message key
     * @return true or false
     * @throws MessagingException Get thrown if there happen an error on modify the mail
     */
    private boolean removeMail(SpoolRepository spoolRepository, String key) throws MessagingException, SpoolManagementException {
        if (!spoolRepository.lock(key)) throw new IllegalStateException("locking failure");

        Mail m = spoolRepository.retrieve(key);
        if (m == null) throw new SpoolManagementException("mail not available having key " + key);
        if (!m.getState().equals(Mail.ERROR)) return false;

        spoolRepository.remove(key);
        return true;
    }

    /**
     * Retrieve a spoolRepository by the given url
     * 
     * @param url The spoolRepository url
     * @return The spoolRepository
     * @throws ServiceException Get thrown if the spoolRepository can not retrieved
     */
    private SpoolRepository getSpoolRepository(String url)
            throws ServiceException {
        // Setup all needed data
        DefaultConfiguration spoolConf = new DefaultConfiguration("spool",
                "generated:RemoteManager.java");
        spoolConf.setAttribute("destinationURL", url);
        spoolConf.setAttribute("type", "SPOOL");

        return (SpoolRepository) mailStore.select(spoolConf);
    }


}
