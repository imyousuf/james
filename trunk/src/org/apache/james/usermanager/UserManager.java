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
    private String type;
 
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
	    type = conf.getConfiguration("type").getValue();
	    String rootPath = conf.getConfiguration("repository").getValue();

	    if (type.equals("file")) {
		store = (Store) comp.getComponent(Interfaces.STORE);
		rootRepository = (UsersRepository) store.getPrivateRepository(rootPath, UsersRepository.USER, Store.ASYNCHRONOUS);

	    } else if (type.equals("ldap")) {
		UsersLDAPRepository LDAPrep = new UsersLDAPRepository();
		try {
		    LDAPrep.setConfiguration(conf.getConfiguration("LDAPRepository"));
		    LDAPrep.setContext(context);
		    LDAPrep.setComponentManager(comp);
		    LDAPrep.setServerRoot();
		    LDAPrep.init();
		    rootRepository = (UsersRepository) LDAPrep;
		} catch (Exception e) {
		    logger.log("Exception in UsersLDAPRepository init: " + e.getMessage(), "UserManager", logger.ERROR);
		    throw e;
		}
	    
	    } else {
		logger.log("Unknown user repository type in conf.xml", "UserManager", logger.ERROR);
	    }
	
	}
    
    public UsersRepository getUserRepository(String name) {

	UsersRepository thisRepository = null;

	if (type.equals("file")) {
	    String path = rootRepository.getChildDestination(name);
	    logger.log("Opening user repositoy " + name + " in " + path, "UserManager", logger.INFO);
	    thisRepository = (UsersRepository) store.getPrivateRepository(path, UsersRepository.USER, Store.ASYNCHRONOUS);

	} else if (type.equals("ldap")) {
	    if (name.equals("root")) {
	    thisRepository = rootRepository;

	    } else if (name.equals("LocalUsers")|| name.startsWith("list-")) {
		String newBase = rootRepository.getChildDestination(name);
		UsersLDAPRepository newRep = new UsersLDAPRepository();
		try {
		    newRep.setConfiguration(conf.getConfiguration("LDAPRepository"));
		    newRep.setContext(context);
		    newRep.setComponentManager(comp);
		    newRep.setBase(newBase);
		    newRep.init();
		    thisRepository = (UsersRepository) newRep;
		} catch (Exception e) {
		    logger.log("Exception in getUserRepository (LDAP): " + e.getMessage(), "UserManager", logger.ERROR);
		   
		}
	    }
	}
	return thisRepository;
    }


    public void destroy() {
    }
}
    
