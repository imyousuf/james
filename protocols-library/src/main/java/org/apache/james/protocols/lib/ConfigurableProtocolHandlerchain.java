package org.apache.james.protocols.lib;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.protocols.api.ProtocolHandlerChain;

public interface ConfigurableProtocolHandlerchain extends ProtocolHandlerChain {

    public void init(HierarchicalConfiguration config) throws ConfigurationException;
}
