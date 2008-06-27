/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver;

import org.apache.james.nntpserver.repository.NNTPRepository;
import org.apache.james.services.MailServer;
import org.apache.james.services.UsersRepository;

/**
 * Provides a number of server-wide constant values to the
 * NNTPHandlers
 *
 * @author Peter M. Goldstein <farsight@alum.mit.edu>
 */
public interface NNTPHandlerConfigurationData {

    /**
     * Returns the service wide hello name
     *
     * @return the hello name
     */
    String getHelloName();

    /**
     * Returns whether NNTP auth is active for this server.
     *
     * @return whether NNTP authentication is on
     */
    boolean isAuthRequired();

    /**
     * Returns the NNTPRepository used by this service.
     *
     * @return the NNTPRepository used by this service
     */
    NNTPRepository getNNTPRepository();

    /**
     * Returns the UsersRepository for this service.
     *
     * @return the local users repository
     */
    UsersRepository getUsersRepository();

}
