/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.services;

import java.util.Collection;

/**
 * Provides abstraction for DNS resolutions. The interface is Mail specific.
 * It may be a good idea to make the interface more generic or expose 
 * commonly needed DNS methods.
 *
 * @author  Harmeet <harmeet@kodemuse.com>
 */
public interface DNSServer {

    /**
     * The component role used by components implementing this service
     */
    String ROLE = "org.apache.james.services.DNSServer";

    /**
     * <p>Get a priority-sorted collection of DNS MX records for a given hostname</p>
     *
     * <p>TODO: Change this to a list, as not all collections are sortable</p>
     *
     * @param hostname the hostname to check
     * @return collection of strings representing MX record values. 
     */
    Collection findMXRecords(String hostname);
}
