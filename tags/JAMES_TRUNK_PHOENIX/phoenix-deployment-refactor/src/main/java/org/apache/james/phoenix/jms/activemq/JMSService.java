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

package org.apache.james.phoenix.jms.activemq;

import java.lang.reflect.InvocationTargetException;

import javax.jms.JMSException;

import org.apache.activemq.broker.BrokerService;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.commons.logging.impl.AvalonLogger;
import org.apache.james.api.jms.MailBuilder;
import org.apache.james.api.jms.MailConsumer;
import org.apache.james.jms.activemq.BrokerManager;
import org.apache.james.jms.builder.SimpleMailBuilder;
import org.apache.james.jms.consumer.SpoolToJamesMailConsumer;
import org.apache.james.services.MailServer;

public class JMSService extends AbstractLogEnabled implements Configurable, Serviceable, Initializable {

    private static final String NAME = "JAMES ActiveMQ JMS ";
    private static final String LOG_MESSAGE_DISABLED = NAME + "is disabled.";
    private static final String LOG_MESSAGE_DISABLED_BY_CONFIGURATION = NAME + "is disabled by configuration.";
    
    private MailServer mailServer;
    private BrokerManager brokerManager;
    
    public void configure(Configuration configuration) throws ConfigurationException {
        final boolean isEnabled = configuration.getAttributeAsBoolean("enabled", true);
        if (isEnabled) {
            BrokerManager brokerManager = configureService(configuration.getChild("activemq-broker"));
            Configuration[] consumers = configuration.getChildren("consumer");
            this.brokerManager = configureConsumers(brokerManager, consumers);
        } else {
            getLogger().info(LOG_MESSAGE_DISABLED_BY_CONFIGURATION);
            brokerManager = null;
        }
    }
        
    private BrokerManager configureConsumers(final BrokerManager broker, 
            final Configuration[] consumers) throws ConfigurationException {
        final BrokerManager result = broker;
        if (broker != null && consumers != null) {
            final int length = consumers.length;
            for (int i=0;i<length;i++) {
                final Configuration configuration = consumers[i];
                final MailConsumer consumer = createConsumer(configuration);
                final Configuration builderConfiguration = configuration.getChild("builder");
                final MailBuilder builder = createBuilder(builderConfiguration);
                final Configuration destination = configuration.getChild("destination");
                if (destination == null) {
                    throw new ConfigurationException("Element 'consumer' must contain one element 'destination'.");
                } else {
                    final Configuration nameConfiguration = destination.getChild("name");
                    if (nameConfiguration == null) {
                        throw new ConfigurationException("Element 'destination' must contain one element 'name'.");
                    } else {    
                        final String name = nameConfiguration.getValue();
                        try {
                            if (destination.getChild("queue") != null) {
                                broker.consumeQueue(consumer, builder, name);
                            } else if (destination.getChild("topic") != null) {
                                broker.subscribeToTopic(consumer, builder, name);
                            } else {
                                throw new ConfigurationException("Element 'destination' must contain either 'topic' or 'queue'.");      
                            }
                        } catch (JMSException e) {
                            throw new ConfigurationException("Cannot connect to destination " + name, e);
                        }
                    }
                }
            }
        }
        return result;
    }
    
    private void setup(final Object subject, final Configuration configuration) throws ConfigurationException {
        if (subject != null) {
            setupLogger(subject);
            if (subject instanceof Configurable && configuration != null) {
                final Configurable configurable = (Configurable) subject;
                configurable.configure(configuration);
            }
        }
    }

    protected void setupLogger(Object subject) {
        super.setupLogger(subject);
        if (!(subject instanceof AbstractLogEnabled)) {
            Class[] commonsLog = {org.apache.commons.logging.Log.class};
            try {
                Object[] args = {new AvalonLogger(getLogger())};
                subject.getClass().getMethod("setLog", commonsLog).invoke(subject, args);
            } catch (SecurityException e) {
                getLogger().debug("Cannot use reflection to determine whether component uses commons logging", e);
            } catch (NoSuchMethodException e) {
                // ok
            } catch (IllegalArgumentException e) {
                getLogger().debug("Failed to set log on" + subject, e);
            } catch (IllegalAccessException e) {
                getLogger().debug("Failed to set log on" + subject, e);
            } catch (InvocationTargetException e) {
                getLogger().debug("Failed to set log on" + subject, e);
            }
        }
    }

    private MailBuilder createBuilder(final Configuration configuration) throws ConfigurationException {
        final MailBuilder result;
        if (configuration == null) {
            throw new ConfigurationException("Element 'consumer' must contain one 'builder' element.");
        } else {
            String type;
            try {
                type = configuration.getAttribute("type");
            } catch (ConfigurationException e) {
                type = null;
            }
            if (type == null || "".equals(type)) {
                final String className = configuration.getAttribute("classname");
                if (className == null || "".equals(className)) {
                    throw new ConfigurationException(
                    "Element 'builder' requires either attribute 'classname' or 'type'.");
                } else {
                    try {
                        result = (MailBuilder) load(className);
                    } catch (ClassCastException e) {
                        throw new ConfigurationException("Class is not a MailConsumer: " + className, e);
                    }
                }
            } else {
                if ("SimpleMailBuilder".equals(type)) {
                    SimpleMailBuilder.JamesIdGenerator idGenerator = new SimpleMailBuilder.JamesIdGenerator(mailServer);
                    result = new SimpleMailBuilder(idGenerator);
                } else {
                    throw new ConfigurationException("Unknown standard type: " +type);
                }
            }
            setup(result, configuration);
        }
        return result;
    }
    
    private MailConsumer createConsumer(final Configuration configuration) throws ConfigurationException {
        String type;
        try {
            type = configuration.getAttribute("type");
        } catch (ConfigurationException e) {
            type = null;
        }
        final MailConsumer consumer;
        if (type == null || "".equals(type)) {
            final String className = configuration.getAttribute("classname");
            if (className == null || "".equals(className)) {
                throw new ConfigurationException(
                        "Element 'consumer' requires either attribute 'classname' or 'type'.");
            } else {
                try {
                    consumer = (MailConsumer) load(className);
                } catch (ClassCastException e) {
                    throw new ConfigurationException("Class is not a MailConsumer: " + className, e);
                }
            }
        } else {
            if ("james-in".equals(type)) {
                consumer = new SpoolToJamesMailConsumer(mailServer, new AvalonLogger(getLogger()));
            } else {
                throw new ConfigurationException("Unknown standard type: " +type);
            }
        }
        setup(consumer, configuration);
        return consumer;
    }
    
    private Object load(String className) throws ConfigurationException {
        final Object result;
        Class clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            getLogger().debug("Trying context classloader", e);
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            try {
                clazz = classLoader.loadClass(className);
            } catch (ClassNotFoundException e1) {
                throw new ConfigurationException("Cannot load type " + className, e);
            }
        }
        
        try {
            result = clazz.newInstance();
        } catch (InstantiationException e) {
            throw new ConfigurationException("Cannot load type " + className, e);
        } catch (IllegalAccessException e) {
            throw new ConfigurationException("Cannot load type " + className, e);
        }
        
        return result;
    }

    private BrokerManager configureService(Configuration configuration) throws ConfigurationException {
        BrokerService broker = new BrokerService();
        configureJmx(configuration, broker);
        configurePersistent(configuration, broker);
        Configuration[] connectors = configuration.getChildren("connector");
        configureConnectors(broker, connectors);
        
        BrokerManager result = new BrokerManager(broker, new AvalonLogger(getLogger()));
        return result;
    }

    private void configurePersistent(Configuration configuration, BrokerService broker) {
        final boolean persistent = configuration.getAttributeAsBoolean("persistent", false);
        broker.setPersistent(persistent);
    }

    private void configureConnectors(BrokerService broker, Configuration[] connectors) throws ConfigurationException {
        if (connectors != null) {
        for (int i=0;i<connectors.length;i++) {
            final String url = connectors[i].getValue();
            try {
                Logger logger = getLogger();
                if (logger.isDebugEnabled()) {
                    logger.debug("Adding connector URL " + url);
                }
                broker.addConnector(url);
            } catch (Exception e) {
                throw new ConfigurationException("Cannot add connection " + url, e);
            }
        } 
        }
    }

    private void configureJmx(Configuration configuration, BrokerService broker) {
        final boolean jmx = configuration.getAttributeAsBoolean("jmx", true);
        broker.setUseJmx(jmx);
    }
    

    public void service(ServiceManager serviceManager) throws ServiceException {
        mailServer = (MailServer) serviceManager.lookup(MailServer.ROLE);
    }

    public void initialize() throws Exception {
        if (brokerManager != null) {
            brokerManager.start();
        } else {
            getLogger().info(LOG_MESSAGE_DISABLED);
        }
    }

    
}
