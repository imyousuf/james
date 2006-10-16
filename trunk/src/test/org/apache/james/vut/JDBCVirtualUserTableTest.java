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
    
    public void testStoreAndRetrieveWildCardAddressMapping() throws ErrorMappingException {
	    
        String user = "test";
        String user2 = "test2";
        String domain = "localhost";
        String address = "test@localhost2";
        String address2 = "test@james";


       try {
                 
            assertTrue("No mapping",virtualUserTable.getMappings(user, domain).isEmpty());
        
            assertTrue("Added virtual mapping", virtualUserTable.addAddressMapping(null, domain, address));
            assertTrue("Added virtual mapping", virtualUserTable.addAddressMapping(user, domain, address2));

          
            assertTrue("One mappings",virtualUserTable.getMappings(user, domain).size() == 1);
            assertTrue("One mappings",virtualUserTable.getMappings(user2, domain).size() == 1);
           
            assertTrue("remove virtual mapping", virtualUserTable.removeAddressMapping(user, domain, address2));
            assertTrue("remove virtual mapping", virtualUserTable.removeAddressMapping(null, domain, address));
            assertTrue("No mapping",virtualUserTable.getMappings(user, domain).isEmpty());
            assertTrue("No mapping",virtualUserTable.getMappings(user2, domain).isEmpty());
      
        } catch (InvalidMappingException e) {
           fail("Storing failed");
        }
    
    }
    
}
