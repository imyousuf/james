package org.apache.james.container.spring.examples.configuration;

import org.apache.james.container.spring.configuration.ConfigurationInterceptor;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.MutableConfiguration;

/**
 * traverses over all configurations in the configuration tree
 */
public abstract class TraversingConfigurationInterceptor implements ConfigurationInterceptor {
    public Configuration intercept(Configuration configuration) {
        interceptInternal(configuration);
        return configuration;
    }

    private void interceptInternal(Configuration configuration) {
        if (configuration instanceof MutableConfiguration) {
            MutableConfiguration mutableConfiguration = (MutableConfiguration) configuration;
            process(mutableConfiguration);
        }

        // go deep.
        Configuration[] children = configuration.getChildren();
        for (int i = 0; i < children.length; i++) {
            Configuration childConfiguration = children[i];
            interceptInternal(childConfiguration);
        }
    }

    protected abstract void process(MutableConfiguration mutableConfiguration);
}
