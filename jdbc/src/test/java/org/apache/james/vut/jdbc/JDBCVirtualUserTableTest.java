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
package org.apache.james.vut.jdbc;

import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.derby.jdbc.EmbeddedDriver;
import org.apache.james.filesystem.api.mock.MockFileSystem;
import org.apache.james.vut.api.VirtualUserTableException;
import org.apache.james.vut.jdbc.JDBCVirtualUserTable;
import org.apache.james.vut.lib.AbstractVirtualUserTable;
import org.apache.james.vut.lib.AbstractVirtualUserTableTest;
import org.slf4j.LoggerFactory;

/**
 * Test the JDBC Virtual User Table implementation.
 */
public class JDBCVirtualUserTableTest extends AbstractVirtualUserTableTest {

    /**
     * @see org.apache.james.vut.lib.AbstractVirtualUserTableTest#getVirtualUserTable()
     */
    protected AbstractVirtualUserTable getVirtualUserTable() throws Exception {
        JDBCVirtualUserTable virtualUserTable = new JDBCVirtualUserTable();
        virtualUserTable.setLog(LoggerFactory.getLogger("MockLog"));
        virtualUserTable.setDataSource(getDataSource());
        virtualUserTable.setFileSystem(new MockFileSystem());
        DefaultConfigurationBuilder defaultConfiguration = new DefaultConfigurationBuilder();
        defaultConfiguration.addProperty("[@destinationURL]", "db://maildb/VirtualUserTable");
        defaultConfiguration.addProperty("sqlFile", "file://conf/sqlResources.xml");
        virtualUserTable.configure(defaultConfiguration);
        virtualUserTable.init();
        return virtualUserTable;
    }

    private BasicDataSource getDataSource() {
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName(EmbeddedDriver.class.getName());
        ds.setUrl("jdbc:derby:target/testdb;create=true");
        ds.setUsername("james");
        ds.setPassword("james");
        return ds;
    }

    /**
     * @see org.apache.james.vut.lib.AbstractVirtualUserTableTest#addMapping(java.lang.String,
     *      java.lang.String, java.lang.String, int)
     */
    protected boolean addMapping(String user, String domain, String mapping, int type) throws VirtualUserTableException {
        try {
            if (type == ERROR_TYPE) {
                virtualUserTable.addErrorMapping(user, domain, mapping);
            } else if (type == REGEX_TYPE) {
                virtualUserTable.addRegexMapping(user, domain, mapping);
            } else if (type == ADDRESS_TYPE) {
                virtualUserTable.addAddressMapping(user, domain, mapping);
            } else if (type == ALIASDOMAIN_TYPE) {
                virtualUserTable.addAliasDomainMapping(domain, mapping);
            } else {
                return false;
            }
        } catch (VirtualUserTableException ex) {
            return false;
        }
        return true;
    }

    /**
     * @see org.apache.james.vut.lib.AbstractVirtualUserTableTest#removeMapping(java.lang.String,
     *      java.lang.String, java.lang.String, int)
     */
    protected boolean removeMapping(String user, String domain, String mapping, int type) throws VirtualUserTableException {
        try {

            if (type == ERROR_TYPE) {
                virtualUserTable.removeErrorMapping(user, domain, mapping);
            } else if (type == REGEX_TYPE) {
                virtualUserTable.removeRegexMapping(user, domain, mapping);
            } else if (type == ADDRESS_TYPE) {
                virtualUserTable.removeAddressMapping(user, domain, mapping);
            } else if (type == ALIASDOMAIN_TYPE) {
                virtualUserTable.removeAliasDomainMapping(domain, mapping);
            } else {
                return false;
            }
        } catch (VirtualUserTableException ex) {
            return false;
        }
        return true;
    }

}
