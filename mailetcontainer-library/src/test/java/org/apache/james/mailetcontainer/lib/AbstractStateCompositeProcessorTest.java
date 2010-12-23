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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.mail.MessagingException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.impl.SimpleLog;
import org.apache.james.core.MailImpl;
import org.apache.james.mailetcontainer.api.MailProcessor;
import org.apache.james.mailetcontainer.lib.mock.MockMailProcessor;
import org.apache.mailet.Mail;

import junit.framework.TestCase;

public abstract class AbstractStateCompositeProcessorTest extends TestCase{

    
    public void testChooseRightProcessor() throws Exception {

        Map<String,String> configMap = new HashMap<String, String>();
        configMap.put("root", "test");
        configMap.put("test", "invalid");
        configMap.put("error", "invalid");
        
        final AtomicInteger count = new AtomicInteger(0);
        AbstractStateCompositeProcessor processor = new AbstractStateCompositeProcessor() {
            
            @Override
            protected MailProcessor createMailProcessor(final String state, HierarchicalConfiguration config) throws Exception {
                String newstate = config.getString("[@newstate]");
                return new MockMailProcessor(newstate) {

                    @Override
                    public void service(Mail mail) throws MessagingException {
                        // check if the right processor was selected depending on the state
                        assertEquals(state, mail.getState());
                        super.service(mail);
                        count.incrementAndGet();
                    }
                    
                };
            }
        };
        SimpleLog log = new SimpleLog("MockLog");
        log.setLevel(SimpleLog.LOG_LEVEL_DEBUG);
        processor.setLog(log);
        processor.configure(createMockConfig(configMap));
        processor.init();
        
        try {
            processor.service(new MailImpl());
            fail("Should have failed because of an not configured processor");
        } catch (MessagingException ex) {
            // we should have gone throw 2 processors
            assertEquals(2, count.get());
        } finally {
            processor.dispose();
        }
        
    }

    
    protected abstract AbstractStateCompositeProcessor createProcessor(HierarchicalConfiguration config) throws ConfigurationException, Exception;
    
    
    public void testGhostProcessor() throws Exception {
        AbstractStateCompositeProcessor processor = null;

        try {
            processor = createProcessor(createConfig(Arrays.asList("root", "error", "ghost")));

            fail("ghost processor should not be allowed");
        } catch (ConfigurationException e) {
            // expected
        } finally {
            if (processor != null) {
                processor.dispose();
            }
        }
       
    }
    
    public void testNoRootProcessor() throws Exception {
        AbstractStateCompositeProcessor processor = null;
        try {
            processor = createProcessor(createConfig(Arrays.asList("test", "error")));
            fail("root processor is needed");
        } catch (ConfigurationException e) {
            // expected
        } finally {
            if (processor != null) {
                processor.dispose();
            }
        }
    }
    
    public void testNoErrorProcessor() throws Exception {
        AbstractStateCompositeProcessor processor = null;
        try {
            processor = createProcessor(createConfig(Arrays.asList("test", "root")));
            fail("error processor is needed");
        } catch (ConfigurationException e) {
            // expected
        } finally {
            if (processor != null) {
                processor.dispose();
            }
        }
    }
    
    private HierarchicalConfiguration createConfig(List<String> states) throws ConfigurationException {

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?>");
        sb.append("<processors>");
        for (int i = 0 ; i < states.size(); i++) {
            sb.append("<processor state=\"");
            sb.append(states.get(i));
            sb.append("\"/>");
        }
        sb.append("</processors>");

        DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder();
        builder.load(new ByteArrayInputStream(sb.toString().getBytes()));
        return builder;
    }
    
    private HierarchicalConfiguration createMockConfig(Map<String,String> states) throws ConfigurationException {

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?>");
        sb.append("<processors>");
        Iterator<String> keys = states.keySet().iterator();
        while(keys.hasNext()) {
            String state = keys.next();
            String newstate = states.get(state);
            sb.append("<processor state=\"");
            sb.append(state);
            sb.append("\" newstate=\"");
            sb.append(newstate);
            sb.append("\"/>");
        }
        sb.append("</processors>");

        DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder();
        builder.load(new ByteArrayInputStream(sb.toString().getBytes()));
        return builder;
    }
}
