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
import java.util.Iterator;
import java.util.List;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.mail.MessagingException;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ChoiceDefinition;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.lifecycle.Configurable;
import org.apache.james.lifecycle.LogEnabled;
import org.apache.james.transport.MailetLoader;
import org.apache.james.transport.MatcherLoader;
import org.apache.mailet.Mailet;
import org.apache.mailet.Matcher;
import org.apache.mailet.base.MatcherInverter;

/**
 * Build up the Camel Route by parsing the spoolmanager.xml configuration file. 
 * 
 * @author norman
 *
 */
public class MailProcessorRouteBuilder extends RouteBuilder implements Configurable, LogEnabled {

    private MatcherLoader matcherLoader;
    private HierarchicalConfiguration config;
    private MailetLoader mailetLoader;
    private Log logger;

    private List<Mailet> mailets = new ArrayList<Mailet>();
    
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

        
        ChoiceDefinition spoolDef = from("spool://spoolRepository").choice();
        
        List<HierarchicalConfiguration> processorConfs = config.configurationsAt("processor");
        for (int i = 0; i < processorConfs.size(); i++) {
            final HierarchicalConfiguration processorConf = processorConfs.get(i);
            String processorName = processorConf.getString("[@name]");

            // Check which route we need to go
            ChoiceDefinition processorDef = spoolDef.when(header(MailMessage.STATE).isEqualTo(processorName)).setHeader("currentProcessor", constant(processorName));
            
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
                    // Store the matcher to use for splitter
                    processorDef.setHeader(MatcherSplitter.MATCHER_HEADER, constant(matcher))
                        // do splitting
                        .split().method(MatcherSplitter.class)
                            // check if we need to execute the mailet and remove headers
                            .choice().when(header(MatcherSplitter.MATCHER_MATCHED_HEADER).isEqualTo(true)).process(new MailetProcessor(mailet)).removeHeader(MatcherSplitter.MATCHER_MATCHED_HEADER)
                            .end()
                        .end()
                    // remove matcher from header
                    .removeHeader(MatcherSplitter.MATCHER_HEADER);
                    
                    // store mailet for later destroy on shutdown
                    mailets.add(mailet);
                }
              

            }
            processorDef.to("spool://spoolRepository");
        }

        // just use a mock for now
        spoolDef.end();
        
    }

    @PreDestroy
    public void dispose() {
        Iterator<Mailet> it = mailets.iterator();
        boolean debugEnabled = logger.isDebugEnabled();
        while (it.hasNext()) {
            Mailet mailet = it.next();
            if (debugEnabled) {
                logger.debug("Shutdown mailet " + mailet.getMailetInfo());
            }
            mailet.destroy();
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

}
