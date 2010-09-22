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

import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.commons.logging.impl.SimpleLog;
import org.apache.james.filepair.FilePersistentObjectRepository;
import org.apache.james.filepair.FilePersistentStreamRepository;
import org.apache.james.test.mock.avalon.MockStore;
import org.apache.james.test.mock.james.MockFileSystem;

public class FileMailRepositoryTest extends AbstractMailRepositoryTest {

    /**
     * @return
     * @throws ServiceException
     * @throws ConfigurationException
     * @throws Exception
     */
    protected MailRepository getMailRepository() throws Exception {
        MockFileSystem fs =  new MockFileSystem();
        FileMailRepository mr = new FileMailRepository();
        MockStore mockStore = new MockStore();
        FilePersistentStreamRepository file_Persistent_Stream_Repository = new FilePersistentStreamRepository();
        file_Persistent_Stream_Repository.setFileSystem(fs);
        file_Persistent_Stream_Repository.setLog(new SimpleLog("MockLog"));
        
        DefaultConfigurationBuilder defaultConfiguration2 = new DefaultConfigurationBuilder();
        defaultConfiguration2.addProperty( "[@destinationURL]", "file://target/var/mr");
        file_Persistent_Stream_Repository.configure(defaultConfiguration2);
        file_Persistent_Stream_Repository.init();
        
        mockStore.add("STREAM.mr", file_Persistent_Stream_Repository);
        FilePersistentObjectRepository file_Persistent_Object_Repository = new FilePersistentObjectRepository();
        file_Persistent_Object_Repository.setFileSystem(fs);
        file_Persistent_Object_Repository.setLog(new SimpleLog("MockLog"));
        DefaultConfigurationBuilder defaultConfiguration22 = new DefaultConfigurationBuilder();
        defaultConfiguration22.addProperty( "[@destinationURL]", "file://target/var/mr");
        file_Persistent_Object_Repository.configure(defaultConfiguration22);
        file_Persistent_Object_Repository.init();
        mockStore.add("OBJECT.mr", file_Persistent_Object_Repository);
        mr.setStore(mockStore);

        mr.setLog(new SimpleLog("MockLog"));
        DefaultConfigurationBuilder defaultConfiguration = new DefaultConfigurationBuilder();
        defaultConfiguration.addProperty( "[@destinationURL]","file://target/var/mr");
        defaultConfiguration.addProperty( "[@type]","MAIL");
        mr.configure(defaultConfiguration);
        mr.init();
        return mr;
    }

}

