/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.pop3server;

import org.apache.james.services.MailServer;
import org.apache.james.services.UsersRepository;

/**
 * Provides a number of server-wide constant values to the
 * POP3Handlers
 *
 * @author Peter M. Goldstein <farsight@alum.mit.edu>
 */
public interface POP3HandlerConfigurationData {

    /**
     * Returns the service wide hello name
     *
     * @return the hello name
     */
    String getHelloName();

    /**
     * Returns the service wide reset length in bytes.
     *
     * @return the reset length
     */
    int getResetLength();

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

}
