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



package org.apache.james.container.spring;


import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.container.spring.Registry.RegistryException;
import org.apache.james.transport.LinearProcessor;
import org.apache.james.transport.MailProcessor;
import org.apache.james.transport.ProcessorList;
import org.apache.mailet.Mail;
import org.apache.mailet.MailetException;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * This class is responsible for creating a set of named processors and
 * directing messages to the appropriate processor (given the State of the mail)
 *
 */
public class StateAwareProcessorList implements MailProcessor, ProcessorList, BeanFactoryPostProcessor, BeanNameAware, ApplicationContextAware {

    /**
     * The map of processor names to processors
     */
    private final List<String> processors;

    private Log logger;

    private Registry<Log> logRegistry;

    private Registry<HierarchicalConfiguration> confRegistry;

    private String name;

    private ApplicationContext context;
    
    public StateAwareProcessorList() {
        super();
        this.processors = new ArrayList<String>();
    }



    public void setLogRegistry(Registry<Log> logRegistry) {
        this.logRegistry = logRegistry;
    }
    

    public void setConfigurationRegistry(Registry<HierarchicalConfiguration> confRegistry) {
        this.confRegistry = confRegistry;
    }
    
    
    /**
     * Process this mail message by the appropriate processor as designated
     * in the state of the Mail object.
     *
     * @param mail the mail message to be processed
     *
     * @see org.apache.james.transport.MailProcessor#service(org.apache.mailet.Mail)
     */
    public void service(Mail mail) {
        while (true) {
            String processorName = mail.getState();
            if (processorName.equals(Mail.GHOST)) {
                //This message should disappear
                return;
            }
            try {
                MailProcessor processor
                    = (MailProcessor)context.getBean(processorName);
                if (processor == null) {
                    StringBuffer exceptionMessageBuffer =
                        new StringBuffer(128)
                            .append("Unable to find processor ")
                            .append(processorName)
                            .append(" requested for processing of ")
                            .append(mail.getName());
                    String exceptionMessage = exceptionMessageBuffer.toString();
                    logger.debug(exceptionMessage);
                    mail.setState(Mail.ERROR);
                    throw new MailetException(exceptionMessage);
                }
                StringBuffer logMessageBuffer = null;
                if (logger.isDebugEnabled()) {
                    logMessageBuffer =
                        new StringBuffer(64)
                                .append("Processing ")
                                .append(mail.getName())
                                .append(" through ")
                                .append(processorName);
                    logger.debug(logMessageBuffer.toString());
                }
                processor.service(mail);
                if (logger.isDebugEnabled()) {
                    logMessageBuffer =
                        new StringBuffer(128)
                                .append("Processed ")
                                .append(mail.getName())
                                .append(" through ")
                                .append(processorName);
                    logger.debug(logMessageBuffer.toString());
                    logger.debug("Result was " + mail.getState());
                }
                return;
            } catch (Throwable e) {
                // This is a strange error situation that shouldn't ordinarily
                // happen
                StringBuffer exceptionBuffer = 
                    new StringBuffer(64)
                            .append("Exception in processor <")
                            .append(processorName)
                            .append(">");
                logger.error(exceptionBuffer.toString(), e);
                if (processorName.equals(Mail.ERROR)) {
                    // We got an error on the error processor...
                    // kill the message
                    mail.setState(Mail.GHOST);
                    mail.setErrorMessage(e.getMessage());
                } else {
                    //We got an error... send it to the requested processor
                    if (!(e instanceof MessagingException)) {
                        //We got an error... send it to the error processor
                        mail.setState(Mail.ERROR);
                    }
                    mail.setErrorMessage(e.getMessage());
                }
            }
            if (logger.isErrorEnabled()) {
                StringBuffer logMessageBuffer =
                    new StringBuffer(128)
                            .append("An error occurred processing ")
                            .append(mail.getName())
                            .append(" through ")
                            .append(processorName);
                logger.error(logMessageBuffer.toString());
                logger.error("Result was " + mail.getState());
            }
        }
    }

    /**
     * @return names of all configured processors
     */
    public String[] getProcessorNames() {
        return (String[]) processors.toArray(new String[]{});
    }

    public MailProcessor getProcessor(String name) {
        return (MailProcessor) context.getBean(name);
    }


    /*
     * (non-Javadoc)
     * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor#postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)
     */
    @SuppressWarnings("unchecked")
    public void postProcessBeanFactory(ConfigurableListableBeanFactory factory) throws BeansException {

        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) factory;
        try {
            List<HierarchicalConfiguration> processorConfs = confRegistry.getForComponent(name).configurationsAt( "processor" );
            logger = logRegistry.getForComponent(name);
            
            for ( int i = 0; i < processorConfs.size(); i++ )
            {
                final HierarchicalConfiguration processorConf = processorConfs.get(i);
                String processorName = processorConf.getString("[@name]");
                String processorClass = processorConf.getString("[@class]", LinearProcessor.class.getName());

                try {
                    logRegistry.registerForComponent(processorName, logger);
                    confRegistry.registerForComponent(processorName, processorConf);
                    
                    registry.registerBeanDefinition(processorName, BeanDefinitionBuilder.rootBeanDefinition(processorClass).setLazyInit(false).getBeanDefinition());              
                    processors.add(processorName);
                    
                   
                    if (logger.isInfoEnabled()) {
                        StringBuffer infoBuffer =
                            new StringBuffer(64)
                                    .append("Processor ")
                                    .append(processorName)
                                    .append(" instantiated.");
                        logger.info(infoBuffer.toString());
                    }
                } catch (Exception ex) {
                    if (logger.isErrorEnabled()) {
                        StringBuffer errorBuffer =
                           new StringBuffer(256)
                                   .append("Unable to init processor ")
                                   .append(processorName)
                                   .append(": ")
                                   .append(ex.toString());
                        logger.error( errorBuffer.toString(), ex );
                    }
                    throw new FatalBeanException("Unable to init processor " + processorName, ex);
                }
            }
        } catch (RegistryException e) {
            throw new FatalBeanException("Unable to load component for " +name , e);
        }
       
    }



    /*
     * (non-Javadoc)
     * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
     */
    public void setBeanName(String name) {
        this.name = name;
    }



    /*
     * (non-Javadoc)
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }

}
