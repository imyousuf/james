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

package org.apache.james.mailetcontainer.impl;

import java.io.ByteArrayInputStream;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.james.mailetcontainer.impl.MailetConfigImpl;

import junit.framework.TestCase;

public class MailetConfigImplTest extends TestCase{

    public void testDotParamsFromXML() throws ConfigurationException {
        DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder();
        builder.load(new ByteArrayInputStream("<mailet><mail.debug>true</mail.debug></mailet>".getBytes()));
        
        MailetConfigImpl config = new MailetConfigImpl();
        config.setConfiguration(builder);
        
        String param = config.getInitParameterNames().next();
        assertEquals("mail.debug", param);
        assertEquals("true", config.getInitParameter(param));
    }
    
    public void testDotParamsFromConfig() throws ConfigurationException {
        DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder();
        builder.addProperty("mail.debug", "true");
        
        MailetConfigImpl config = new MailetConfigImpl();
        config.setConfiguration(builder);
        
        String param = config.getInitParameterNames().next();
        assertEquals("mail.debug", param);
        assertEquals("true", config.getInitParameter(param));
    }
    
    // See JAMES-1232
    public void testParamWithComma() throws ConfigurationException {
        DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder();
        builder.load(new ByteArrayInputStream("<mailet><whatever>value1,value2</whatever></mailet>".getBytes()));
        
        MailetConfigImpl config = new MailetConfigImpl();
        config.setConfiguration(builder);
        
        String param = config.getInitParameterNames().next();
        assertEquals("whatever", param);
        assertEquals("value1,value2", config.getInitParameter(param));
    }
}
