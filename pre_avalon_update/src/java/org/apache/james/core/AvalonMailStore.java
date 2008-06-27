/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE file.
 */
package org.apache.james.core;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.phoenix.Block;
import org.apache.james.services.MailRepository;
import org.apache.james.services.MailStore;
import org.apache.james.services.SpoolRepository;

import java.util.HashMap;

/**
 * Provides Registry of mail repositories. A mail repository is uniquely
 * identified
 * by destinationURL, type and model.
 *
 * @author <a href="mailto:fede@apache.org">Federico Barbieri</a>
 * @author Darrell DeBoer <dd@bigdaz.com>
 */
public class AvalonMailStore
    extends AbstractLogEnabled
    implements Block, Contextualizable, Composable, Configurable, Initializable, MailStore {

    private static final String REPOSITORY_NAME = "Repository";
    private static long id;
    // map of [destinationURL + type]->Repository
    private HashMap repositories;

    // map of [protocol(destinationURL) + type ]->classname of repository;
    private HashMap classes;

    // map of [Repository Class]->default config for repository.
    private HashMap defaultConfigs;

    protected Context                context;
    protected Configuration          configuration;
    protected ComponentManager       componentManager;

    private SpoolRepository inboundSpool;

    public void contextualize(final Context context)
            throws ContextException {
        this.context = context;
    }

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
        defaultConfigs = new HashMap();
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

        // Get the default configuration for this Repository class.
        Configuration defConf = repConf.getChild("config");
        if ( defConf != null ) {
            defaultConfigs.put(className, defConf);
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

                // If default values have been set, create a new repository
                // configuration element using the default values
                // and the values in the selector.
                // If no default values, just use the selector.
                Configuration config;
                Configuration defConf = (Configuration)defaultConfigs.get(repClass);
                if ( defConf == null) {
                    config = repConf;
                }
                else {
                    config = new DefaultConfiguration(repConf.getName(),
                                                      repConf.getLocation());
                    copyConfig(defConf, (DefaultConfiguration)config);
                    copyConfig(repConf, (DefaultConfiguration)config);
                }

                try {
                    reply = (MailRepository) Class.forName(repClass).newInstance();
                    if (reply instanceof LogEnabled) {
                       setupLogger(reply);
                    }
                    if (reply instanceof Contextualizable) {
                        ((Contextualizable) reply).contextualize(context);
                    }
                    if (reply instanceof Composable) {
                        ((Composable) reply).compose( componentManager );
                    }
                    if (reply instanceof Configurable) {
                        ((Configurable) reply).configure(config);
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
                    e.printStackTrace();
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

    public boolean hasComponent( Object hint ) {
        Component comp = null;
        try {
            comp = select(hint);
        } catch(ComponentException ex) {
            getLogger().error("Exception AvalonMailStore.hasComponent-"+ex.toString());
        }
        return (comp != null);
    }

    /**
     * Copies values from one config into another, overwriting duplicate attributes
     * and merging children.
     */
    private void copyConfig(Configuration fromConfig, DefaultConfiguration toConfig)
    {
        // Copy attributes
        String[] attrs = fromConfig.getAttributeNames();
        for ( int i = 0; i < attrs.length; i++ ) {
            String attrName = attrs[i];
            String attrValue = fromConfig.getAttribute(attrName, null);
            toConfig.setAttribute(attrName, attrValue);
        }

        // Copy children
        Configuration[] children = fromConfig.getChildren();
        for ( int i = 0; i < children.length; i++ ) {
            Configuration child = children[i];
            String childName = child.getName();
            Configuration existingChild = toConfig.getChild(childName, false);
            if ( existingChild == null ) {
                toConfig.addChild(child);
            }
            else {
                copyConfig(child, (DefaultConfiguration)existingChild);
            }
        }

        // Copy value
        String val = fromConfig.getValue(null);
        if ( val != null ) {
            toConfig.setValue(val);
        }
    }
}
