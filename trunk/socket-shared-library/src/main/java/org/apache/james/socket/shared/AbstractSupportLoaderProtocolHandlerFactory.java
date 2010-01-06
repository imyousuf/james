package org.apache.james.socket.shared;

import javax.annotation.Resource;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.api.kernel.LoaderService;
import org.apache.james.api.protocol.ProtocolHandlerChain;

/**
 * Abstract base class which ProtocolHandlerFactory implementations should extend when they use a HandlerChain
 * @author norman
 *
 */
public abstract class AbstractSupportLoaderProtocolHandlerFactory extends AbstractProtocolHandlerFactory{

    
    private HierarchicalConfiguration configuration;
    private ProtocolHandlerChainImpl handlerChain;
    private LoaderService loader;

    /**
     * Gets the current instance loader.
     * @return the loader
     */
    public final LoaderService getLoader() {
        return loader;
    }

    /**
     * Sets the loader to be used for instances.
     * @param loader the loader to set, not null
     */
    @Resource(name="org.apache.james.LoaderService")
    public final void setLoader(LoaderService loader) {
        this.loader = loader;
    }
    
    private void prepareHandlerChain() throws Exception {
    	
        //read from the XML configuration and create and configure each of the handlers
        HierarchicalConfiguration jamesConfiguration = configuration.configurationAt("handler.handlerchain");
        if (jamesConfiguration.getString("[@coreHandlersPackage]") == null)
            jamesConfiguration.addProperty("[@coreHandlersPackage]", getHandlersPackage().getName());
        
    	handlerChain = loader.load(ProtocolHandlerChainImpl.class, getLogger(), jamesConfiguration);
    }

    @Override
    protected void onConfigure(HierarchicalConfiguration config) throws ConfigurationException {
        this.configuration = config;
    }

    
    protected ProtocolHandlerChain getProtocolHandlerChain() {
        return handlerChain;
    }
    
  
    /**
     * Return the class which will get used to load all core handlers
     * 
     * @return class
     */
    protected abstract Class<?> getHandlersPackage();
    
    @Override
    protected void onInit() throws Exception {
        prepareHandlerChain();
    }
    
}
