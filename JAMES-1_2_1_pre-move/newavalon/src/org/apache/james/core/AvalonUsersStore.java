/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE file.
 */
package org.apache.james.core;

import java.util.HashMap;
import java.util.Iterator;
import java.net.URL;
import java.net.MalformedURLException;

import org.apache.avalon.*;
import org.apache.avalon.services.Store;
import org.apache.avalon.blocks.AbstractBlock;

import org.apache.james.services.UsersStore;
import org.apache.james.services.UsersRepository;

import org.apache.log.LogKit;
import org.apache.log.Logger;


/**
 *
 * @author <a href="mailto:fede@apache.org">Federico Barbieri</a>
 */
public class AvalonUsersStore extends AbstractBlock implements UsersStore, Initializable {

    private HashMap repositories;
  
    
    public void init() 
        throws Exception {

        getLogger().info("AvalonUsersStore init...");
        repositories = new HashMap();
     
        Iterator repConfs = m_configuration.getChildren("repository");
        while (repConfs.hasNext()) {
            Configuration repConf = (Configuration) repConfs.next();
	    String repName = repConf.getAttribute("name");
	    String repClass = repConf.getAttribute("class");
	    UsersRepository rep = (UsersRepository) Class.forName(repClass).newInstance();
	    if (rep instanceof Loggable) {
		setupLogger((Component)rep);
	    }
	    if (rep instanceof Configurable) {
		((Configurable) rep).configure(repConf);
	    }
	    if (rep instanceof Composer) {
		((Composer) rep).compose( m_componentManager );
	    }
	    /*
            if (rep instanceof Contextualizable) {
		((Contextualizable) rep).contextualize(context);
	    }
	    */
	    if (rep instanceof Initializable) {
		((Initializable) rep).init();
	    }
	    repositories.put(repName, rep);
	    getLogger().info("UsersRepository " + repName + " started.");
        }
        getLogger().info("AvalonUsersStore ...init");
    }
    

    public UsersRepository getRepository(String request) {
	UsersRepository response = (UsersRepository) repositories.get(request);
	if (response == null) {
	    getLogger().warn("No users repository called: " + request);
	}
	return response;
    }


}
