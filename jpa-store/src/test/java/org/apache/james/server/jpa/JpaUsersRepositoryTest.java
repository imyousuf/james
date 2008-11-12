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

package org.apache.james.server.jpa;

import java.util.HashMap;

import javax.persistence.PersistenceException;

import org.apache.james.api.user.UsersRepository;
import org.apache.james.userrepository.MockUsersRepositoryTest;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAEntityTransaction;
import org.apache.openjpa.persistence.OpenJPAPersistence;

public class JpaUsersRepositoryTest extends MockUsersRepositoryTest {

    private HashMap<String, String> properties;
    private OpenJPAEntityManagerFactory factory;
    private OpenJPAEntityManager manager;

    @Override
    protected void setUp() throws Exception {
        properties = new HashMap<String, String>();
        properties.put("openjpa.ConnectionDriverName", "org.h2.Driver");
        properties.put("openjpa.ConnectionURL", "jdbc:h2:target/users/db");
        properties.put("openjpa.Log", "JDBC=WARN, SQL=WARN, Runtime=WARN");
        properties.put("openjpa.ConnectionFactoryProperties", "PrettyPrint=true, PrettyPrintLineLength=72");
        properties.put("openjpa.jdbc.SynchronizeMappings", "buildSchema(ForeignKeys=true)");
        properties.put("openjpa.MetaDataFactory", "jpa(Types=org.apache.james.server.jpa.JPAUser)");
        super.setUp();
        deleteAll();
    }

    @Override
    protected void tearDown() throws Exception {
        deleteAll();
        super.tearDown();
        if (manager != null)
        {
            manager.close();
        }
        if (factory != null)
        {
            factory.close();
        }
    }
    
    private void deleteAll() {
        try
        {
            OpenJPAEntityManager manager = factory.createEntityManager();
            final OpenJPAEntityTransaction transaction = manager.getTransaction();
            transaction.begin();
            manager.createQuery("DELETE FROM User user").executeUpdate();
            transaction.commit();
        } catch (PersistenceException e) {
            e.printStackTrace();
        }
    }

    protected UsersRepository getUsersRepository() throws Exception 
    {
        factory = OpenJPAPersistence.getEntityManagerFactory(properties);
        manager = factory.createEntityManager();
        return new JPAUsersRepository(manager);
    }
}
