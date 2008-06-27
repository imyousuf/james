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

package org.apache.james.remotemanager;

import org.apache.james.services.MailServer;
import org.apache.james.services.UsersRepository;
import org.apache.james.services.UsersStore;

import java.util.HashMap;

/**
 * Provides a number of server-wide constant values to the
 * RemoteManagerHandlers
 *
 */
public interface RemoteManagerHandlerConfigurationData {

    /**
     * Returns the service wide hello name
     *
     * @return the hello name
     */
    String getHelloName();

    /**
     * Returns the MailServer interface for this service.
     *
     * @return the MailServer interface for this service
     */
    MailServer getMailServer();

    /**
     * Returns the UsersRepository for this service.
     *
     * @return the local users repository
     */
    UsersRepository getUsersRepository();

    /**
     * Returns the UsersStore for this service.
     *
     * @return the local users store
     */
    UsersStore getUserStore();

    /**
     * Returns the Administrative Account Data
     *
     * TODO: Change the return type to make this immutable.
     *
     * @return the admin account data
     */
    HashMap getAdministrativeAccountData();

    /**
     * Returns the prompt to be displayed when waiting for input. e.g. "james> ".
     * 
     * @return the configured prompt, or an empty string when the prompt is not configured.
     */
    String getPrompt();

}
