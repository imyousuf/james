package org.apache.james.container.spring;

import javax.annotation.Resource;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.mailrepository.AbstractMailStore;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

public class SpringMailStore extends AbstractMailStore implements BeanFactoryAware{

    private Registry<HierarchicalConfiguration> confRegistry;
    private Registry<Log> logRegistry;
    
    @Resource(name="configurationRegistry")
    public void setConfigurationRegistry(Registry<HierarchicalConfiguration> confRegistry) {
        this.confRegistry = confRegistry;
    }
    
    @Resource(name="logRegistry")
    public void setLogRegistry(Registry<Log> logRegistry) {
        this.logRegistry = logRegistry;
    }
    
    private ConfigurableListableBeanFactory factory;
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailrepository.AbstractMailStore#load(java.lang.String, org.apache.commons.configuration.HierarchicalConfiguration, org.apache.commons.logging.Log)
     */
    protected Object load(String className, HierarchicalConfiguration config, Log log) throws Exception{
        
        // just register it with the classname as key. The createBean method will use the classname as BeanDefinitation name anyway
        confRegistry.registerForComponent(className, config);
        logRegistry.registerForComponent(className, log);
        
        return factory.createBean(factory.getBeanClassLoader().loadClass(className));
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
     */
    public void setBeanFactory(BeanFactory factory) throws BeansException {
        this.factory = (ConfigurableListableBeanFactory) factory;
    }

}
