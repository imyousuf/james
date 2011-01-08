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
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import javax.mail.MessagingException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.core.MailImpl;
import org.apache.james.mailetcontainer.api.mock.MockMailet;
import org.apache.james.mailetcontainer.api.mock.MockMatcher;
import org.apache.james.mailetcontainer.lib.AbstractStateMailetProcessor.MailetProcessorListener;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.Mailet;
import org.apache.mailet.Matcher;

import junit.framework.TestCase;

public abstract class AbstractStateMailetProcessorTest extends TestCase{

    protected abstract AbstractStateMailetProcessor createProcessor(HierarchicalConfiguration configuration) throws ConfigurationException, Exception;


    
    private HierarchicalConfiguration createConfig(int count) throws ConfigurationException {
        StringBuilder sb = new StringBuilder();
        sb.append("<processor state=\"" + Mail.DEFAULT + "\">");
        sb.append("<mailet match=\"").append(MockMatcher.class.getName()).append("=").append(count).append("\"").append(" class=\"").append(MockMailet.class.getName()).append("\">");
        sb.append("<state>test</state>");
        sb.append("</mailet>");
        
        sb.append("</processor>");
        
        DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder();
        builder.load(new ByteArrayInputStream(sb.toString().getBytes()));
        return builder;
    }
    
    public void testSimpleRouting() throws ConfigurationException, Exception {
        final CountDownLatch latch = new CountDownLatch(2);
        final MailImpl mail = new MailImpl();
        mail.setName(MailImpl.getId());
        mail.setSender(new MailAddress("test@localhost"));
        mail.setRecipients(Arrays.asList(new MailAddress("test@localhost"), new MailAddress("test2@localhost")));
        
        AbstractStateMailetProcessor processor = createProcessor(createConfig(1));
        processor.addListener(new MailetProcessorListener() {
                        
            public void afterMatcher(Matcher m, String mailName, Collection<MailAddress> recipients, Collection<MailAddress> matches, long processTime, MessagingException e) {
                if (MockMatcher.class.equals(m.getClass())) {
                    assertEquals(mail.getName(), mailName);
                    // match one recipient
                    assertEquals(1, matches.size());
                    assertNull(e);
                    latch.countDown();
                }

            }
            
            public void afterMailet(Mailet m, String mailName, String state, long processTime, MessagingException e) {
                // check for class name as the terminating  mailet will kick in too

                if (MockMailet.class.equals(m.getClass())) {
                    //assertEquals(mail.getName(), mailName);
                    assertEquals("test", state);
                    assertNull(e);
                    latch.countDown();
                }
            }
        });
       
        assertEquals(Mail.DEFAULT, mail.getState());
        processor.service(mail);
        
        
        // the source mail should be ghosted as it reached the end of processor as only one recipient matched 
        assertEquals(Mail.GHOST, mail.getState());
        latch.await();
        processor.destroy();

    }
    
    public void testSimpleRoutingMatchAll() throws ConfigurationException, Exception {
        final CountDownLatch latch = new CountDownLatch(2);
        final MailImpl mail = new MailImpl();
        mail.setName(MailImpl.getId());
        mail.setSender(new MailAddress("test@localhost"));
        mail.setRecipients(Arrays.asList(new MailAddress("test@localhost"), new MailAddress("test2@localhost")));
        
        AbstractStateMailetProcessor processor = createProcessor(createConfig(2));
        processor.addListener(new MailetProcessorListener() {
                        
            public void afterMatcher(Matcher m, String mailName, Collection<MailAddress> recipients, Collection<MailAddress> matches, long processTime, MessagingException e) {
                if (MockMatcher.class.equals(m.getClass())) {
                    assertEquals(mail.getName(), mailName);
                    // match one recipient
                    assertEquals(2, matches.size());
                    assertNull(e);
                    latch.countDown();
                }

            }
            
            public void afterMailet(Mailet m, String mailName, String state, long processTime, MessagingException e) {
                // check for class name as the terminating  mailet will kick in too

                if (MockMailet.class.equals(m.getClass())) {
                    //assertEquals(mail.getName(), mailName);
                    assertEquals("test", state);
                    assertNull(e);
                    latch.countDown();
                }
            }
        });
       
        assertEquals(Mail.DEFAULT, mail.getState());
        processor.service(mail);
        
        
        // the source mail should have the new state as it was a full match
        assertEquals("test", mail.getState());
        latch.await();
        processor.destroy();

    }
}
