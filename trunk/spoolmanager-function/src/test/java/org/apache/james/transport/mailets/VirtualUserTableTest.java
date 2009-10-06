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

package org.apache.james.transport.mailets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import javax.mail.MessagingException;

import junit.framework.TestCase;

import org.apache.james.api.vut.ErrorMappingException;
import org.apache.james.test.mock.james.MockVirtualUserTableStore;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.base.test.FakeMail;
import org.apache.mailet.base.test.FakeMailContext;
import org.apache.mailet.base.test.FakeMailetConfig;
import org.apache.mailet.base.test.FakeMimeMessage;

public class VirtualUserTableTest extends TestCase{

    private VirtualUserTable table;
    private MockVirtualUserTableStore mockVutStore;
    
    @Override
    protected void setUp() throws Exception {
        
        table = new VirtualUserTable();
        final FakeMailContext mockMailetContext = new FakeMailContext() {

            @Override
            public boolean isLocalServer(String serverName) {
                if (serverName.equals("localhost")) {
                    return true;
                }
                
                return false;
            }
            
        };
        FakeMailetConfig mockMailetConfig = new FakeMailetConfig("vut", mockMailetContext, new Properties());
        //mockMailetConfig.put("virtualusertable", "vut");
        
        table.setVut(new org.apache.james.api.vut.VirtualUserTable() {

            public Collection<String> getMappings(String user, String domain) throws ErrorMappingException {
                if (user.equals("test") && domain.equals("localhost")) return Arrays.asList(new String[]{"whatever@localhost","blah@localhost"});
                return null;
            }
            
        });

        table.init(mockMailetConfig);

    }


    @Override
    protected void tearDown() throws Exception {
        table = null;
        
    }


    public void testAddressMapping() throws Exception {
        Mail mail = createMail(new String[] {"test@localhost", "apache@localhost"});
        table.service(mail);
        
        assertEquals(3,mail.getRecipients().size());
        Iterator it = mail.getRecipients().iterator();
        assertEquals("whatever@localhost", ((MailAddress)it.next()).toString());
        assertEquals("blah@localhost", ((MailAddress)it.next()).toString());
        assertEquals("apache@localhost", ((MailAddress)it.next()).toString());

    }
    
    /**
     * @return
     * @throws MessagingException 
     */
    private Mail createMail(String[] recipients) throws MessagingException {
        Mail mail = new FakeMail();
        ArrayList<MailAddress> a = new ArrayList<MailAddress>(recipients.length);
        for (int i = 0; i < recipients.length; i++) {
            a.add(new MailAddress(recipients[i]));
        }
        mail.setRecipients(a);
        mail.setMessage(new FakeMimeMessage());
        return mail;
    }
}
