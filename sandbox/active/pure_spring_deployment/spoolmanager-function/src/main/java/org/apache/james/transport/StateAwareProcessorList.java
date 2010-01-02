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



package org.apache.james.transport;


import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.mail.MessagingException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.api.kernel.LoaderService;
import org.apache.james.lifecycle.Configurable;
import org.apache.james.lifecycle.LogEnabled;
import org.apache.james.services.SpoolRepository;
import org.apache.mailet.Mail;
import org.apache.mailet.MailetException;

/**
 * This class is responsible for creating a set of named processors and
 * directing messages to the appropriate processor (given the State of the mail)
 *
 */
public class StateAwareProcessorList implements MailProcessor, ProcessorList, LogEnabled, Configurable {

    /**
     * The map of processor names to processors
     */
    private final Map<String, MailProcessor> processors;

    private Log logger;

    private HierarchicalConfiguration config;

    private MailetLoader mailetLoader;

    private MatcherLoader matcherLoader;

    private SpoolRepository spoolRepos;

    private LoaderService loader;
    
    public StateAwareProcessorList() {
        super();
        this.processors = new HashMap<String, MailProcessor>();
    }

    
    public final void setLog(Log logger) {
        this.logger = logger;
    }
    

    @Resource(name="mailetpackages")
    public final void setMailetLoader(MailetLoader mailetLoader) {
        this.mailetLoader = mailetLoader;
    }
    
    @Resource(name="matcherpackages")
    public final void setMatcherLoader(MatcherLoader matcherLoader) {
        this.matcherLoader = matcherLoader;
    }

    @Resource(name="spoolrepository")
    public final void setSpoolRepository(SpoolRepository spoolRepos) {
        this.spoolRepos = spoolRepos;
    }
    
    @Resource(name="org.apache.james.LoaderService")
    public final void setLoaderService(LoaderService loader) {
        this.loader = loader;
    }
    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    @SuppressWarnings("unchecked")
    @PostConstruct
    public void init() throws Exception {
        final List<HierarchicalConfiguration> processorConfs = config.configurationsAt( "processor" );
        for ( int i = 0; i < processorConfs.size(); i++ )
        {
            final HierarchicalConfiguration processorConf = processorConfs.get(i);
            String processorName = processorConf.getString("[@name]");
            String processorClass = processorConf.getString("[@class]","org.apache.james.transport.LinearProcessor");

            try {
               
                MailProcessor processor = (MailProcessor) Thread.currentThread().getContextClassLoader().loadClass(processorClass).newInstance();
                loader.injectDependenciesWithLifecycle(processor, logger, processorConf);
              
                processors.put(processorName, processor);
                
                //setupLogger(processor, processorName);
                //ContainerUtil.service(processor, compMgr);
                //ContainerUtil.configure(processor, processorConf);
                
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
                throw ex;
            }
        }
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
                    = (MailProcessor)processors.get(processorName);
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
     * The dispose operation is called at the end of a components lifecycle.
     * Instances of this class use this method to release and destroy any
     * resources that they own.
     *
     * This implementation shuts down the Processors managed by this
     * Component
     *
     * @see org.apache.avalon.framework.activity.Disposable#dispose()
     */
    @PreDestroy
    public void dispose() {
        Iterator<String> it = processors.keySet().iterator();
        while (it.hasNext()) {
            String processorName = it.next();
            if (logger.isDebugEnabled()) {
                logger.debug("Processor " + processorName);
            }
            Object processor = processors.get(processorName);
            processors.remove(processor);
        }
    }

    /**
     * @return names of all configured processors
     */
    public String[] getProcessorNames() {
        return (String[]) processors.keySet().toArray(new String[]{});
    }

    public MailProcessor getProcessor(String name) {
        return (MailProcessor) processors.get(name);
    }


	public void configure(HierarchicalConfiguration config)
			throws org.apache.commons.configuration.ConfigurationException {
		this.config = config;
	}

}
