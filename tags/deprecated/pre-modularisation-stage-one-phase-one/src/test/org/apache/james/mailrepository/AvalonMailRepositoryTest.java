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

import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.service.DefaultServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.james.mailrepository.filepair.File_Persistent_Object_Repository;
import org.apache.james.mailrepository.filepair.File_Persistent_Stream_Repository;
import org.apache.james.services.FileSystem;
import org.apache.james.services.MailRepository;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.avalon.MockStore;
import org.apache.james.test.mock.james.MockFileSystem;

public class AvalonMailRepositoryTest extends AbstractMailRepositoryTest {

    /**
     * @return
     * @throws ServiceException
     * @throws ConfigurationException
     * @throws Exception
     */
    protected MailRepository getMailRepository() throws ServiceException, ConfigurationException, Exception {
        DefaultServiceManager serviceManager = new DefaultServiceManager();
        serviceManager.put(FileSystem.ROLE, new MockFileSystem());
        AvalonMailRepository mr = new AvalonMailRepository();
        MockStore mockStore = new MockStore();
        File_Persistent_Stream_Repository file_Persistent_Stream_Repository = new File_Persistent_Stream_Repository();
        file_Persistent_Stream_Repository.service(serviceManager);
        file_Persistent_Stream_Repository.enableLogging(new MockLogger());
        DefaultConfiguration defaultConfiguration2 = new DefaultConfiguration("conf");
        defaultConfiguration2.setAttribute("destinationURL", "file://target/var/mr");
        file_Persistent_Stream_Repository.configure(defaultConfiguration2);
        file_Persistent_Stream_Repository.initialize();
        mockStore.add("STREAM.mr", file_Persistent_Stream_Repository);
        File_Persistent_Object_Repository file_Persistent_Object_Repository = new File_Persistent_Object_Repository();
        file_Persistent_Object_Repository.service(serviceManager);
        file_Persistent_Object_Repository.enableLogging(new MockLogger());
        DefaultConfiguration defaultConfiguration22 = new DefaultConfiguration("conf");
        defaultConfiguration22.setAttribute("destinationURL", "file://target/var/mr");
        file_Persistent_Object_Repository.configure(defaultConfiguration22);
        file_Persistent_Object_Repository.initialize();
        mockStore.add("OBJECT.mr", file_Persistent_Object_Repository);
        mr.setStore(mockStore);

        mr.enableLogging(new MockLogger());
        DefaultConfiguration defaultConfiguration = new DefaultConfiguration("ReposConf");
        defaultConfiguration.setAttribute("destinationURL","file://target/var/mr");
        defaultConfiguration.setAttribute("type","MAIL");
        mr.configure(defaultConfiguration);
        mr.initialize();
        return mr;
    }

}

