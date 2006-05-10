package org.apache.james.mailrepository;

import org.apache.avalon.cornerstone.blocks.datasources.DefaultDataSourceSelector;
import org.apache.avalon.cornerstone.services.datasources.DataSourceSelector;
import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.context.ContextException;
import org.apache.james.context.AvalonContextConstants;
import org.apache.james.core.MailImpl;
import org.apache.james.mailrepository.filepair.File_Persistent_Stream_Repository;
import org.apache.james.remotemanager.RemoteManagerTestConfiguration;
import org.apache.james.test.mock.avalon.MockContext;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.avalon.MockServiceManager;
import org.apache.james.test.mock.avalon.MockStore;
import org.apache.james.util.io.IOUtil;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.util.SharedFileInputStream;

import java.io.File;
import java.io.FileOutputStream;
//import java.lang.management.ManagementFactory;
//import java.lang.management.MemoryMXBean;
//import java.lang.management.MemoryUsage;
import java.util.Arrays;
import java.util.Properties;

import junit.framework.TestCase;

/**
 * test for the JDBCMailRepository
 */
public class JDBCMailRepositoryTest extends TestCase {

    protected JDBCMailRepository mailRepository;
    protected JDBCMailRepository mailRepository2;
    protected RemoteManagerTestConfiguration m_testConfiguration;
    private MockContext context;
    
    protected void setUp() throws Exception {
        context = new MockContext() {

            public Object get(Object object) throws ContextException {
                if (AvalonContextConstants.APPLICATION_HOME.equals(object)) {
                    return new File("./src");
                } else {
                    return null;
                }
            }
            
        };
        
        mailRepository = new JDBCMailRepository();
        mailRepository.enableLogging(new MockLogger());
        mailRepository.contextualize(context);
        DefaultConfiguration mr = new DefaultConfiguration("mailrepository");
        // db or dbfile doesn't change the result, the difference is in the presence of
        // the filestore configuration.
        mr.setAttribute("destinationURL","dbfile://maildb/testtable/testrepository");
        mr.addChild(new AttrValConfiguration("sqlFile","file://conf/sqlResources.xml"));
        //mr.addChild(new AttrValConfiguration("filestore","file://var/dbmail"));
        
        mailRepository.service(setUpServiceManager());
        mailRepository.configure(mr);
        mailRepository.initialize();

    
        mailRepository2 = new JDBCMailRepository();
        mailRepository2.enableLogging(new MockLogger());
        mailRepository2.contextualize(context);
        DefaultConfiguration mr2 = new DefaultConfiguration("mailrepository2");
        // db or dbfile doesn't change the result, the difference is in the presence of
        // the filestore configuration.
        mr2.setAttribute("destinationURL","dbfile://maildb/testtable/testrepository22");
        mr2.addChild(new AttrValConfiguration("sqlFile","file://conf/sqlResources.xml"));
        //mr.addChild(new AttrValConfiguration("filestore","file://var/dbmail"));
        
        mailRepository2.service(setUpServiceManager());
        mailRepository2.configure(mr2);
        mailRepository2.initialize();

    }
    
    public void testStoreAndRetrieve() throws Exception {
//        MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
//        mbean.gc();
//        MemoryUsage mu1 = mbean.getHeapMemoryUsage();
//        System.out.println("Memory1: "+mu1.getInit()+"|"+mu1.getUsed()+"|"+mu1.getCommitted()+"|"+mu1.getMax());
        
        String key = "1234";
        String key2 = "1235";
        MailImpl m = new MailImpl();
        m.setRecipients(Arrays.asList(new MailAddress[] {new MailAddress("test@test.com")}));
        m.setName(key);
//      SharedFileInputStream sbais = new SharedFileInputStream("\\dsfasdfasdfas.eml");
//        String inputName = "\\dsfasdfasdfas.eml";
        String inputName = "\\a.eml";
        SharedFileInputStream sbais = new SharedFileInputStream(inputName);
        MimeMessage mm = new MimeMessage(Session.getDefaultInstance(new Properties()),sbais);
        
        /*
        String filename1 = "\\temp1.tmp";
        FileOutputStream tempFile = new FileOutputStream(new File(filename1));
        mm.writeTo(tempFile);
        tempFile.close();
        */
        
//        mu1 = mbean.getHeapMemoryUsage();
//        System.out.println("Memory1: "+mu1.getInit()+"|"+mu1.getUsed()+"|"+mu1.getCommitted()+"|"+mu1.getMax());
//        mbean.gc();
//        mu1 = mbean.getHeapMemoryUsage();
//        System.out.println("Memory1b: "+mu1.getInit()+"|"+mu1.getUsed()+"|"+mu1.getCommitted()+"|"+mu1.getMax());
        
        mm.addHeader("TEST","MIAO");
        mm.setContent("TEST TEST TEST","text/plain");
        ///mm.saveChanges();

        m.setMessage(mm);
        mailRepository.store(m);
        
//        mu1 = mbean.getHeapMemoryUsage();
//        System.out.println("Memory2: "+mu1.getInit()+"|"+mu1.getUsed()+"|"+mu1.getCommitted()+"|"+mu1.getMax());
//        mbean.gc();
//        mu1 = mbean.getHeapMemoryUsage();
//        System.out.println("Memory2b: "+mu1.getInit()+"|"+mu1.getUsed()+"|"+mu1.getCommitted()+"|"+mu1.getMax());

        m.setName(key2);
        mailRepository.store(m);
        
//        mu1 = mbean.getHeapMemoryUsage();
//        System.out.println("Memory3: "+mu1.getInit()+"|"+mu1.getUsed()+"|"+mu1.getCommitted()+"|"+mu1.getMax());
//        mbean.gc();
//        mu1 = mbean.getHeapMemoryUsage();
//        System.out.println("Memory3b: "+mu1.getInit()+"|"+mu1.getUsed()+"|"+mu1.getCommitted()+"|"+mu1.getMax());

        
        Mail m2 = mailRepository.retrieve(key);
        
//        mu1 = mbean.getHeapMemoryUsage();
//        System.out.println("Memory4: "+mu1.getInit()+"|"+mu1.getUsed()+"|"+mu1.getCommitted()+"|"+mu1.getMax());
//        mbean.gc();
//        mu1 = mbean.getHeapMemoryUsage();
//        System.out.println("Memory4b: "+mu1.getInit()+"|"+mu1.getUsed()+"|"+mu1.getCommitted()+"|"+mu1.getMax());

        assertNotNull(m2);
        //TEMPassertEquals(key,m2.getName());
//        System.out.println("Miao: "+sbais.available());
        
        MimeMessage m3 = m2.getMessage();
        

//        mu1 = mbean.getHeapMemoryUsage();
//        System.out.println("Memory5: "+mu1.getInit()+"|"+mu1.getUsed()+"|"+mu1.getCommitted()+"|"+mu1.getMax());
//        mbean.gc();
//        mu1 = mbean.getHeapMemoryUsage();
//        System.out.println("Memory5b: "+mu1.getInit()+"|"+mu1.getUsed()+"|"+mu1.getCommitted()+"|"+mu1.getMax());

        String filename = "\\temp.tmp";

        System.out.println("Writing to file: "+filename);
        System.out.flush();
        
        FileOutputStream f = new FileOutputStream(new File(filename));
        m3.writeTo(f);
        
        System.out.println("File written.");
        System.out.flush();
        f.close();

//        mu1 = mbean.getHeapMemoryUsage();
//        System.out.println("Memory6: "+mu1.getInit()+"|"+mu1.getUsed()+"|"+mu1.getCommitted()+"|"+mu1.getMax());
//        mbean.gc();
//        mu1 = mbean.getHeapMemoryUsage();
//        System.out.println("Memory6b: "+mu1.getInit()+"|"+mu1.getUsed()+"|"+mu1.getCommitted()+"|"+mu1.getMax());

        SharedFileInputStream s1 = new SharedFileInputStream(inputName);
        SharedFileInputStream s2 = new SharedFileInputStream(filename);
        
        //TEMP assertTrue(IOUtil.contentEquals(s1,s2));

//        mu1 = mbean.getHeapMemoryUsage();
//        System.out.println("Memory7: "+mu1.getInit()+"|"+mu1.getUsed()+"|"+mu1.getCommitted()+"|"+mu1.getMax());
//        mbean.gc();
//        mu1 = mbean.getHeapMemoryUsage();
//        System.out.println("Memory7b: "+mu1.getInit()+"|"+mu1.getUsed()+"|"+mu1.getCommitted()+"|"+mu1.getMax());

        mailRepository.remove(key);
        
        m2 = mailRepository.retrieve(key);
        assertNull(m2);
        
        
        m2 = mailRepository.retrieve(key2);
        mailRepository2.store(m2);
        mailRepository.remove(key2);
        
        mailRepository2.retrieve(key2);
        mailRepository2.remove(key2);
        
        
        
//        mu1 = mbean.getHeapMemoryUsage();
//        System.out.println("Memory8: "+mu1.getInit()+"|"+mu1.getUsed()+"|"+mu1.getCommitted()+"|"+mu1.getMax());
//        mbean.gc();
//        mu1 = mbean.getHeapMemoryUsage();
//        System.out.println("Memory8b: "+mu1.getInit()+"|"+mu1.getUsed()+"|"+mu1.getCommitted()+"|"+mu1.getMax());

    }
    
    private class AttrValConfiguration extends DefaultConfiguration {
        
        public AttrValConfiguration(String name, String value) {
            super(name);
            //setName(name+"1");
            setValue(value);
        }
        
        public AttrValConfiguration(String name, String attrName, String value) {
            super(name);
            addChild(new AttrValConfiguration(attrName,value));
        }
        
    }

    private MockServiceManager setUpServiceManager() throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        DefaultDataSourceSelector dataSourceSelector = new DefaultDataSourceSelector();
        dataSourceSelector.enableLogging(new MockLogger());
        DefaultConfiguration dc = new DefaultConfiguration("database-connections");
        DefaultConfiguration ds = new DefaultConfiguration("data-source");
        ds.setAttribute("name","maildb");
        ds.setAttribute("class","org.apache.james.util.dbcp.JdbcDataSource");
        
        /* derby 
        ds.addChild(new AttrValConfiguration("driver","org.apache.derby.jdbc.EmbeddedDriver"));
        ds.addChild(new AttrValConfiguration("dburl","jdbc:derby:derbydb;create=true"));
        ds.addChild(new AttrValConfiguration("user","james"));
        ds.addChild(new AttrValConfiguration("password","james8en2"));
        */
        /* mysql */
        ds.addChild(new AttrValConfiguration("driver","com.mysql.jdbc.Driver"));
        // ds.addChild(new AttrValConfiguration("dburl","jdbc:mysql://elysium/james?autoReconnect=true&emulateLocators=true"));
        ds.addChild(new AttrValConfiguration("dburl","jdbc:mysql://192.168.0.33/james?autoReconnect=true&emulateLocators=true"));
        ds.addChild(new AttrValConfiguration("user","james"));
        ds.addChild(new AttrValConfiguration("password","james8en2"));
        /* */

        /* hsqldb
        ds.addChild(new AttrValConfiguration("driver","org.hsqldb.jdbcDriver"));
        ds.addChild(new AttrValConfiguration("dburl","jdbc:hsqldb:file:mydb;ifexists=false"));
        ds.addChild(new AttrValConfiguration("user","sa"));
        ds.addChild(new AttrValConfiguration("password",""));
        */
        
        /* postgresql
        ds.addChild(new AttrValConfiguration("driver","org.postgresql.Driver"));
        ds.addChild(new AttrValConfiguration("dburl","jdbc:postgresql://192.168.0.35:5432/postgres"));
        ds.addChild(new AttrValConfiguration("user","postgres"));
        ds.addChild(new AttrValConfiguration("password","pg8en2"));
        */
        
        /*
        ds.addChild(new AttrValConfiguration("driver","oracle.jdbc.OracleDriver"));
        ds.addChild(new AttrValConfiguration("dburl","jdbc:oracle:thin:@elysium:1521:ORA920"));
        ds.addChild(new AttrValConfiguration("user","NAO12"));
        ds.addChild(new AttrValConfiguration("password","NAO8EN2"));
        */
        ds.addChild(new AttrValConfiguration("max","20"));
        dc.addChild(ds);
        dataSourceSelector.configure(dc);
        dataSourceSelector.contextualize(context);
        dataSourceSelector.initialize();
        
        MockStore store = new MockStore();
        File_Persistent_Stream_Repository fs = new File_Persistent_Stream_Repository();
        fs.enableLogging(new MockLogger());
        DefaultConfiguration c = new DefaultConfiguration("temp");
        c.setAttribute("destinationURL","file://var/dbmail");
        fs.configure(c);
        fs.service(serviceManager);
        fs.contextualize(context);
        fs.initialize();
        store.add("dbmail",fs);
        serviceManager.put(Store.ROLE, store);
        serviceManager.put(DataSourceSelector.ROLE, dataSourceSelector);
        return serviceManager;
    }


    /**
     * Temporary method to improve debugging
     */
    public static void main(String[] args) throws Exception {
        JDBCMailRepositoryTest t = new JDBCMailRepositoryTest();
        t.setUp();
        t.testStoreAndRetrieve();
        t.tearDown();
    }

}
