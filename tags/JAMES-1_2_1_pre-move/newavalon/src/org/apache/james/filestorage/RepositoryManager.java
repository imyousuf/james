/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE file.
 */
package org.apache.james.filestorage;

import org.apache.avalon.services.Store;
import org.apache.avalon.Configuration;
import org.apache.avalon.ConfigurationException;
import org.apache.avalon.Initializable;
import org.apache.avalon.Composer;
import org.apache.avalon.Configurable;
import org.apache.avalon.Component;
import org.apache.avalon.ComponentNotAccessibleException;
import org.apache.avalon.ComponentNotFoundException;
import org.apache.avalon.Loggable;
import org.apache.avalon.blocks.AbstractBlock;
import org.apache.log.LogKit;
import org.apache.log.Logger;
import java.util.HashMap;
import java.util.Iterator;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * DO NOT USE - only here to deal with classloader problems, use avalon version
 *
 * @author <a href="mailto:fede@apache.org">Federico Barbieri</a>
 */
public class RepositoryManager extends AbstractBlock implements Store, Initializable {

    protected final static boolean       LOG       = true;
    protected final static boolean       DEBUG     = LOG && false;
    //  protected Logger LOGGER = LOG ? LogKit.getLoggerFor("Store") : null;

    private static final String REPOSITORY_NAME = "Repository";
    private static long id;
    private HashMap repositories;
    private HashMap models;
    private HashMap classes;
    
    public void init() 
        throws Exception {

        m_logger.info("James RepositoryManager init...");
        repositories = new HashMap();
        models = new HashMap();
        classes = new HashMap();
        Iterator registeredClasses = m_configuration.getChild("repositories").getChildren("repository");
        while (registeredClasses.hasNext()) {
            registerRepository((Configuration) registeredClasses.next());
        }
        m_logger.info("James RepositoryManager ...init");
    }
    
    public void registerRepository(Configuration repConf) throws ConfigurationException {
        String className = repConf.getAttribute("class");
        m_logger.info("Registering Repository " + className);
        Iterator protocols = repConf.getChild("protocols").getChildren("protocol");
        Iterator types = repConf.getChild("types").getChildren("type");
        Iterator models = repConf.getChild("models").getChildren("model");
        while (protocols.hasNext()) {
            String protocol = ((Configuration) protocols.next()).getValue();
            while (types.hasNext()) {
                String type = ((Configuration) types.next()).getValue();
                while (models.hasNext()) {
                    String model = ((Configuration) models.next()).getValue();
                    classes.put(protocol + type + model, className);
                    m_logger.info("   for " + protocol + "," + type + "," + model);
                }
            }
        }
    }

    public Component select(Object hint) throws ComponentNotFoundException,
        ComponentNotAccessibleException {
        
        Configuration repConf = null;
        try {
            repConf = (Configuration) hint;
        } catch (ClassCastException cce) {
            throw new ComponentNotAccessibleException("hint is of the wrong type. Must be a Configuration", cce);
        }
        URL destination = null;
        try {
            destination = new URL(repConf.getAttribute("destinationURL"));
        } catch (ConfigurationException ce) {
            throw new ComponentNotAccessibleException("Malformed configuration has no destinationURL attribute", ce);
        } catch (MalformedURLException mue) {
            throw new ComponentNotAccessibleException("destination is malformed. Must be a valid URL", mue);
        }

        try
        {
            String type = repConf.getAttribute("type");
            String repID = destination + type;
            Store.Repository reply = (Store.Repository) repositories.get(repID);
            String model = (String) repConf.getAttribute("model");
            if (reply != null) {
                if (models.get(repID).equals(model)) {
                    return reply;
                } else {
                    throw new ComponentNotFoundException("There is already another repository with the same destination and type but with different model");
                }
            } else {
                String protocol = destination.getProtocol();
                String repClass = (String) classes.get( protocol + type + model );

                m_logger.debug( "Need instance of " + repClass + 
                                        " to handle: " + protocol + type + model );

                try {
                    reply = (Store.Repository) Class.forName(repClass).newInstance();
		    if (reply instanceof Loggable) {
			((Loggable) reply).setLogger(m_logger);
		    }
                    if (reply instanceof Configurable) {
                        ((Configurable) reply).configure(repConf);
                    }
                    if (reply instanceof Composer) {
                        ((Composer) reply).compose( m_componentManager );
                    }
/*                if (reply instanceof Contextualizable) {
                  ((Contextualizable) reply).contextualize(context);
                  }*/
                    if (reply instanceof Initializable) {
                        ((Initializable) reply).init();
                    }
                    repositories.put(repID, reply);
                    models.put(repID, model);
                    m_logger.info( "New instance of " + repClass + 
                                           " created for " + destination );
                    return reply;
                } catch (Exception e) {
                    m_logger.warn( "Exception while creating repository:" +
                                           e.getMessage(), e );

                    throw new 
                        ComponentNotAccessibleException( "Cannot find or init repository", e );
                }
	    }
	} catch( final ConfigurationException ce ) {
	    throw new ComponentNotAccessibleException( "Malformed configuration", ce );
	}
    }
        
    public static final String getName() {
	return REPOSITORY_NAME + id++;
    }
}
