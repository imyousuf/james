/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.usermanager;

import org.apache.avalon.*;
import org.apache.avalon.blocks.*;
import org.apache.avalon.utils.*;
import org.apache.james.*;
import java.util.*;

/**
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class UserManager implements Component, Configurable, Composer, Service, Contextualizable {

    private Context context;
    private Configuration conf;
    private ComponentManager comp;
    private Logger logger;
    private Store store;
    private UsersRepository rootRepository;

    public void setConfiguration(Configuration conf) {
        this.conf = conf;
    }
    
    public void setContext(Context context) {
        this.context = context;
    }
    
    public void setComponentManager(ComponentManager comp) {
        this.comp = comp;
    }

	public void init() throws Exception {
        logger = (Logger) comp.getComponent(Interfaces.LOGGER);
        String rootPath = conf.getConfiguration("repository").getValue("file://../var/users/");
        store = (Store) comp.getComponent(Interfaces.STORE);
        rootRepository = (UsersRepository) store.getPrivateRepository(rootPath, UsersRepository.USER, Store.ASYNCHRONOUS);
    }
    
    public UsersRepository getUserRepository(String name) {
        String path = rootRepository.getChildDestination(name);
        logger.log("Opening user repositoy " + name + " in " + path, "UserManager", logger.INFO);
        return (UsersRepository) store.getPrivateRepository(path, UsersRepository.USER, Store.ASYNCHRONOUS);
    }

    public void destroy() {
    }
}
    
