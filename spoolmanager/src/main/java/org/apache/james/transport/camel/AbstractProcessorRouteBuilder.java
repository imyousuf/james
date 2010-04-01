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

package org.apache.james.transport.camel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.mail.MessagingException;

import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ChoiceDefinition;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.lifecycle.Configurable;
import org.apache.james.lifecycle.LogEnabled;
import org.apache.james.services.SpoolManager;
import org.apache.james.transport.MailetConfigImpl;
import org.apache.james.transport.MailetLoader;
import org.apache.james.transport.MatcherConfigImpl;
import org.apache.james.transport.MatcherLoader;
import org.apache.mailet.Mail;
import org.apache.mailet.Mailet;
import org.apache.mailet.MailetConfig;
import org.apache.mailet.Matcher;
import org.apache.mailet.MatcherConfig;
import org.apache.mailet.base.GenericMailet;
import org.apache.mailet.base.MatcherInverter;

/**
 * Build up the Camel Route by parsing the spoolmanager.xml configuration file. 
 * 
 */
public abstract class AbstractProcessorRouteBuilder extends RouteBuilder implements SpoolManager, Configurable, LogEnabled {

    private MatcherLoader matcherLoader;
    private HierarchicalConfiguration config;
    private MailetLoader mailetLoader;
    private Log logger;

    private final Map<String,List<Mailet>> mailets = new HashMap<String,List<Mailet>>();
    private final Map<String,List<Matcher>> matchers = new HashMap<String,List<Matcher>>();

    private final List<String> processors = new ArrayList<String>();
    
       
    @Resource(name = "matcherpackages")
    public void setMatcherLoader(MatcherLoader matcherLoader) {
        this.matcherLoader = matcherLoader;
    }

    @Resource(name = "mailetpackages")
    public void setMailetLoader(MailetLoader mailetLoader) {
        this.mailetLoader = mailetLoader;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.camel.builder.RouteBuilder#configure()
     */
    @SuppressWarnings("unchecked")
    @Override
    public void configure() throws Exception {

        Processor terminatingMailetProcessor = new MailetProcessor(new TerminatingMailet(), logger);
        Processor disposeProcessor = new DisposeProcessor();
        
        List<HierarchicalConfiguration> processorConfs = config.configurationsAt("processor");
        for (int i = 0; i < processorConfs.size(); i++) {
            final HierarchicalConfiguration processorConf = processorConfs.get(i);
            String processorName = processorConf.getString("[@name]");

          
            processors.add(processorName);
            mailets.put(processorName, new ArrayList<Mailet>());
            matchers.put(processorName, new ArrayList<Matcher>());

            // Check which route we need to go
            ChoiceDefinition processorDef = fromF(getFromUri(processorName))
            
                // exchange mode is inOnly
                .inOnly()
                
                // use transaction
                .transacted()
                
                // dispose the mail object if an exception was thrown while processing this route
                .onException(Exception.class).process(disposeProcessor).end()
                
                // dispose the mail object if route processing was complete
                .onCompletion().process(disposeProcessor).end()
                
                // check that body is not null, just to be sure...
                .choice().when(body().isNotNull());
            
            final List<HierarchicalConfiguration> mailetConfs = processorConf.configurationsAt("mailet");
            // Loop through the mailet configuration, load
            // all of the matcher and mailets, and add
            // them to the processor.
            for (int j = 0; j < mailetConfs.size(); j++) {
                HierarchicalConfiguration c = mailetConfs.get(j);

                // We need to set this because of correctly parsing comma
                String mailetClassName = c.getString("[@class]");
                String matcherName = c.getString("[@match]", null);
                String invertedMatcherName = c.getString("[@notmatch]", null);

                Mailet mailet = null;
                Matcher matcher = null;
                try {

                    if (matcherName != null && invertedMatcherName != null) {
                        // if no matcher is configured throw an Exception
                        throw new ConfigurationException("Please configure only match or nomatch per mailet");
                    } else if (matcherName != null) {
                        matcher = matcherLoader.getMatcher(matcherName);
                    } else if (invertedMatcherName != null) {
                        matcher = new MatcherInverter(matcherLoader.getMatcher(invertedMatcherName));

                    } else {
                        // default matcher is All
                        matcher = matcherLoader.getMatcher("All");
                    }

                    // The matcher itself should log that it's been inited.
                    if (logger.isInfoEnabled()) {
                        StringBuffer infoBuffer = new StringBuffer(64).append("Matcher ").append(matcherName).append(" instantiated.");
                        logger.info(infoBuffer.toString());
                    }
                } catch (MessagingException ex) {
                    // **** Do better job printing out exception
                    if (logger.isErrorEnabled()) {
                        StringBuffer errorBuffer = new StringBuffer(256).append("Unable to init matcher ").append(matcherName).append(": ").append(ex.toString());
                        logger.error(errorBuffer.toString(), ex);
                        if (ex.getNextException() != null) {
                            logger.error("Caused by nested exception: ", ex.getNextException());
                        }
                    }
                    System.err.println("Unable to init matcher " + matcherName);
                    System.err.println("Check spool manager logs for more details.");
                    // System.exit(1);
                    throw new ConfigurationException("Unable to init matcher", ex);
                }
                try {
                    mailet = mailetLoader.getMailet(mailetClassName, c);
                    if (logger.isInfoEnabled()) {
                        StringBuffer infoBuffer = new StringBuffer(64).append("Mailet ").append(mailetClassName).append(" instantiated.");
                        logger.info(infoBuffer.toString());
                    }
                } catch (MessagingException ex) {
                    // **** Do better job printing out exception
                    if (logger.isErrorEnabled()) {
                        StringBuffer errorBuffer = new StringBuffer(256).append("Unable to init mailet ").append(mailetClassName).append(": ").append(ex.toString());
                        logger.error(errorBuffer.toString(), ex);
                        if (ex.getNextException() != null) {
                            logger.error("Caused by nested exception: ", ex.getNextException());
                        }
                    }
                    System.err.println("Unable to init mailet " + mailetClassName);
                    System.err.println("Check spool manager logs for more details.");
                    throw new ConfigurationException("Unable to init mailet", ex);
                }
                if (mailet != null && matcher != null) {
                    String onMatchException = null;
                    MailetConfig mailetConfig = mailet.getMailetConfig();
                    
                    if (mailetConfig instanceof MailetConfigImpl) {
                        onMatchException = ((MailetConfigImpl) mailetConfig).getInitAttribute("onMatchException");
                    }
                    
                    // Store the matcher to use for splitter in properties
                    processorDef.setProperty(MatcherSplitter.MATCHER_PROPERTY, constant(matcher))
                    
                            // store the config in properties
                            .setProperty(MatcherSplitter.ON_MATCH_EXCEPTION_PROPERTY, constant(onMatchException))
                            
                            // store the logger in properties
                            .setProperty(MatcherSplitter.LOGGER_PROPERTY, constant(logger))

                            // do splitting of the mail based on the stored matcher
                            .split().method(MatcherSplitter.class)
                            
                            // speed up things by processing in parallel
                            .parallelProcessing()
                            
                            // start first choice
                            .choice()
                            
                            // check if we need to execute the mailet. If so execute it and remove the header on the end
                            .when(new MatcherMatch()).process(new MailetProcessor(mailet, logger))
                                            
                            
                            // end second choice
                            .end()
                            
                            .choice()
               
                            // if the mailstate is GHOST whe should just dispose and stop here.
                            .when(new MailStateEquals(Mail.GHOST)).stop()
                             
                            // check if the state of the mail is the same as the
                            // current processor. If not just route it to the right endpoint via recipientList and stop processing.
                            .when(new MailStateNotEquals(processorName)).recipientList().method(getRecipientList()).stop()
                            
                            // end first choice
                            .end()
                            
                            // remove matcher from properties
                            .removeProperty(MatcherSplitter.MATCHER_PROPERTY)

                            // remove config from properties
                            .removeProperty(MatcherSplitter.ON_MATCH_EXCEPTION_PROPERTY)
                    
                            // remove logger from properties
                            .removeProperty(MatcherSplitter.LOGGER_PROPERTY);

                    // store mailet and matcher
                    mailets.get(processorName).add(mailet);
                    matchers.get(processorName).add(matcher);
                }
              

            }
            
            processorDef
                    // start choice
                    .choice()
                    
                    // when the mail state did not change till yet ( the end of the route) we need to call the TerminatingMailet to
                    // make sure we don't fall into a endless loop
                    .when(new MailStateEquals(processorName)).process(terminatingMailetProcessor)
                    
                    // end the choice
                    .end()
                    
                     // route it to the right processor
                    .recipientList().method(getRecipientList());
                  
        }
    }


    /**
     * Destroy all mailets and matchers
     */
    @PreDestroy
    public void dispose() {
        boolean debugEnabled = logger.isDebugEnabled();

        Iterator<List<Mailet>> it = mailets.values().iterator();
        while (it.hasNext()) {
            List<Mailet> mList = it.next();
            for (int i = 0; i < mList.size(); i++) {
                Mailet m = mList.get(i);
                if (debugEnabled) {
                    logger.debug("Shutdown mailet " + m.getMailetInfo());
                }
                m.destroy();
            }
           
        }
        
        Iterator<List<Matcher>> mit = matchers.values().iterator();     
        while (mit.hasNext()) {
            List<Matcher> mList = mit.next();
            for (int i = 0; i < mList.size(); i++) {
                Matcher m = mList.get(i);
                if (debugEnabled) {
                    logger.debug("Shutdown matcher " + m.getMatcherInfo());
                }
                m.destroy();
            }
           
        }      
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.Configurable#configure(org.apache.commons.configuration.HierarchicalConfiguration)
     */
    public void configure(HierarchicalConfiguration config) throws ConfigurationException {
        this.config = config;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.LogEnabled#setLog(org.apache.commons.logging.Log)
     */
    public void setLog(Log log) {
        this.logger = log;

    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.services.SpoolManager#getMailetConfigs(java.lang.String)
     */
    public List<MailetConfig> getMailetConfigs(String processorName) {
        List<MailetConfig> mailetConfigs = new ArrayList<MailetConfig>();
        Iterator<Mailet> iterator = mailets.get(processorName).iterator();
        while (iterator.hasNext()) {
            Mailet mailet = (Mailet) iterator.next();
            MailetConfig mailetConfig = mailet.getMailetConfig();
            if (mailetConfig == null) mailetConfigs.add(new MailetConfigImpl()); // placeholder
            else mailetConfigs.add(mailetConfig);
        }
        return mailetConfigs; 
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.services.SpoolManager#getMatcherConfigs(java.lang.String)
     */
    public List<MatcherConfig> getMatcherConfigs(String processorName) {
        List<MatcherConfig> matcherConfigs = new ArrayList<MatcherConfig>();
        Iterator<Matcher> iterator = matchers.get(processorName).iterator();
        while (iterator.hasNext()) {
            Matcher matcher = (Matcher) iterator.next();
            MatcherConfig matcherConfig = matcher.getMatcherConfig();
            if (matcherConfig == null) matcherConfigs.add(new MatcherConfigImpl()); // placeholder
            else matcherConfigs.add(matcherConfig);
        }      
        return matcherConfigs;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.services.SpoolManager#getProcessorNames()
     */
    public String[] getProcessorNames() {
        return processors.toArray(new String[processors.size()]);
    }

    
    /**
     * Mailet which protect us to not fall into an endless loop caused by an configuration error
     *
     */
    private final class TerminatingMailet extends GenericMailet {
        /**
         *  The name of the mailet used to terminate the mailet chain.  The
         *  end of the matcher/mailet chain must be a matcher that matches
         *  all mails and a mailet that sets every mail to GHOST status.
         *  This is necessary to ensure that mails are removed from the spool
         *  in an orderly fashion.
         */
        private static final String TERMINATING_MAILET_NAME = "Terminating%Mailet%Name";

        /*
         * (non-Javadoc)
         * @see org.apache.mailet.base.GenericMailet#service(org.apache.mailet.Mail)
         */
        public void service(Mail mail) {
            if (!(Mail.ERROR.equals(mail.getState()))) {
                // Don't complain if we fall off the end of the
                // error processor.  That is currently the
                // normal situation for James, and the message
                // will show up in the error store.
                StringBuffer warnBuffer = new StringBuffer(256)
                                      .append("Message ")
                                      .append(mail.getName())
                                      .append(" reached the end of this processor, and is automatically deleted.  This may indicate a configuration error.");
                logger.warn(warnBuffer.toString());
            }
            
            // Set the mail to ghost state
            mail.setState(Mail.GHOST);
        }
    
        @Override
        public String getMailetInfo() {
            return getMailetName();
        }
    
        @Override
        public String getMailetName() {
            return TERMINATING_MAILET_NAME;
        }
    }
    
    /**
     * Return the uri for the processor to use for consuming mails
     * 
     * @param processor
     * @return consumerUri
     */
    protected abstract String getFromUri(String processor);

    /**
     * Return the class which get used for dynamic lookup the ToUris for the mails (producers)
     * 
     * @return recipientListClass
     */
    protected abstract Class<?> getRecipientList();
}
