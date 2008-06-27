/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.remotemanager;

import org.apache.james.services.MailServer;
import org.apache.james.services.UsersRepository;
import org.apache.james.services.UsersStore;

import java.util.HashMap;

/**
 * Provides a number of server-wide constant values to the
 * RemoteManagerHandlers
 *
 * @author Peter M. Goldstein <farsight@alum.mit.edu>
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

}
