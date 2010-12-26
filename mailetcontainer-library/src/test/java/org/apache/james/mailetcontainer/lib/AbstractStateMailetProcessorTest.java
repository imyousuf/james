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

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.core.MailImpl;
import org.apache.james.mailetcontainer.api.mock.MockMailet;
import org.apache.james.mailetcontainer.api.mock.MockMatcher;
import org.apache.mailet.MailAddress;

import junit.framework.TestCase;

public abstract class AbstractStateMailetProcessorTest extends TestCase{

    protected abstract AbstractStateMailetProcessor createProcessor(HierarchicalConfiguration configuration) throws ConfigurationException, Exception;


    
    private HierarchicalConfiguration createConfig() throws ConfigurationException {
        StringBuilder sb = new StringBuilder();
        sb.append("<processor>");
        sb.append("<mailet match=\"").append(MockMatcher.class.getName()).append("=test@localhost\"").append(" class=\"").append(MockMailet.class.getName()).append("\">");
        sb.append("<flag>test</flag>");
        sb.append("</mailet>");
        
        sb.append("</processor");
        
        DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder();
        builder.load(new ByteArrayInputStream(sb.toString().getBytes()));
        return builder;
    }
    
    public void testRouting() throws ConfigurationException, Exception {
        AbstractStateMailetProcessor processor = createProcessor(createConfig());
        
        MailImpl mail = new MailImpl();
        mail.setSender(new MailAddress("test@localhost"));
        mail.setRecipients(Arrays.asList(new MailAddress("test@localhost"), new MailAddress("test2@localhost")));
        
        processor.service(mail);
    }
}
