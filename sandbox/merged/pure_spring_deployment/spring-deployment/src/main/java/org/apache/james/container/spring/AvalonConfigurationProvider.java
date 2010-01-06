package org.apache.james.container.spring;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;


public interface AvalonConfigurationProvider {

	public Configuration getAvalonConfigurationForComponent(String name) throws ConfigurationException;
}
