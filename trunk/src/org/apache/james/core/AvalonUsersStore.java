/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE file.
 */
package org.apache.james.core;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.avalon.AbstractLoggable;
import org.apache.avalon.Component;
import org.apache.avalon.ComponentManager;
import org.apache.avalon.ComponentManagerException;
import org.apache.avalon.Composer;
import org.apache.avalon.Configurable;
import org.apache.avalon.Configuration;
import org.apache.avalon.ConfigurationException;
import org.apache.avalon.Initializable;
import org.apache.james.services.UsersRepository;
import org.apache.james.services.UsersStore;
import org.apache.phoenix.Block;

/**
 *
 * @author <a href="mailto:fede@apache.org">Federico Barbieri</a>
 */
public class AvalonUsersStore 
    extends AbstractLoggable 
    implements Block, Composer, Configurable, UsersStore, Initializable {

    private HashMap repositories;
    protected Configuration          configuration;
    protected ComponentManager       componentManager;

    public void configure( final Configuration configuration )
        throws ConfigurationException {
        this.configuration = configuration;
    }

    public void compose( final ComponentManager componentManager )
        throws ComponentManagerException {
        this.componentManager = componentManager;
    }
      
    public void init() 
        throws Exception {

        getLogger().info("AvalonUsersStore init...");
        repositories = new HashMap();
     
        Iterator repConfs = configuration.getChildren("repository");
        while (repConfs.hasNext()) {
            Configuration repConf = (Configuration) repConfs.next();
            String repName = repConf.getAttribute("name");
            String repClass = repConf.getAttribute("class");

            UsersRepository rep = (UsersRepository) Class.forName(repClass).newInstance();

            setupLogger((Component)rep);

            if (rep instanceof Configurable) {
                ((Configurable) rep).configure(repConf);
            }
            if (rep instanceof Composer) {
                ((Composer) rep).compose( componentManager );
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
