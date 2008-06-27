/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.usermanager;

import org.apache.arch.*;
import org.apache.avalon.blocks.*;
import org.apache.java.util.*;
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
    private Store.ObjectRepository users;

    public UserManager() {
    }

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
        String userRepositoryPath = conf.getConfiguration("repository", "file://../users/").getValue();
        Store store = (Store) comp.getComponent(Interfaces.STORE);
        users = (Store.ObjectRepository) store.getPrivateRepository(userRepositoryPath, Store.OBJECT, Store.ASYNCHRONOUS);
    }
    
    public Enumeration list() {
      return users.list();  
    }
    
    public void remove(String login) {
        users.remove(login);
    }
    
    public void addUser(String login, String password) {
        users.store(login, password);
    }

    public boolean test(String login, String password) {
        return users.test(login, password);
    }
    
    public boolean containsKey(String login) {
        return users.containsKey(login);
    }
    
    public void destroy() {
    }
}
    
