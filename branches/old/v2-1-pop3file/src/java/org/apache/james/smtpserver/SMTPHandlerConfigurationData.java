/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.smtpserver;

import org.apache.james.services.MailServer;
import org.apache.james.services.UsersRepository;

/**
 * Provides a number of server-wide constant values to the
 * SMTPHandlers
 *
 * @author Peter M. Goldstein <farsight@alum.mit.edu>
 */
public interface SMTPHandlerConfigurationData {

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
     * Returns the service wide maximum message size in bytes.
     *
     * @return the maximum message size
     */
    long getMaxMessageSize();

    /**
     * Returns whether SMTP auth is active for this server.
     *
     * @return whether SMTP authentication is on
     */
    boolean isAuthRequired();

    /**
     * Returns whether the service validates the identity
     * of its senders.
     *
     * @return whether SMTP authentication is on
     */
    boolean isVerifyIdentity();

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
