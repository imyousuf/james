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

package org.apache.james.userrepository;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.commons.logging.impl.SimpleLog;
import org.apache.james.lifecycle.LifecycleUtil;
import org.apache.james.services.MockFileSystem;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.lib.MockUsersRepositoryTest;
import org.apache.james.util.TestUtil;

import java.util.Iterator;

import javax.sql.DataSource;

/**
 * Test basic behaviours of UsersFileRepository
 */
public class ListUsersJdbcRepositoryTest extends MockUsersRepositoryTest {

    /**
     * Create the repository to be tested.
     * 
     * @return the user repository
     * @throws Exception 
     */
    protected UsersRepository getUsersRepository() throws Exception {
        ListUsersJdbcRepository res = new ListUsersJdbcRepository();
        String tableString = "listusers";
        configureAbstractJdbcUsersRepository(res, tableString);
        return res;
    }

    protected boolean getPasswordsEnabled() {
        return false;
    }

    
    protected boolean getCheckCase() {
        return true;
    }

    /**
     * @param res
     * @param tableString
     * @throws Exception
     * @throws ConfigurationException
     */
    protected void configureAbstractJdbcUsersRepository(AbstractJdbcUsersRepository res, String tableString) throws Exception, ConfigurationException {
        res.setFileSystem(new MockFileSystem());
        DataSource dataSource = TestUtil.getDataSource();  
        res.setDatasource(dataSource );        
        DefaultConfigurationBuilder configuration = new DefaultConfigurationBuilder();
        configuration.addProperty("[@destinationURL]", "db://maildb/"+tableString);
        configuration.addProperty("sqlFile","file://conf/sqlResources.xml");
        res.setLog(new SimpleLog("MockLog"));
        res.configure(configuration);
        
        res.init();
    }

    protected void disposeUsersRepository() {
        Iterator<String> i = this.usersRepository.list();
        while (i.hasNext()) {
            this.usersRepository.removeUser((String) i.next());
        }
        LifecycleUtil.dispose(this.usersRepository);
    }

}
