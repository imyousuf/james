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
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Matcher;

import javax.mail.MessagingException;
import javax.mail.Address;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * high-level management of spool contents like list, remove, resend
 */
public class SpoolManagement implements Serviceable, SpoolManagementService, SpoolManagementMBean {

    private Store mailStore;

    public void setStore(Store mailStore) {
        this.mailStore = mailStore;
    }

    public void service(ServiceManager serviceManager) throws ServiceException {
        Store mailStore = (Store)serviceManager.lookup("org.apache.avalon.cornerstone.services.store.Store" );
        setStore(mailStore);
    }

    /**
     * Lists all mails from the given repository matching the given filter criteria 
     * @param spoolRepositoryURL the spool whose item are listed
     * @param state if not NULL, only mails with matching state are returned
     * @param header if not NULL, only mails with at least one header with a value matching headerValueRegex are returned
     * @param headerValueRegex the regular expression the header must match
     * @return String array, each line describing one matching mail from the spool 
     * @throws SpoolManagementException
     */
    public String[] listSpoolItems(String spoolRepositoryURL, String state, String header, String headerValueRegex) 
            throws SpoolManagementException {
        return listSpoolItems(spoolRepositoryURL, new SpoolFilter(state, header, headerValueRegex));
    }

    /**
     * Lists all mails from the given repository matching the given filter criteria 
     * @param spoolRepositoryURL the spool whose item are listed
     * @param filter the criteria against which all mails are matched
     * @return String array, each line describing one matching mail from the spool 
     * @throws SpoolManagementException
     */
    public String[] listSpoolItems(String spoolRepositoryURL, SpoolFilter filter) throws SpoolManagementException {
        List spoolItems;
        try {
            spoolItems = getSpoolItems(spoolRepositoryURL, filter);
        } catch (Exception e) {
             throw new SpoolManagementException(e);
        }
        return (String[]) spoolItems.toArray(new String[]{});
    }

    /**
     * @param mail
     * @param filter
     * @return TRUE, if given mail matches all given filter criteria
     * @throws SpoolManagementException
     */
    protected boolean filterMatches(Mail mail, SpoolFilter filter) throws SpoolManagementException {
        if (filter == null || !filter.doFilter()) return true;

        if (filter.doFilterState() && !mail.getState().equalsIgnoreCase(filter.getState())) return false;
        
        if (filter.doFilterHeader()) {

            Perl5Matcher matcher = new Perl5Matcher();
            
            // check, if there is a match for every header/regex pair
            Iterator headers = filter.getHeaders();
            while (headers.hasNext()) {
                String header = (String) headers.next();
                
                String[] headerValues;
                try {
                    headerValues = mail.getMessage().getHeader(header);
                    if (headerValues == null) {
                        // some headers need special retrieval
                        if (header.equalsIgnoreCase("to")) {
                            headerValues = addressesToStrings(mail.getMessage().getRecipients(MimeMessage.RecipientType.TO));
                        }
                        else if (header.equalsIgnoreCase("cc")) { 
                            headerValues = addressesToStrings(mail.getMessage().getRecipients(MimeMessage.RecipientType.CC));
                        }
                        else if (header.equalsIgnoreCase("bcc")) { 
                            headerValues = addressesToStrings(mail.getMessage().getRecipients(MimeMessage.RecipientType.BCC));
                        }
                        else if (header.equalsIgnoreCase("from")) { 
                            headerValues = new String[]{mail.getMessage().getSender().toString()};
                        }
                    }
                } catch (MessagingException e) {
                    throw new SpoolManagementException("could not filter mail by headers", e);
                }
                if (headerValues == null) return false; // no header for this criteria

                Pattern pattern = filter.getHeaderValueRegexCompiled(header);

                // the regex must match at least one entry for the header
                boolean matched = false;
                for (int i = 0; i < headerValues.length; i++) {
                    String headerValue = headerValues[i];
                    if (matcher.matches(headerValue, pattern)) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) return false;
            }
        }
            
        return true;
    }

    private String[] addressesToStrings(Address[] addresses) {
        if (addresses == null) return null;
        if (addresses.length == 0) return new String[]{};
        String[] addressStrings = new String[addresses.length];
        for (int i = 0; i < addresses.length; i++) {
            addressStrings[i] = addresses[i].toString();
        }
        return addressStrings;
    }

    /**
     * @param spoolRepositoryURL
     * @param filter
     * @return List<Mail> all matching mails from the given spool
     * @throws SpoolManagementException
     */
    public List getSpoolItems(String spoolRepositoryURL, SpoolFilter filter)
            throws ServiceException, MessagingException, SpoolManagementException {
        SpoolRepository spoolRepository = getSpoolRepository(spoolRepositoryURL);

        List items = new ArrayList();

        // get an iterator of all keys
        Iterator spoolR = spoolRepository.list();
        while (spoolR.hasNext()) {
            String key = spoolR.next().toString();
            Mail m = spoolRepository.retrieve(key);

            if (filterMatches(m, filter)) {
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

    public int removeSpoolItems(String spoolRepositoryURL, String key, String state, String header, String headerValueRegex) 
            throws SpoolManagementException {
        return removeSpoolItems(spoolRepositoryURL, key, new SpoolFilter(state, header, headerValueRegex));
    }

    /**
     * Removes all mails from the given repository matching the filter 
     * @param spoolRepositoryURL the spool whose item are listed
     * @param key ID of the mail to be removed. if not NULL, all other filters are ignored
     * @param filter the criteria against which all mails are matched. only applied if key is NULL.
     * @return number of removed mails
     * @throws SpoolManagementException
     */
    public int removeSpoolItems(String spoolRepositoryURL, String key, SpoolFilter filter) throws SpoolManagementException {
        try {
            return removeSpoolItems(spoolRepositoryURL, key, null, filter);
        } catch (Exception e) {
            throw new SpoolManagementException(e);
        }
    }

    /**
     * Removes all mails from the given repository matching the filter 
     * @param spoolRepositoryURL the spool whose item are listed
     * @param key ID of the mail to be removed. if not NULL, all other filters are ignored
     * @param lockingFailures is populated with a list of mails which could not be processed because
     * a lock could not be obtained
     * @param filter the criteria against which all mails are matched. only applied if key is NULL.
     * @return number of removed mails
     */
    public int removeSpoolItems(String spoolRepositoryURL, String key, List lockingFailures, SpoolFilter filter) throws ServiceException, MessagingException {
        int count = 0;
        SpoolRepository spoolRepository = getSpoolRepository(spoolRepositoryURL);

        if (key != null) {
            count = removeMail(spoolRepository, key, count, lockingFailures, null);
        } else {
            Iterator spoolR = spoolRepository.list();

            while (spoolR.hasNext()) {
                key = (String)spoolR.next();
                count = removeMail(spoolRepository, key, count, lockingFailures, filter);
            }
        }
        return count;
    }

    private int removeMail(SpoolRepository spoolRepository, String key, int count, List lockingFailures, SpoolFilter filter) throws MessagingException {
        try {
            if (removeMail(spoolRepository, key, filter)) count++;
        } catch (IllegalStateException e) {
            lockingFailures.add(key);
        } catch (SpoolManagementException e) {
            return count;
        }
        return count;
    }

    public int resendSpoolItems(String spoolRepositoryURL, String key, SpoolFilter filter) throws SpoolManagementException {
        try {
            return resendSpoolItems(spoolRepositoryURL, key, null, filter);
        } catch (Exception e) {
            throw new SpoolManagementException(e);
        }
    }

    /**
     * Tries to resend all mails from the given repository matching the given filter criteria 
     * @param spoolRepositoryURL the spool whose item are about to be resend
     * @param key ID of the mail to be resend. if not NULL, all other filters are ignored
     * @param state if not NULL, only mails with matching state are resend
     * @param header if not NULL, only mails with at least one header with a value matching headerValueRegex are resend
     * @param headerValueRegex the regular expression the header must match
     * @return int number of resent mails 
     * @throws SpoolManagementException
     */
    public int resendSpoolItems(String spoolRepositoryURL, String key, String state, String header, String headerValueRegex) throws SpoolManagementException {
        return resendSpoolItems(spoolRepositoryURL, key, new SpoolFilter(state, header, headerValueRegex));
    }

    /**
     * Tries to resend all mails from the given repository matching the given filter criteria 
     * @param spoolRepositoryURL the spool whose item are about to be resend
     * @param key ID of the mail to be resend. if not NULL, all other filters are ignored
     * @param lockingFailures is populated with a list of mails which could not be processed because
     * a lock could not be obtained
     * @param filter the criteria against which all mails are matched. only applied if key is NULL.
     * @return int number of resent mails 
     * @throws SpoolManagementException
     */
    public int resendSpoolItems(String spoolRepositoryURL, String key, List lockingFailures, SpoolFilter filter)
            throws ServiceException, MessagingException, SpoolManagementException {
        int count = 0;
        SpoolRepository spoolRepository = getSpoolRepository(spoolRepositoryURL);

        // check if an key was given as argument
        if (key != null) {
            try {
                if (resendMail(spoolRepository, key, filter)) count++;
            } catch (IllegalStateException e) {
                if (lockingFailures != null) lockingFailures.add(key);
            }
        } else {
            // get an iterator of all keys
            Iterator spoolR = spoolRepository.list();

            while (spoolR.hasNext()) {
                key = spoolR.next().toString();
                try {
                    if (resendMail(spoolRepository, key, filter)) count++;
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
     * @param filter
     * @return true or false
     * @throws MessagingException Get thrown if there happen an error on modify the mail
     */
    private boolean resendMail(SpoolRepository spoolRepository, String key, SpoolFilter filter)
            throws MessagingException, IllegalStateException, SpoolManagementException {
        if (!spoolRepository.lock(key)) throw new IllegalStateException("locking failure");

        // get the mail and set the error_message to "0" that will force the spoolmanager to try to deliver it now!
        Mail m = spoolRepository.retrieve(key);

        if (filterMatches(m, filter)) {

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
     * @param filter
     * @return true or false
     * @throws MessagingException Get thrown if there happen an error on modify the mail
     */
    private boolean removeMail(SpoolRepository spoolRepository, String key, SpoolFilter filter) 
            throws MessagingException, SpoolManagementException {
        if (!spoolRepository.lock(key)) throw new IllegalStateException("locking failure");

        Mail m = spoolRepository.retrieve(key);
        if (m == null) throw new SpoolManagementException("mail not available having key " + key);
        if (!filterMatches(m, filter)) return false;

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
