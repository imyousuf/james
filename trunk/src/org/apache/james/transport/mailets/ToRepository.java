/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/


package org.apache.james.transport.mailets;

import org.apache.james.transport.*;
import java.util.*;
import org.apache.java.lang.*;
import org.apache.james.*;
import org.apache.avalon.interfaces.*;
import org.apache.mail.*;

/**
 * Stores incoming Mail in the specified Repository. 
 * If the "passThrough" in confs is true the mail will be returned untouched in 
 * the pipe. If false will be destroyed.
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class ToRepository extends AbstractMailet {

    private MailRepository repository;
    private Logger logger;
    private boolean passThrough;
    private String repositoryPath;

    public void init() {
        MailetContext context = getContext();
        ComponentManager comp = context.getComponentManager();
        logger = (Logger) comp.getComponent(Interfaces.LOGGER);
        Configuration conf = context.getConfiguration();
        repositoryPath = conf.getConfiguration("repositoryPath").getValue();
        passThrough = conf.getConfiguration("passThrough", "false").getValueAsBoolean();
        Store store = (Store) comp.getComponent(Interfaces.STORE);
        repository = (MailRepository) store.getPrivateRepository(repositoryPath, MailRepository.MAIL, Store.ASYNCHRONOUS);
    }
    
    public void service(Mail mail) {
        logger.log("Storing mail " + mail.getName() + " in " + repositoryPath);
        repository.store(mail);
        if (!passThrough) mail.setState(Mail.GHOST);
    }

    public String getServletInfo() {
        return "ToRepository Mail Servlet";
    }
}