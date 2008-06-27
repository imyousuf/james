/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport.servlet;

import org.apache.mail.*;

/**
 * Simpliest MailServlet which destroy any incoming messages.
 *
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class Null extends GenericMailServlet {

    public Mail service(Mail mail) {
        log("Destroing mail " + mail.getName());
        return (Mail) null;
    }
    
    public String getServletInfo() {
        return "Null Mail Servlet";
    }
}
    
