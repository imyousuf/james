/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport.servlet;

import org.apache.james.transport.*;
import java.util.*;
import org.apache.arch.*;
import org.apache.james.*;
import org.apache.avalon.blocks.*;
import org.apache.mail.*;

/**
 * Stores incoming Mail in the specified Repository. 
 * If the "passThrough" in confs is true the mail will be returned untouched in 
 * the pipe. If false will be destroyed.
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class ToRepository extends GenericMailServlet {

    private MailRepository repository;
    private boolean passThrough;
    private String repositoryPath;

    public void init() {
        ComponentManager comp = getComponentManager();
        repositoryPath = getConfiguration("repositoryPath").getValue();
        passThrough = getConfiguration("passThrough", "false").getValueAsBoolean();
        Store store = (Store) comp.getComponent(Interfaces.STORE);
        repository = (MailRepository) store.getPrivateRepository(repositoryPath, MailRepository.MAIL, Store.ASYNCHRONOUS);
    }
    
    public Mail service(Mail mail) {
        log("Storing mail " + mail.getName() + " in " + repositoryPath);
        repository.store(mail);
        if (passThrough) return mail;
        else return (Mail) null;
    }

    public String getServletInfo() {
        return "ToRepository Mail Servlet";
    }
}
    
