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
package org.apache.james.mailetcontainer.lib;

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

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.lifecycle.api.Configurable;
import org.apache.james.lifecycle.api.LogEnabled;
import org.apache.james.mailetcontainer.api.MailetContainer;
import org.apache.james.mailetcontainer.api.MailetContainerListener;
import org.apache.james.mailetcontainer.api.MailetLoader;
import org.apache.james.mailetcontainer.api.MatcherLoader;
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

/**
 * Abstract base class for {@link MailetContainer} implementations which simplify to implement your own implementation
 *
 */
public abstract class AbstractMailetContainer implements MailetContainer, Configurable, LogEnabled{

    private MailetContext mailetContext;
    private MatcherLoader matcherLoader;
    private List<MailetContainerListener> listeners = Collections.synchronizedList(new ArrayList<MailetContainerListener>());
    private String processorName;
    private JMXMailetContainerListener jmxListener;
    private boolean enableJmx = true;
    private Log logger;
    private HierarchicalConfiguration config;
    private MailetLoader mailetLoader;
    private List<MatcherMailetPair> pairs = new ArrayList<MatcherMailetPair>();

    
    @Resource(name = "matcherloader")
    public void setMatcherLoader(MatcherLoader matcherLoader) {
        this.matcherLoader = matcherLoader;
    }

    @Resource(name= "mailetcontext")
    public void setMailetContext(MailetContext mailetContext) {
        this.mailetContext = mailetContext;
    }

    @Resource(name = "mailetloader")
    public void setMailetLoader(MailetLoader mailetLoader) {
        this.mailetLoader = mailetLoader;
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
        this.processorName = config.getString("[@name]", null);
        if (processorName == null) throw new ConfigurationException("Processor name must be configured");
        if (processorName.equals(Mail.GHOST)) throw new ConfigurationException("ProcessorName of " + Mail.GHOST + " is reserved for internal use, choose a different name");

        this.enableJmx = config.getBoolean("[@enableJmx]", true);
        this.config = config;
        
    }
   
   
    /**
     * Init the container
     * 
     * @throws Exception
     */
    @PostConstruct
    public void init() throws Exception {
        parseConfiguration();
        setupRouting(pairs);
        
        if (enableJmx) {
            this.jmxListener = new JMXMailetContainerListener(processorName, this);
            addListener(jmxListener);
        }
    }
    
    
    /**
     * Destroy the container
     */
    @PreDestroy
    public void destroy() {
        listeners.clear();
        if (enableJmx && jmxListener != null) {
            jmxListener.dispose();
        }

        for (int i = 0; i < pairs.size(); i++) {
            MatcherMailetPair pair = pairs.get(i);
            Mailet mailet = pair.getMailet();
            Matcher matcher = pair.getMatcher();
            if (logger.isDebugEnabled()) {
                logger.debug("Shutdown matcher " + matcher.getMatcherInfo());
            }
            matcher.destroy();
            
            if (logger.isDebugEnabled()) {
                logger.debug("Shutdown mailet " + mailet.getMailetInfo());
            }
            mailet.destroy();

        }
    }
    
    
    protected Log getLogger() {
        return logger;
    }
    
    protected String getName() {
        return processorName;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.api.MailetContainer#getMailets()
     */
    public List<Mailet> getMailets() {
        List<Mailet> mailets = new ArrayList<Mailet>();
        for (int i = 0; i < pairs.size(); i++) {
            mailets.add(pairs.get(i).getMailet());
        }
        return Collections.unmodifiableList(mailets);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.api.MailetContainer#getMatchers()
     */
    public List<Matcher> getMatchers() {
        List<Matcher> matchers = new ArrayList<Matcher>();
        for (int i = 0; i < pairs.size(); i++) {
            matchers.add(pairs.get(i).getMatcher());
        }
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


    /**
     * Create a {@link MailetConfig} for the given mailetname and configuration
     * 
     * @param mailetName
     * @param configuration
     * @return mailetConfig
     */
    private MailetConfig createMailetConfig(String mailetName, HierarchicalConfiguration configuration) {

        final MailetConfigImpl configImpl = new MailetConfigImpl();
        configImpl.setMailetName(mailetName);
        configImpl.setConfiguration(configuration);
        configImpl.setMailetContext(mailetContext);
        return configImpl;
    }
    
    /**
     * Create a {@link MatcherConfig} for the given "match=" attribute. 
     * 
     * @param matchName
     * @return matcherConfig
     */
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
    private List<Matcher> loadCompositeMatchers(String processorName, Map<String,Matcher> compMap, List<HierarchicalConfiguration> compMatcherConfs) throws ConfigurationException, MessagingException {
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
    
    @SuppressWarnings("unchecked")
    private void parseConfiguration() throws MessagingException, ConfigurationException {
        
        // load composite matchers if there are any
        Map<String,Matcher> compositeMatchers = new HashMap<String, Matcher>();
        loadCompositeMatchers(getName(), compositeMatchers, config.configurationsAt("matcher"));
        
        
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
                throw new ConfigurationException("Unable to init matcher " + matcherName, ex);
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
                throw new ConfigurationException("Unable to init mailet " + mailetClassName, ex);
            }
            
            if (matcher != null && mailet != null) {
                pairs.add(new MatcherMailetPair(matcher, mailet));
            } else {
                throw new ConfigurationException("Unable to load Mailet or Matcher");
            }
        }
    }
    
    /**
     * Setup the routing for the configured {@link MatcherMailetPair}'s for this {@link MailetContainer}
     * 
     * 
     */
    protected abstract void setupRouting(List<MatcherMailetPair> pairs) throws MessagingException;
    
    
    
    /**
     * Mailet which protect us to not fall into an endless loop caused by an configuration error
     *
     */
    public final class TerminatingMailet extends GenericMailet {
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
    

}
