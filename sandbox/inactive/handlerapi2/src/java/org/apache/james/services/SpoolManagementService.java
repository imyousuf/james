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

    public int removeSpoolItems(String spoolRepositoryURL, String key, List lockingFailures, SpoolFilter filter) 
            throws ServiceException, MessagingException;
    
    public int resendSpoolItems(String spoolRepositoryURL, String key, List lockingFailures, SpoolFilter filter) 
            throws ServiceException, MessagingException, SpoolManagementException;

    public List getSpoolItems(String spoolRepositoryURL, SpoolFilter filter) 
            throws ServiceException, MessagingException, SpoolManagementException;
}
