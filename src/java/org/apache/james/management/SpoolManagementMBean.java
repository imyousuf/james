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

/**
 * Expose spool management functionality through JMX.
 * 
 * @phoenix:mx-topic name="SpoolAdministration"
 */
public interface SpoolManagementMBean {

    /**
     * List mails on the spool whose state is ERROR
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description List mails on the spool whose state is ERROR
     *
     * @param spoolRepositoryURL specifies the spool
     * @return number of removed items
     */
    String[] listSpoolItems(String spoolRepositoryURL) throws SpoolManagementException;
    
    /**
     * Removes one specific or all mails from the given spool repository
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description Removes one specific or all mails from the given spool repository
     *
     * @param spoolRepositoryURL specifies the spool
     * @param key identifies the item to be removed. if NULL, all items are removed
     * @return number of removed items
     */
    int removeSpoolItems(String spoolRepositoryURL, String key) throws SpoolManagementException;

    /**
     * (Re-)tries to send one specific or all mails with status ERROR in the given spool repository
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description (Re-)tries to send one specific or all mails with status ERROR in the given spool repository
     *
     * @param spoolRepositoryURL specifies the spool
     * @param key identifies the item to be sent. if NULL, all items with status ERROR are sent
     * @return number of processed items
     */
    int resendSpoolItems(String spoolRepositoryURL, String key) throws SpoolManagementException;

}
