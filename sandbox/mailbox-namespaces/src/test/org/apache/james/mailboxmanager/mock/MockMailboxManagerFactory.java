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

package org.apache.james.mailboxmanager.mock;

import java.util.HashSet;
import java.util.Set;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.manager.MailboxManager;
import org.apache.james.mailboxmanager.manager.MailboxManagerFactory;
import org.apache.james.services.User;

public class MockMailboxManagerFactory implements MailboxManagerFactory, Configurable, Initializable {

    public Configuration configuration;
    
    public Set mountPoints = new HashSet() ;
    
    public int init=0;

    public void deleteEverything() throws MailboxManagerException {
    }

    public MailboxManager getMailboxManagerInstance(User user) throws MailboxManagerException {
        return null;
    }

    public void configure(Configuration configuration) throws ConfigurationException {
        this.configuration=configuration;
        
    }


    public void addMountPoint(String point) {
        mountPoints.add(point);
    }

    public void initialize() throws Exception {
        init++;
    }

}
