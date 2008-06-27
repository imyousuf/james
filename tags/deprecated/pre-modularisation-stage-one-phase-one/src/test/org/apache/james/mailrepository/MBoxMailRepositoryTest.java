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


package org.apache.james.mailrepository;

import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.james.services.MailRepository;
import org.apache.james.test.mock.avalon.MockLogger;

/**
 * NOTE this test *WAS* disabled because MBoxMailRepository does not
 * currently support most simple operations for the MailRepository interface.
 * 
 * NOTE this previously extended AbstractMailRepositoryTest to run all of the
 * common mail repository tests on the MBox implementation.
 */
public class MBoxMailRepositoryTest extends TestCase {

    
    protected MailRepository getMailRepository() throws ServiceException, ConfigurationException, Exception {
        MBoxMailRepository mr = new MBoxMailRepository();

        mr.enableLogging(new MockLogger());
        DefaultConfiguration defaultConfiguration = new DefaultConfiguration("ReposConf");
        defaultConfiguration.setAttribute("destinationURL","mbox://src/test/org/apache/james/mailrepository/testdata/Inbox");
        defaultConfiguration.setAttribute("type","MAIL");
        mr.configure(defaultConfiguration);
        return mr;
    }

    // Try to write a unit test for JAMES-744. At the moment it seems that we cannot reproduce it.
    public void testReadMboxrdFile() throws ServiceException, ConfigurationException, Exception {
        MailRepository mr = getMailRepository();
    
        Iterator keys = mr.list();
    
        assertTrue("Two messages in list", keys.hasNext());
        keys.next();
    
        assertTrue("One messages in list", keys.hasNext());
        keys.next();
    
        assertFalse("No messages", keys.hasNext());
    }

    /*
    public void runBare() throws Throwable {
        System.err.println("TEST DISABLED!");
        // Decomment this or remove this method to re-enable the MBoxRepository testing
        // super.runBare();
    }
    */

}

