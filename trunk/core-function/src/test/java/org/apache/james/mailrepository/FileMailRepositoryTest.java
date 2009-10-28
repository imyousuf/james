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
import org.apache.avalon.framework.service.ServiceException;
import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.commons.logging.impl.SimpleLog;
import org.apache.james.mailrepository.filepair.File_Persistent_Object_Repository;
import org.apache.james.mailrepository.filepair.File_Persistent_Stream_Repository;
import org.apache.james.services.MailRepository;
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
        File_Persistent_Stream_Repository file_Persistent_Stream_Repository = new File_Persistent_Stream_Repository();
        file_Persistent_Stream_Repository.setFileSystem(fs);
        file_Persistent_Stream_Repository.setLogger(new SimpleLog("MockLog"));
        
        DefaultConfigurationBuilder defaultConfiguration2 = new DefaultConfigurationBuilder();
        defaultConfiguration2.addProperty( "[@destinationURL]", "file://target/var/mr");
        file_Persistent_Stream_Repository.setConfiguration(defaultConfiguration2);
        file_Persistent_Stream_Repository.init();
        
        mockStore.add("STREAM.mr", file_Persistent_Stream_Repository);
        File_Persistent_Object_Repository file_Persistent_Object_Repository = new File_Persistent_Object_Repository();
        file_Persistent_Object_Repository.setFileSystem(fs);
        file_Persistent_Object_Repository.setLogger(new SimpleLog("MockLog"));
        DefaultConfigurationBuilder defaultConfiguration22 = new DefaultConfigurationBuilder();
        defaultConfiguration22.addProperty( "[@destinationURL]", "file://target/var/mr");
        file_Persistent_Object_Repository.setConfiguration(defaultConfiguration22);
        file_Persistent_Object_Repository.init();
        mockStore.add("OBJECT.mr", file_Persistent_Object_Repository);
        mr.setStore(mockStore);

        mr.setLogger(new SimpleLog("MockLog"));
        DefaultConfigurationBuilder defaultConfiguration = new DefaultConfigurationBuilder();
        defaultConfiguration.addProperty( "[@destinationURL]","file://target/var/mr");
        defaultConfiguration.addProperty( "[@type]","MAIL");
        mr.setConfiguration(defaultConfiguration);
        mr.init();
        return mr;
    }

}

