package org.apache.james.vut;

import org.apache.avalon.cornerstone.services.datasources.DataSourceSelector;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.service.DefaultServiceManager;
import org.apache.avalon.framework.service.ServiceException;

import org.apache.james.services.FileSystem;

import org.apache.james.services.VirtualUserTableManagement;
import org.apache.james.test.mock.avalon.MockLogger;

import org.apache.james.test.mock.james.MockFileSystem;
import org.apache.james.test.mock.util.AttrValConfiguration;
import org.apache.james.test.util.Util;

public class JDBCVirtualUserTableTest extends AbstractVirtualUserTableTest {
    
    protected VirtualUserTableManagement getVirtalUserTable() throws ServiceException, ConfigurationException, Exception {
        DefaultServiceManager serviceManager = new DefaultServiceManager();
        serviceManager.put(FileSystem.ROLE, new MockFileSystem());
        serviceManager.put(DataSourceSelector.ROLE, Util.getDataSourceSelector());
        JDBCVirtualUserTable mr = new JDBCVirtualUserTable();
        

        mr.enableLogging(new MockLogger());
        DefaultConfiguration defaultConfiguration = new DefaultConfiguration("ReposConf");
        defaultConfiguration.addChild(new AttrValConfiguration("repositoryPath","db://maildb"));
        defaultConfiguration.addChild(new AttrValConfiguration("sqlFile","file://conf/sqlResources.xml"));
        mr.service(serviceManager);
        mr.configure(defaultConfiguration);
        mr.initialize();
        return mr;
    }
}
