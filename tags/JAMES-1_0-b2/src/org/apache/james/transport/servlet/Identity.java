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
 * Opposite of Null Servlet. It let any incoming mail untouched. Used only for 
 * debugging.
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class Identity extends GenericMailServlet {

    public Mail service(Mail mail) {
        log("Untouching mail " + mail.getName());
        return mail;
    }
    
    public String getServletInfo() {
        return "Identity Mail Servlet";
    }
}
    
