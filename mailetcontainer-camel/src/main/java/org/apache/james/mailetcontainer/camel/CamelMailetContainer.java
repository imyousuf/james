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
package org.apache.james.mailetcontainer.camel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.management.NotCompliantMBeanException;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.processor.aggregate.UseLatestAggregationStrategy;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.lifecycle.api.Configurable;
import org.apache.james.lifecycle.api.LogEnabled;
import org.apache.james.mailetcontainer.api.MailetContainer;
import org.apache.james.mailetcontainer.api.MailetContainerListener;
import org.apache.james.mailetcontainer.api.MailetLoader;
import org.apache.james.mailetcontainer.api.MatcherLoader;
import org.apache.james.mailetcontainer.lib.MailetConfigImpl;
import org.apache.james.mailetcontainer.lib.MatcherConfigImpl;
import org.apache.james.mailetcontainer.lib.jmx.JMXMailetContainerListener;
import org.apache.james.mailetcontainer.lib.matchers.CompositeMatcher;
import org.apache.mailet.Mail;
import org.apache.mailet.Mailet;
import org.apache.mailet.MailetConfig;
import org.apache.mailet.MailetContext;
import org.apache.mailet.Matcher;
import org.apache.mailet.MatcherConfig;
import org.apache.mailet.base.GenericMailet;
import org.apache.mailet.base.MatcherInverter;

public class CamelMailetContainer implements MailetContainer, Configurable, LogEnabled, CamelContextAware{

    private Log logger;
    private CamelContext context;
    private String processorName;
    private JMXMailetContainerListener jmxListener;
    private List<MailetContainerListener> listeners = Collections.synchronizedList(new ArrayList<MailetContainerListener>());
    private boolean enableJmx = true;
    private List<Matcher> matchers = new ArrayList<Matcher>();
    private List<Mailet> mailets = new ArrayList<Mailet>();
    private ProducerTemplate producerTemplate;
    private HierarchicalConfiguration config;
    
    private final UseLatestAggregationStrategy aggr = new UseLatestAggregationStrategy();
    private MailetContext mailetContext;
    private MatcherLoader matcherLoader;
    private MailetLoader mailetLoader;
    
    @Resource(name = "matcherloader")
    public void setMatcherLoader(MatcherLoader matcherLoader) {
        this.matcherLoader = matcherLoader;
    }

    @Resource(name = "mailetloader")
    public void setMailetLoader(MailetLoader mailetLoader) {
        this.mailetLoader = mailetLoader;
    }
    
    @Resource(name= "mailetcontext")
    public void setMailetContext(MailetContext mailetContext) {
        this.mailetContext = mailetContext;
    }

    
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.api.MailProcessor#service(org.apache.mailet.Mail)
     */
    public void service(Mail mail) throws MessagingException {
        try {
            producerTemplate.sendBody(getEndpoint(), mail);
            
         } catch (CamelExecutionException ex) {
             throw new MessagingException("Unable to process mail " + mail.getName(),ex);
         }        
     }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.api.MailetContainer#getMailets()
     */
    public List<Mailet> getMailets() {
        return Collections.unmodifiableList(mailets);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.api.MailetContainer#getMatchers()
     */
    public List<Matcher> getMatchers() {
        return Collections.unmodifiableList(matchers);       
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.api.MailetContainer#addListener(org.apache.james.mailetcontainer.api.MailetContainerListener)
     */
    public void addListener(MailetContainerListener listener) {
        listeners.add(listener);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.api.MailetContainer#removeListener(org.apache.james.mailetcontainer.api.MailetContainerListener)
     */
    public void removeListener(MailetContainerListener listener) {
        listeners.remove(listener);            
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.api.MailetContainer#getListeners()
     */
    public List<MailetContainerListener> getListeners() {
        return listeners;
    }


    /*
     * (non-Javadoc)
     * @see org.apache.camel.CamelContextAware#getCamelContext()
     */
    public CamelContext getCamelContext() {
        return context;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.camel.CamelContextAware#setCamelContext(org.apache.camel.CamelContext)
     */
    public void setCamelContext(CamelContext context) {
        this.context = context;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.api.LogEnabled#setLog(org.apache.commons.logging.Log)
     */
    public void setLog(Log log) {
        this.logger = log;
    }

    
    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.api.Configurable#configure(org.apache.commons.configuration.HierarchicalConfiguration)
     */
    public void configure(HierarchicalConfiguration config) throws ConfigurationException {
        this.config = config;
        this.processorName = config.getString("[@name]", null);
        if (processorName == null) throw new ConfigurationException("Processor name must be configured");
        this.enableJmx = config.getBoolean("[@enableJmx]", true);
        
    }
    
    /**
     * Return the endpoint for the processorname. 
     * 
     * This will return a "direct" endpoint. 
     * 
     * @param processorName
     * @return endPoint
     */
    protected String getEndpoint() {
        return "direct:processor." + processorName;
    }
    
    
    @PostConstruct
    public void init() throws Exception {
        context.addRoutes(new MailetContainerRouteBuilder());

        
        producerTemplate = context.createProducerTemplate();
        if (enableJmx) {
            this.jmxListener = new JMXMailetContainerListener(processorName, this);
            addListener(jmxListener);
        }
    }
    
    @PreDestroy
    public void destroy() {
        listeners.clear();
        if (enableJmx && jmxListener != null) {
            jmxListener.dispose();
        }

        for (int i = 0; i < mailets.size(); i++) {
            Mailet m = mailets.get(i);
            if (logger.isDebugEnabled()) {
                logger.debug("Shutdown mailet " + m.getMailetInfo());
            }
            m.destroy();

        }

        for (int i = 0; i < matchers.size(); i++) {
            Matcher m = matchers.get(i);
            if (logger.isDebugEnabled()) {
                logger.debug("Shutdown matcher " + m.getMatcherInfo());
            }
            m.destroy();
        }

    }
    


    private final class MailetContainerRouteBuilder extends RouteBuilder {

        @SuppressWarnings("unchecked")
        @Override
        public void configure() throws Exception {
            Processor disposeProcessor = new DisposeProcessor();
            Processor removePropsProcessor = new RemovePropertiesProcessor();
            Processor completeProcessor = new CompleteProcessor();
            
            if (processorName.equals(Mail.GHOST)) throw new ConfigurationException("ProcessorName of " + Mail.GHOST + " is reserved for internal use, choose a different name");


            RouteDefinition processorDef = from(getEndpoint()).routeId(processorName).inOnly()
            // store the logger in properties
            .setProperty(MatcherSplitter.LOGGER_PROPERTY, constant(logger));

            // load composite matchers if there are any
            Map<String,Matcher> compositeMatchers = new HashMap<String, Matcher>();
            loadCompositeMatchers(processorName, compositeMatchers, config.configurationsAt("matcher"));
            
            
            final List<HierarchicalConfiguration> mailetConfs = config.configurationsAt("mailet");
            
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
                        // try to load from compositeMatchers first
                        matcher = compositeMatchers.get(matcherName);
                        if (matcher == null) {
                            // no composite Matcher found, try to load it via MatcherLoader
                            matcher = matcherLoader.getMatcher(createMatcherConfig(matcherName));
                        }
                    } else if (invertedMatcherName != null) {
                        // try to load from compositeMatchers first
                        matcher = compositeMatchers.get(matcherName);
                        if (matcher == null) {
                            // no composite Matcher found, try to load it via MatcherLoader
                            matcher = matcherLoader.getMatcher(createMatcherConfig(invertedMatcherName));
                        }
                        matcher = new MatcherInverter(matcher);

                    } else {
                        // default matcher is All
                        matcher = matcherLoader.getMatcher(createMatcherConfig("All"));
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
                    mailet = mailetLoader.getMailet(createMailetConfig(mailetClassName, c));
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
                
                    MailetProcessor mailetProccessor = new MailetProcessor(mailet, logger, CamelMailetContainer.this);
                    // Store the matcher to use for splitter in properties
                    processorDef
                        .setProperty(MatcherSplitter.MATCHER_PROPERTY, constant(matcher)).setProperty(MatcherSplitter.ON_MATCH_EXCEPTION_PROPERTY, constant(onMatchException)).setProperty(MatcherSplitter.MAILETCONTAINER_PROPERTY, constant(CamelMailetContainer.this))
                        
                        // do splitting of the mail based on the stored matcher
                        .split().method(MatcherSplitter.class).aggregationStrategy(aggr).parallelProcessing()
                        
                        .choice().when(new MatcherMatch()).process(mailetProccessor).end()
                        
                        .choice().when(new MailStateEquals(Mail.GHOST)).process(disposeProcessor).stop().otherwise().process(removePropsProcessor).end()

                        .choice().when(new MailStateNotEquals(processorName)).process(completeProcessor).stop().end();

                    // store mailet and matcher
                    mailets.add(mailet);
                    matchers.add(matcher);
                }
          

            }
            
            Processor terminatingMailetProcessor = new MailetProcessor(new TerminatingMailet(), logger, CamelMailetContainer.this);

            
            processorDef
                // start choice
                .choice()
             
                // when the mail state did not change till yet ( the end of the route) we need to call the TerminatingMailet to
                // make sure we don't fall into a endless loop
                .when(new MailStateEquals(processorName)).process(terminatingMailetProcessor).stop()
                
                   
                // dispose when needed
                .when(new MailStateEquals(Mail.GHOST)).process(disposeProcessor).stop()
                
                 // this container is complete
                .otherwise().process(completeProcessor).stop();
            
                       
        }
        
        
        private MailetConfig createMailetConfig(String mailetName, HierarchicalConfiguration configuration) {

            final MailetConfigImpl configImpl = new MailetConfigImpl();
            configImpl.setMailetName(mailetName);
            configImpl.setConfiguration(configuration);
            configImpl.setMailetContext(mailetContext);
            return configImpl;
        }
        
        private MatcherConfig createMatcherConfig(String matchName) {

            String condition = (String) null;
            int i = matchName.indexOf('=');
            if (i != -1) {
                condition = matchName.substring(i + 1);
                matchName = matchName.substring(0, i);
            }
            final MatcherConfigImpl configImpl = new MatcherConfigImpl();
            configImpl.setMatcherName(matchName);
            configImpl.setCondition(condition);
            configImpl.setMailetContext(mailetContext);
            return configImpl;

        }
        
        /**
         * Load  {@link CompositeMatcher} implementations and their child {@link Matcher}'s
         * 
         * CompositeMatcher were added by JAMES-948
         * 
         * @param processorName
         * @param compMap
         * @param compMatcherConfs
         * @return compositeMatchers
         * @throws ConfigurationException
         * @throws MessagingException
         * @throws NotCompliantMBeanException
         */
        @SuppressWarnings("unchecked")
        private List<Matcher> loadCompositeMatchers(String processorName, Map<String,Matcher> compMap, List<HierarchicalConfiguration> compMatcherConfs) throws ConfigurationException, MessagingException, NotCompliantMBeanException {
            List<Matcher> matchers = new ArrayList<Matcher>();

            for (int j= 0 ; j < compMatcherConfs.size(); j++) {
                HierarchicalConfiguration c = compMatcherConfs.get(j);
                String compName = c.getString("[@name]", null);
                String matcherName = c.getString("[@match]", null);
                String invertedMatcherName = c.getString("[@notmatch]", null);

                Matcher matcher = null;
                if (matcherName != null && invertedMatcherName != null) {
                    // if no matcher is configured throw an Exception
                    throw new ConfigurationException("Please configure only match or nomatch per mailet");
                } else if (matcherName != null) {
                    matcher = matcherLoader.getMatcher(createMatcherConfig(matcherName));
                    if (matcher instanceof CompositeMatcher) {
                        CompositeMatcher compMatcher = (CompositeMatcher) matcher;
                        
                        List<Matcher> childMatcher = loadCompositeMatchers(processorName, compMap,c.configurationsAt("matcher"));
                        for (int i = 0 ; i < childMatcher.size(); i++) {
                            compMatcher.add(childMatcher.get(i));
                        }
                    }
                } else if (invertedMatcherName != null) {
                    Matcher m = matcherLoader.getMatcher(createMatcherConfig(invertedMatcherName));
                    if (m instanceof CompositeMatcher) {
                        CompositeMatcher compMatcher = (CompositeMatcher) m;
                        
                        List<Matcher> childMatcher = loadCompositeMatchers(processorName, compMap,c.configurationsAt("matcher"));
                        for (int i = 0 ; i < childMatcher.size(); i++) {
                            compMatcher.add(childMatcher.get(i));
                        }
                    }
                    matcher = new MatcherInverter(m);
                }
                if (matcher == null) throw new ConfigurationException("Unable to load matcher instance");
                matchers.add(matcher);
                if (compName != null) {
                    // check if there is already a composite Matcher with the name registered in the processor
                    if (compMap.containsKey(compName)) throw new ConfigurationException("CompositeMatcher with name " + compName + " is already defined in processor " + processorName);
                    compMap.put(compName, matcher);
                }
            }
            return matchers;
        }
        

        private final class RemovePropertiesProcessor implements Processor {

            public void process(Exchange exchange) throws Exception {
                exchange.removeProperty(MatcherSplitter.ON_MATCH_EXCEPTION_PROPERTY);
                exchange.removeProperty(MatcherSplitter.MATCHER_PROPERTY);
            }
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
        
        private final class CompleteProcessor implements Processor {
            
            public void process(Exchange ex) throws Exception {
                logger.debug("End of mailetcontainer" + processorName + " reached");
                ex.setProperty(Exchange.ROUTE_STOP, true);
            }
        }
        
    }

}
