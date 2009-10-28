/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/



package org.apache.james.mailrepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.avalon.cornerstone.services.datasources.DataSourceSelector;
import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.commons.collections.map.ReferenceMap;
import org.apache.commons.configuration.CombinedConfiguration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.services.FileSystem;
import org.guiceyfruit.jsr250.Jsr250Module;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.name.Names;

/**
 * Provides a registry of mail repositories. A mail repository is uniquely
 * identified by its destinationURL, type and model.
 *
 */
public class GuiceMailStore
    implements Store {

    // Prefix for repository names
    private static final String REPOSITORY_NAME = "Repository";

    // Static variable used to name individual repositories.  Should only
    // be accessed when a lock on the AvalonMailStore.class is held
    private static long id;

    // map of [destinationURL + type]->Repository
    @SuppressWarnings("unchecked")
    private Map repositories;

    // map of [protocol(destinationURL) + type ]->classname of repository;
    private Map<String,String> classes;

    // map of [protocol(destinationURL) + type ]->default config for repository.
    private Map<String,HierarchicalConfiguration> defaultConfigs;

    /**
     * The Avalon configuration used by the instance
     */
    private HierarchicalConfiguration          configuration;

    private Log logger;

    private FileSystem fs;

    private DataSourceSelector datasources;
    
    @Resource(name="org.apache.avalon.cornerstone.services.datasources.DataSourceSelector")
    public void setDatasources(DataSourceSelector datasources) {
        this.datasources = datasources;
    }


    @Resource(name="org.apache.commons.logging.Log")
    public void setLogger(Log logger) {
        this.logger = logger;
    }
    
    protected Log getLogger() {
        return logger;
    }
      
    @Resource(name="org.apache.commons.configuration.Configuration")
    public void setConfiguration(HierarchicalConfiguration configuration) {
        this.configuration = configuration;
    }
    
    /**
     * Set the Store to use
     * 
     * @param store the Store
     */
    @Resource(name="org.apache.james.services.FileSystem")
    public void setFileSystem(FileSystem fs) {
        this.fs = fs;
    }


    @PostConstruct
    @SuppressWarnings("unchecked")
    public void init()
        throws Exception {

        getLogger().info("JamesMailStore init...");
        
        repositories = new ReferenceMap();
        classes = new HashMap<String,String>();
        defaultConfigs = new HashMap<String, HierarchicalConfiguration>();
        List<HierarchicalConfiguration> registeredClasses
            = configuration.configurationsAt("repositories.repository");
        for ( int i = 0; i < registeredClasses.size(); i++ )
        {
            registerRepository(registeredClasses.get(i));
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
    @SuppressWarnings("unchecked")
    public synchronized void registerRepository(HierarchicalConfiguration repConf)
        throws ConfigurationException {
        String className = repConf.getString("[@class]");
        boolean infoEnabled = getLogger().isInfoEnabled();
        List<String> protocols = repConf.getList("protocols.protocol");
        List<String >types = repConf.getList("types.type");
        
        for ( int i = 0; i < protocols.size(); i++ )
        {
            String protocol = protocols.get(i);

            HierarchicalConfiguration defConf = null;
            
            if (repConf.getKeys("config").hasNext()) {
                // Get the default configuration for these protocol/type combinations.
                defConf = repConf.configurationAt("config");
            }
            

            for ( int j = 0; j < types.size(); j++ )
            {
                String type = types.get(j);
                String key = protocol + type ;
                if (infoEnabled) {
                    StringBuffer infoBuffer =
                        new StringBuffer(128)
                            .append("Registering Repository instance of class ")
                            .append(className)
                            .append(" to handle ")
                            .append(protocol)
                            .append(" protocol requests for repositories of type ")
                            .append(type)
                            .append(" with key ")
                            .append(key);
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
    @SuppressWarnings("unchecked")
    public synchronized Object select(Object hint) throws ServiceException {
        HierarchicalConfiguration repConf = null;
        try {
            repConf = (HierarchicalConfiguration) hint;
        } catch (ClassCastException cce) {
            throw new ServiceException("",
                "hint is of the wrong type. Must be a Configuration", cce);
        }
        
        String destination = null;
        String protocol = null;

        destination = repConf.getString("[@destinationURL]");
        int idx = destination.indexOf(':');
        if ( idx == -1 )
            throw new ServiceException("",
                "destination is malformed. Must be a valid URL: "
                + destination);
        protocol = destination.substring(0,idx);
        

        String type = repConf.getString("[@type]");
        String repID = destination + type;
        Object reply = repositories.get(repID);
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
                            .append(type)
                            .append(" with key ")
                            .append(key);
                getLogger().debug( logBuffer.toString() );
            }

            // If default values have been set, create a new repository
            // configuration element using the default values
            // and the values in the selector.
            // If no default values, just use the selector.
            final CombinedConfiguration config =  new CombinedConfiguration();
            HierarchicalConfiguration defConf = defaultConfigs.get(key);
            if ( defConf == null) {
                config.addConfiguration(repConf);
            }
            else {
                config.addConfiguration(repConf);
                config.addConfiguration(defConf);
            }

            try {
                Class<?> objectClass = Thread.currentThread().getContextClassLoader().loadClass(repClass);
                reply = Guice.createInjector(new Jsr250Module(), new AbstractModule() {
                        
                    @Override
                    protected void configure() {
                        bind(Log.class).annotatedWith(Names.named("org.apache.commons.logging.Log")).toInstance(logger);
                        bind(HierarchicalConfiguration.class).annotatedWith(Names.named("org.apache.commons.configuration.Configuration")).toInstance(config);
                        bind(FileSystem.class).annotatedWith(Names.named("org.apache.james.services.FileSystem")).toInstance(fs);
                        bind(DataSourceSelector.class).annotatedWith(Names.named("org.apache.avalon.cornerstone.services.datasources.DataSourceSelector")).toInstance(datasources);
                        bind(Store.class).annotatedWith(Names.named("org.apache.avalon.cornerstone.services.store.Store")).toInstance(new Store() {

                            public Object select(Object arg0) throws ServiceException {
                                return GuiceMailStore.this.select(arg0);
                            }

                            public boolean isSelectable(Object arg0) {
                                return GuiceMailStore.this.isSelectable(arg0);                            
                            }

                            public void release(Object arg0) {
                                GuiceMailStore.this.release(arg0);
                            }
                            
                        });
                    }
                }).getInstance(objectClass);

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
        synchronized (GuiceMailStore.class) {
            return REPOSITORY_NAME + id++;
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
     * Return the <code>Component</code> when you are finished with it.  In this
     * implementation it does nothing
     *
     * @param component The Component we are releasing.
     */
    public void release(Object component) {}
}
