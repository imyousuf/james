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
 * Simpliest MailServlet which destroy any incoming messages.
 *
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class Null extends AbstractMailet {

    public void service(Mail mail) {
        mail.setState(mail.GHOST);
    }
    
    public String getServletInfo() {
        return "Null Mailet";
    }
}
    
