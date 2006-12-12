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




package org.apache.james.services;

import org.apache.avalon.framework.service.ServiceException;
import org.apache.james.management.SpoolManagementException;
import org.apache.james.management.SpoolFilter;

import javax.mail.MessagingException;
import java.util.List;

public interface SpoolManagementService {
    String ROLE = "org.apache.james.services.SpoolManagementService";

    /**
     * Move all mails from the given repository to another repository matching the given filter criteria
     *
     * @param srcSpoolRepositoryURL the spool whose item are listed
     * @param dstSpoolRepositoryURL the destination spool
     * @param dstState if not NULL, the state will be changed before storing the message to the new repository.
     * @param filter the filter to select messages from the source repository
     * @return a counter of moved mails
     * @throws ServiceException 
     * @throws MessagingException 
     * @throws SpoolManagementException
     */
    public int moveSpoolItems(String srcSpoolRepositoryURL, String dstSpoolRepositoryURL, String dstState, SpoolFilter filter)
            throws ServiceException, MessagingException, SpoolManagementException;

    /**
     * Removes all mails from the given repository matching the filter
     *  
     * @param spoolRepositoryURL the spool whose item are listed
     * @param key ID of the mail to be removed. if not NULL, all other filters are ignored
     * @param lockingFailures is populated with a list of mails which could not be processed because
     * a lock could not be obtained
     * @param filter the criteria against which all mails are matched. only applied if key is NULL.
     * @return number of removed mails
     * @throws ServiceException
     * @throws MessagingException
     */
    public int removeSpoolItems(String spoolRepositoryURL, String key, List lockingFailures, SpoolFilter filter) 
            throws ServiceException, MessagingException;
    
    /**
     * Tries to resend all mails from the given repository matching the given filter criteria 
     * 
     * @param spoolRepositoryURL the spool whose item are about to be resend
     * @param key ID of the mail to be resend. if not NULL, all other filters are ignored
     * @param lockingFailures is populated with a list of mails which could not be processed because
     *                        a lock could not be obtained
     * @param filter the criteria against which all mails are matched. only applied if key is NULL.
     * @return int number of resent mails 
     * @throws ServiceException
     * @throws MessagingException
     * @throws SpoolManagementException
     */
    public int resendSpoolItems(String spoolRepositoryURL, String key, List lockingFailures, SpoolFilter filter) 
            throws ServiceException, MessagingException, SpoolManagementException;

    /**
     * Return a List which contains all mails which can accessed by given spoolRepositoryUrl and matched
     * the given SpoolFilter
     * 
     * @param spoolRepositoryURL the url under which a spool can be accessed
     * @param filter the SpoolFilter to use
     * @return List<Mail> all matching mails from the given spool
     * @throws ServiceException
     * @throws MessagingException
     * @throws SpoolManagementException
     */
    public List getSpoolItems(String spoolRepositoryURL, SpoolFilter filter) 
            throws ServiceException, MessagingException, SpoolManagementException;
}
