/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.services;

import java.util.Collection;
//import org.xbill.DNS.Record;

/**
 * Provides abstraction for DNS resolutions. The interface is Mail specific.
 * It may be a good idea to make the interface more generic or expose 
 * commonly needed DNS methods.
 * @author  Harmeet <harmeet@kodemuse.com>
 */
public interface DNSServer {
    String ROLE = "org.apache.james.services.DNSServer";

    /** 
     * @return collection of strings representing MX record values. 
     * The returned collection is sorted by priority.
     */
    Collection findMXRecords(String hostname);

    /** @param name : query name, 
     * @param type : Record type. @param type is expected to be one of the 
     * types defined in org.xbill.DNS.Type
     */
    // I thought this was a good general method to expose :-)
    // but it ties the interface to xbill library.:-(
    // the interface should be neutral and implementation replaceable.
    // one way is to have a DNSRecord abstraction...
    //Record[] lookup(String name, short type);
}
