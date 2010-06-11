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

import org.apache.avalon.cornerstone.services.datasources.DataSourceSelector;
import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.service.DefaultServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.james.mailrepository.filepair.File_Persistent_Stream_Repository;
import org.apache.james.services.FileSystem;
import org.apache.james.services.MailRepository;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.avalon.MockStore;
import org.apache.james.test.mock.james.MockFileSystem;
import org.apache.james.test.mock.util.AttrValConfiguration;
import org.apache.james.test.util.Util;

public class JDBCMailRepositoryTest extends AbstractMailRepositoryTest {

    /**
     * @return
     * @throws ServiceException
     * @throws ConfigurationException
     * @throws Exception
     */
    protected MailRepository getMailRepository() throws ServiceException, ConfigurationException, Exception {
        DefaultServiceManager serviceManager = new DefaultServiceManager();
        serviceManager.put(FileSystem.ROLE, new MockFileSystem());
        serviceManager.put(DataSourceSelector.ROLE, Util.getDataSourceSelector());
        JDBCMailRepository mr = new JDBCMailRepository();
        
        // only used for dbfile
        MockStore mockStore = new MockStore();
        File_Persistent_Stream_Repository file_Persistent_Stream_Repository = new File_Persistent_Stream_Repository();
        file_Persistent_Stream_Repository.service(serviceManager);
        file_Persistent_Stream_Repository.enableLogging(new MockLogger());
        DefaultConfiguration defaultConfiguration2 = new DefaultConfiguration("conf");
        defaultConfiguration2.setAttribute("destinationURL", "file://target/var/mr/testrepo");
        file_Persistent_Stream_Repository.configure(defaultConfiguration2);
        file_Persistent_Stream_Repository.initialize();
        mockStore.add("STREAM.mr", file_Persistent_Stream_Repository);
        serviceManager.put(Store.ROLE,mockStore);

        mr.enableLogging(new MockLogger());
        DefaultConfiguration defaultConfiguration = new DefaultConfiguration("ReposConf");
        defaultConfiguration.setAttribute("destinationURL","db://maildb/mr/testrepo");
        defaultConfiguration.addChild(new AttrValConfiguration("sqlFile","file://conf/sqlResources.xml"));
        defaultConfiguration.setAttribute("type","MAIL");
        mr.service(serviceManager);
        mr.configure(defaultConfiguration);
        mr.initialize();
        return mr;
    }
    
    protected String getType() {
        return "db";
    }

}

