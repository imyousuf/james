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

import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.logger.ConsoleLogger;
import org.apache.james.services.UsersRepository;
import org.apache.james.test.mock.james.MockFileSystem;
import org.apache.james.test.mock.util.AttrValConfiguration;
import org.apache.james.test.util.Util;

import java.util.Iterator;

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
        res.setDatasources(Util.getDataSourceSelector());
        
        DefaultConfiguration configuration = new DefaultConfiguration("test");
        configuration.setAttribute("destinationURL", "db://maildb/"+tableString);
        configuration.addChild(new AttrValConfiguration("sqlFile","file://conf/sqlResources.xml"));
        res.enableLogging(new ConsoleLogger());
        res.configure(configuration );
        res.initialize();
    }

    protected void disposeUsersRepository() {
        Iterator i = this.usersRepository.list();
        while (i.hasNext()) {
            this.usersRepository.removeUser((String) i.next());
        }
        ContainerUtil.dispose(this.usersRepository);
    }

}
