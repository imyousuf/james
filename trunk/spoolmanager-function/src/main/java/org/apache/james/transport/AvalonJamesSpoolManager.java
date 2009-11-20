package org.apache.james.transport;

import java.util.List;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.AvalonLogger;
import org.apache.james.api.kernel.LoaderService;
import org.apache.james.services.SpoolManager;
import org.apache.james.services.SpoolRepository;
import org.apache.james.util.ConfigurationAdapter;
import org.apache.mailet.MailetConfig;
import org.apache.mailet.MatcherConfig;
import org.guiceyfruit.jsr250.Jsr250Module;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Provider;
import com.google.inject.name.Names;

public class AvalonJamesSpoolManager implements Initializable, LogEnabled, Serviceable, Configurable, Disposable, SpoolManager{

    private SpoolManager mgmt;
    private ConfigurationAdapter config;
    private AvalonLogger logger;
    private SpoolRepository repos;

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(org.apache.avalon.framework.configuration.Configuration)
     */
    public void configure(Configuration config) throws ConfigurationException {
        try {
            this.config = new ConfigurationAdapter(config);
        } catch (org.apache.commons.configuration.ConfigurationException e) {
            throw new ConfigurationException("Unable to convert configuration", e);
        }
    }

    
    /**
     * @see org.apache.avalon.framework.logger.LogEnabled#enableLogging(org.apache.avalon.framework.logger.Logger)
     */
    public void enableLogging(Logger logger) {
        this.logger = new AvalonLogger(logger);
    }

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager)
     */
    public void service(ServiceManager comp) throws ServiceException {
        repos = (SpoolRepository) comp.lookup(SpoolRepository.ROLE);
    }


    public void dispose() {        
    }

    public List<MailetConfig> getMailetConfigs(String processorName) {
        return mgmt.getMailetConfigs(processorName);
    }

    public List<MatcherConfig> getMatcherConfigs(String processorName) {
        return mgmt.getMatcherConfigs(processorName);
    }

    public String[] getProcessorNames() {
        return mgmt.getProcessorNames();
    }

    protected class Module extends AbstractModule {

        @Override
        protected void configure() {
            bind(org.apache.commons.configuration.HierarchicalConfiguration.class).annotatedWith(Names.named("org.apache.commons.configuration.Configuration")).toInstance(config);
            bind(Log.class).annotatedWith(Names.named("org.apache.commons.logging.Log")).toInstance(logger);
            bind(SpoolRepository.class).annotatedWith(Names.named("org.apache.james.services.SpoolRepository")).toInstance(repos);
            bind(LoaderService.class).annotatedWith(Names.named("org.apache.james.LoaderService")).toProvider(new Provider<LoaderService>() {

                public LoaderService get() {
                    return new MyLoaderService();
                }
                
                class MyLoaderService implements LoaderService{

                    public <T> T load(Class<T> type) {
                        return Guice.createInjector(new Jsr250Module(), new Module()).getInstance(type);
                    }
                    
                }
                
            });
            
           
        }
        
    }

    public void initialize() throws Exception {
        mgmt = Guice.createInjector(new Jsr250Module(), new Module()).getInstance(JamesSpoolManager.class);
    }  
}
