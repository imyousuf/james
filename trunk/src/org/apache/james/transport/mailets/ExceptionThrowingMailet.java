/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport.mailets;

import org.apache.mail.*;
import org.apache.james.transport.*;

/**
 * Debugging purpose Mailet. Just throws an exception.
 *
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class ExceptionThrowingMailet extends AbstractMailet {

    public void service(Mail mail) 
    throws Exception {
        throw new Exception("General protection fault");
    }
    
    public String getServletInfo() {
        return "ExceptionThrowingMailet Mailet";
    }
}
    
