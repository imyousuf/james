package org.apache.james.container.spring.configuration;

import org.apache.avalon.framework.configuration.Configuration;

/**
 * interface for modifying configurations
 */
public interface ConfigurationInterceptor {

    /**
     * gets a configuration, inspects and eventually changes it.
     * @param configuration
     * @return changed configuration
     */
    Configuration intercept(Configuration configuration);
}

