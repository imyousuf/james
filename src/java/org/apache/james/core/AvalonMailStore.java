/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.core;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.james.services.MailRepository;
import org.apache.james.services.MailStore;
import org.apache.james.services.SpoolRepository;

import java.util.HashMap;

/**
 * Provides a registry of mail repositories. A mail repository is uniquely
 * identified by its destinationURL, type and model.
 *
 */
public class AvalonMailStore
    extends AbstractLogEnabled
    implements Contextualizable, Serviceable, Configurable, Initializable, MailStore {

    // Prefix for repository names
    private static final String REPOSITORY_NAME = "Repository";

    // Static variable used to name individual repositories.  Should only
    // be accessed when a lock on the AvalonMailStore.class is held
    private static long id;

    // map of [destinationURL + type]->Repository
    private HashMap repositories;

    // map of [protocol(destinationURL) + type ]->classname of repository;
    private HashMap classes;

    // map of [protocol(destinationURL) + type ]->default config for repository.
    private HashMap defaultConfigs;

    /**
     * The Avalon context used by the instance
     */
    protected Context                context;

    /**
     * The Avalon configuration used by the instance
     */
    protected Configuration          configuration;

    /**
     * The Avalon component manager used by the instance
     */
    protected ServiceManager       m_manager;

    private SpoolRepository inboundSpool;

    /**
     * @see org.apache.avalon.framework.context.Contextualizable#contextualize(Context)
     */
    public void contextualize(final Context context)
            throws ContextException {
        this.context = context;
    }

    /**
     * @see org.apache.avalon.framework.service.Servicable#service(ServiceManager)
     */
    public void service( final ServiceManager manager )
        throws ServiceException
    {
        this.m_manager = manager;
    }


    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure( final Configuration configuration )
        throws ConfigurationException
    {
        this.configuration = configuration;
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
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
        if (getLogger().isInfoEnabled()) {
            getLogger().info("SpoolRepository inboundSpool opened: "
                              + inboundSpool.hashCode());
            getLogger().info("James MailStore ...init");
        }
    }

    /**
     * <p>Registers a new mail repository type in the mail store's
     * registry based upon a passed in <code>Configuration</code> object.</p>
     *
     * <p>This is presumably synchronized to prevent corruption of the
     * internal registry.</p>
     *
     * @param repConf the Configuration object used to register the
     *                repository
     *
     * @throws ConfigurationException if an error occurs accessing the
     *                                Configuration object
     */
    public synchronized void registerRepository(Configuration repConf)
        throws ConfigurationException {
        String className = repConf.getAttribute("class");
        boolean infoEnabled = getLogger().isInfoEnabled();
        Configuration[] protocols
            = repConf.getChild("protocols").getChildren("protocol");
        Configuration[] types = repConf.getChild("types").getChildren("type");
        for ( int i = 0; i < protocols.length; i++ )
        {
            String protocol = protocols[i].getValue();

            // Get the default configuration for these protocol/type combinations.
            Configuration defConf = repConf.getChild("config");

            for ( int j = 0; j < types.length; j++ )
            {
                String type = types[j].getValue();
                String key = protocol + type ;
                if (infoEnabled) {
                    StringBuffer infoBuffer =
                        new StringBuffer(128)
                            .append("Registering Repository instance of class ")
                            .append(className)
                            .append(" to handle ")
                            .append(protocol)
                            .append(" protocol requests for repositories of type ")
                            .append(type);
                    getLogger().info(infoBuffer.toString());
                }
                if (classes.get(key) != null) {
                    throw new ConfigurationException("The combination of protocol and type comprise a unique key for repositories.  This constraint has been violated.  Please check your repository configuration.");
                }
                classes.put(key, className);
                if (defConf != null) {
                    defaultConfigs.put(key, defConf);
                }
            }
        }

    }

    /**
     * This method accept a Configuration object as hint and return the
     * corresponding MailRepository.
     * The Configuration must be in the form of:
     * <repository destinationURL="[URL of this mail repository]"
     *             type="[repository type ex. OBJECT or STREAM or MAIL etc.]"
     *             model="[repository model ex. PERSISTENT or CACHE etc.]">
     *   [addition configuration]
     * </repository>
     *
     * @param hint the Configuration object used to look up the repository
     *
     * @return the selected repository
     *
     * @throws ServiceException if any error occurs while parsing the 
     *                            Configuration or retrieving the 
     *                            MailRepository
     */
    public synchronized Object select(Object hint) throws ServiceException {
        Configuration repConf = null;
        try {
            repConf = (Configuration) hint;
        } catch (ClassCastException cce) {
            throw new ServiceException("",
                "hint is of the wrong type. Must be a Configuration", cce);
        }
        String destination = null;
        String protocol = null;
        try {
            destination = repConf.getAttribute("destinationURL");
            int idx = destination.indexOf(':');
            if ( idx == -1 )
                throw new ServiceException("",
                    "destination is malformed. Must be a valid URL: "
                    + destination);
            protocol = destination.substring(0,idx);
        } catch (ConfigurationException ce) {
            throw new ServiceException("",
                "Malformed configuration has no destinationURL attribute", ce);
        }

        try
        {
            String type = repConf.getAttribute("type");
            String repID = destination + type;
            MailRepository reply = (MailRepository) repositories.get(repID);
            StringBuffer logBuffer = null;
            if (reply != null) {
                if (getLogger().isDebugEnabled()) {
                    logBuffer =
                        new StringBuffer(128)
                                .append("obtained repository: ")
                                .append(repID)
                                .append(",")
                                .append(reply.getClass());
                    getLogger().debug(logBuffer.toString());
                }
                return reply;
            } else {
                String key = protocol + type;
                String repClass = (String) classes.get( key );

                if (getLogger().isDebugEnabled()) {
                    logBuffer =
                        new StringBuffer(128)
                                .append("obtained repository: ")
                                .append(repClass)
                                .append(" to handle: ")
                                .append(protocol)
                                .append(",")
                                .append(type);
                    getLogger().debug( logBuffer.toString() );
                }

                // If default values have been set, create a new repository
                // configuration element using the default values
                // and the values in the selector.
                // If no default values, just use the selector.
                Configuration config;
                Configuration defConf = (Configuration)defaultConfigs.get(key);
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
                    reply = (MailRepository) this.getClass().getClassLoader().loadClass(repClass).newInstance();
                    if (reply instanceof LogEnabled) {
                       setupLogger(reply);
                    }
                    if (reply instanceof Contextualizable) {
                        ((Contextualizable) reply).contextualize(context);
                    }
                    if (reply instanceof Serviceable) {
                        ((Serviceable) reply).service( m_manager );
                    }
                    if (reply instanceof Composable) {
                        final String error = "no implementation in place to support Composable";
                        getLogger().error( error );
                        throw new IllegalArgumentException( error );
                    }
                    if (reply instanceof Configurable) {
                        ((Configurable) reply).configure(config);
                    }
                    if (reply instanceof Initializable) {
                        ((Initializable) reply).initialize();
                    }
                    repositories.put(repID, reply);
                    if (getLogger().isInfoEnabled()) {
                        logBuffer =
                            new StringBuffer(128)
                                .append("added repository: ")
                                .append(repID)
                                .append("->")
                                .append(repClass);
                        getLogger().info(logBuffer.toString());
                    }
                    return reply;
                } catch (Exception e) {
                    if (getLogger().isWarnEnabled()) {
                        getLogger().warn( "Exception while creating repository:" +
                                          e.getMessage(), e );
                    }
                    throw new
                        ServiceException("", "Cannot find or init repository",
                                           e);
                }
            }
        } catch( final ConfigurationException ce ) {
            throw new ServiceException("", "Malformed configuration", ce );
        }
    }

    /**
     * <p>Returns a new name for a repository.</p>
     *
     * <p>Synchronized on the AvalonMailStore.class object to ensure
     * against duplication of the repository name</p>
     *
     * @return a new repository name
     */
    public static final String getName() {
        synchronized (AvalonMailStore.class) {
            return REPOSITORY_NAME + id++;
        }
    }

    /**
     * Returns the mail spool associated with this AvalonMailStore
     *
     * @return the mail spool
     *
     * @throws IllegalStateException if the inbound spool has not
     *                               yet been set
     */
    public SpoolRepository getInboundSpool() {
        if (inboundSpool != null) {
            return inboundSpool;
        } else {
            throw new IllegalStateException("Inbound spool not defined");
        }
    }

    /**
     * Returns whether the mail store has a repository corresponding to
     * the passed in hint.
     *
     * @param hint the Configuration object used to look up the repository
     *
     * @return whether the mail store has a repository corresponding to this hint
     */
    public boolean isSelectable( Object hint ) {
        Object comp = null;
        try {
            comp = select(hint);
        } catch(ServiceException ex) {
            if (getLogger().isErrorEnabled()) {
                getLogger().error("Exception AvalonMailStore.hasComponent-" + ex.toString());
            }
        }
        return (comp != null);
    }

    /**
     * Copies values from one config into another, overwriting duplicate attributes
     * and merging children.
     *
     * @param fromConfig the Configuration to be copied
     * @param toConfig the Configuration to which data is being copied
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

    /**
     * Return the <code>Component</code> when you are finished with it.  In this
     * implementation it does nothing
     *
     * @param component The Component we are releasing.
     */
    public void release(Object component) {}
}
