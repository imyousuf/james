/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.transport.mailets;

import org.apache.mailet.*;

/**
 * Opposite of Null Mailet. It let any incoming mail untouched. Used only for
 * debugging.
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class Identity extends GenericMailet {

    public void service(Mail mail) {
        //Do nothing
    }

    public String getMailetInfo() {
        return "Identity Mailet";
    }
}

