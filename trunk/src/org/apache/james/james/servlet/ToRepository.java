/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.james.servlet;

import java.io.*;
import java.util.*;
import org.apache.arch.*;
import org.apache.james.*;
import org.apache.avalon.blocks.*;
import org.apache.mail.*;

/**
 * Stores incoming MessageContainer in the specified Repository. 
 * If the "passThrough" in confs is true the mail will be returned untouched in 
 * the pipe. If false will be destroyed.
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class ToRepository extends GenericAvalonMailServlet {

    private ComponentManager comp;
    private MessageContainerRepository repository;
    private String repositoryPath;
    private boolean passThrough;

    public void init() {
        comp = getComponentManager();
        repositoryPath = getConfiguration("repository").getValue();
        passThrough = getConfiguration("passThrough", "false").getValueAsBoolean();
        Store store = (Store) comp.getComponent(Interfaces.STORE);
        repository = (MessageContainerRepository) store.getPrivateRepository(repositoryPath, MessageContainerRepository.MESSAGE_CONTAINER, Store.ASYNCHRONOUS);
    }
    
    public MessageContainer service(MessageContainer mc) {
        log("Storing mail " + mc.getMessageId() + " in " + repositoryPath);
        repository.store(mc.getMessageId(), mc);
        if (passThrough) return mc;
        else return (MessageContainer) null;
    }

    public void destroy() {
    }

    public String getServletInfo() {
        return "ToRepository Mail Servlet";
    }
}
    
