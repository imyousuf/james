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
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.Loggable;
import org.apache.avalon.framework.logger.AbstractLoggable;
import org.apache.james.services.MailRepository;
import org.apache.james.services.MailStore;
import org.apache.james.services.SpoolRepository;
import org.apache.log.LogKit;
import org.apache.log.Logger;
import org.apache.avalon.phoenix.Block;

/**
 *
 * @author <a href="mailto:fede@apache.org">Federico Barbieri</a>
 *
 * Provides Registry of mail repositories. A mail repository is uniquely
 * identified 
 * by destinationURL, type and model.
 */
public class AvalonMailStore
    extends AbstractLoggable
    implements Block, Composable, Configurable, MailStore, Initializable {

    private static final String REPOSITORY_NAME = "Repository";
    private static long id;
    // map of [destinationURL + type]->Repository
    private HashMap repositories;

    // map of [protocol(destinationURL) + type ]->classname of repository;
    private HashMap classes;
    protected Configuration          configuration;
    protected ComponentManager       componentManager;

    private SpoolRepository inboundSpool;

    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        this.componentManager = componentManager;
    }

    public void configure( final Configuration configuration )
        throws ConfigurationException
    {
        this.configuration = configuration;
    }

    public void initialize()
        throws Exception {

        getLogger().info("JamesMailStore init...");
        repositories = new HashMap();
        classes = new HashMap();
        Configuration[] registeredClasses
            = configuration.getChild("repositories").getChildren("repository");
        for ( int i = 0; i < registeredClasses.length; i++ )
        {
            registerRepository((Configuration) registeredClasses[i]);
        }


        Configuration spoolRepConf
          = configuration.getChild("spoolRepository").getChild("repository");
        try {
           inboundSpool  = (SpoolRepository) select(spoolRepConf);
        } catch (Exception e) {
            getLogger().error("Cannot open private SpoolRepository");
            throw e;
        }
        getLogger().info("SpoolRepository inboundSpool opened: "
                          + inboundSpool.hashCode());
        getLogger().info("James MailStore ...init");
    }

    public synchronized void registerRepository(Configuration repConf)
        throws ConfigurationException {
        String className = repConf.getAttribute("class");
        getLogger().info("Registering Repository " + className);
        Configuration[] protocols
            = repConf.getChild("protocols").getChildren("protocol");
        Configuration[] types = repConf.getChild("types").getChildren("type");
        for ( int i = 0; i < protocols.length; i++ )
        {
            String protocol = protocols[i].getValue();

            for ( int j = 0; j < types.length; j++ )
            {
                String type = types[j].getValue();
		String key = protocol + type ;
                classes.put(key, className);
                getLogger().info("Registered class: " + key+"->"+className);
            }
        }
    }

    public void release(Component component)
    {
    }

    public synchronized Component select(Object hint) throws ComponentException
    {

        Configuration repConf = null;
        try {
            repConf = (Configuration) hint;
        } catch (ClassCastException cce) {
            throw new ComponentException(
                "hint is of the wrong type. Must be a Configuration", cce);
        }
        String destination = null;
        String protocol = null;
        try {
            destination = repConf.getAttribute("destinationURL");
            int idx = destination.indexOf(':');
            if ( idx == -1 )
                throw new ComponentException(
                    "destination is malformed. Must be a valid URL: "
                    + destination);
            protocol = destination.substring(0,idx);
        } catch (ConfigurationException ce) {
            throw new ComponentException(
                "Malformed configuration has no destinationURL attribute", ce);
        }

        try
        {
            String type = repConf.getAttribute("type");
            String repID = destination + type;
            MailRepository reply = (MailRepository) repositories.get(repID);
            if (reply != null) {
                getLogger().debug("obtained repository: " + repID
                                  + "," + reply.getClass());
                return (Component)reply;
            } else {
                String repClass = (String) classes.get( protocol + type );

                getLogger().debug( "Need instance of " + repClass +
                                   " to handle: " + protocol + "," + type  );

                try {
                    reply
                    = (MailRepository) Class.forName(repClass).newInstance();
                   if (reply instanceof Loggable) {
		       setupLogger(reply);
                    }
                    if (reply instanceof Configurable) {
                        ((Configurable) reply).configure(repConf);
                    }
                    if (reply instanceof Composable) {
                        ((Composable) reply).compose( componentManager );
                    }
                    if (reply instanceof Initializable) {
                        ((Initializable) reply).initialize();
                    }
                    repositories.put(repID, reply);
                    getLogger().info("added repository: "+repID+"->"+repClass);
                    return (Component)reply;
                } catch (Exception e) {
                    getLogger().warn( "Exception while creating repository:" +
                                      e.getMessage(), e );

                    throw new
                        ComponentException("Cannot find or init repository",
                                           e);
                }
            }
        } catch( final ConfigurationException ce ) {
            throw new ComponentException( "Malformed configuration", ce );
        }
    }

    public static final String getName() {
        return REPOSITORY_NAME + id++;
    }

    public SpoolRepository getInboundSpool() {
        if (inboundSpool != null) {
            return inboundSpool;
        } else {
            throw new RuntimeException("Inbound spool not defined");
        }
    }

}
