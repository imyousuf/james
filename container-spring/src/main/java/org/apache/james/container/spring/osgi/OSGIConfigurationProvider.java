package org.apache.james.container.spring.osgi;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

public class OSGIConfigurationProvider implements org.apache.james.container.spring.provider.configuration.ConfigurationProvider{

    @Override
    public void registerConfiguration(String beanName, HierarchicalConfiguration conf) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public HierarchicalConfiguration getConfiguration(String beanName) throws ConfigurationException {
        XMLConfiguration config = new XMLConfiguration();
        config.setDelimiterParsingDisabled(true);
        
        // Don't split attributes which can have bad side-effects with matcher-conditions.
        // See JAMES-1233
        config.setAttributeSplittingDisabled(true);
        
        // Use InputStream so we are not bound to File implementations of the
        // config
        try {
            config.load(new FileInputStream("/tmp/" + beanName + ".xml"));
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("Bean " + beanName);
        }
        
        return config;
    }

}
