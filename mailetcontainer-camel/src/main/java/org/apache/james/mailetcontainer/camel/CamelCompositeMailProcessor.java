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

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.mailetcontainer.api.CompositeMailProcessor;
import org.apache.james.mailetcontainer.api.MailProcessor;
import org.apache.james.mailetcontainer.api.MailetLoader;
import org.apache.james.mailetcontainer.api.MatcherLoader;
import org.apache.james.mailetcontainer.lib.AbstractCompositeMailProcessor;
import org.apache.james.mailetcontainer.lib.matchers.CompositeMatcher;
import org.apache.mailet.Mail;
import org.apache.mailet.Mailet;
import org.apache.mailet.MailetContext;
import org.apache.mailet.Matcher;

/**
 * Build up the Camel Routes by parsing the mailetcontainer.xml configuration file. 
 * 
 * It also offer the {@link CompositeMailProcessor} implementation which allow to inject {@link Mail} into the routes.
 *
 * Beside the basic {@link Mailet} / {@link Matcher} support this implementation also supports {@link CompositeMatcher} implementations.
 * See JAMES-948 
 * 
 */
public class CamelCompositeMailProcessor extends AbstractCompositeMailProcessor implements CamelContextAware{

    private CamelContext camelContext;
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

    
    @PostConstruct
    public void init() throws Exception {
        super.init();

        // Make sure the camel context get started
        // See https://issues.apache.org/jira/browse/JAMES-1069
        if (getCamelContext().getStatus().isStopped()) {
            getCamelContext().start();
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.camel.CamelContextAware#getCamelContext()
     */
	public CamelContext getCamelContext() {
        return camelContext;
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.apache.camel.CamelContextAware#setCamelContext(org.apache.camel.
     * CamelContext)
     */
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailetcontainer.lib.AbstractCompositeMailProcessor#createMailProcessor(java.lang.String, org.apache.commons.configuration.HierarchicalConfiguration)
     */
    protected MailProcessor createMailProcessor(String name, HierarchicalConfiguration config) throws Exception{
        CamelMailetContainer container = new CamelMailetContainer();
        container.setLog(logger);
        container.setCamelContext(camelContext);
        container.setMailetContext(mailetContext);
        container.setMailetLoader(mailetLoader);
        container.setMatcherLoader(matcherLoader);
        container.configure(config);
        container.init();
        return container;
    }

    @Override
    public void dispose() {
        String names[] = getProcessorNames();
        for (int i = 0; i < names.length; i++) {
            MailProcessor processor = getProcessor(names[i]);
            if (processor instanceof CamelMailetContainer) {
                ((CamelMailetContainer) processor).destroy();
            }

        }
        super.dispose();
    }
 

}
