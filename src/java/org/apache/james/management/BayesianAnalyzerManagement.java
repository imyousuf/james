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




package org.apache.james.management;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.InputStream;
import java.sql.SQLException;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import net.fortuna.mstor.data.MboxFile;

import org.apache.avalon.cornerstone.services.datasources.DataSourceSelector;
import org.apache.avalon.excalibur.datasource.DataSourceComponent;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.context.AvalonContextUtilities;
import org.apache.james.services.BayesianAnalyzerManagementService;
import org.apache.james.util.JDBCBayesianAnalyzer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Management for BayesianAnalyzer
 */
public class BayesianAnalyzerManagement implements BayesianAnalyzerManagementService, Serviceable, Initializable, Contextualizable, Configurable, BayesianAnalyzerManagementMBean {

    private final static String HAM = "HAM";
    private final static String SPAM = "SPAM";
    private DataSourceSelector selector;
    private DataSourceComponent component;
    private String repos;
    private Context context;
    private String sqlFileUrl = "file://conf/sqlResources.xml";
    
    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager)
     */
    public void service(ServiceManager arg0) throws ServiceException {
        DataSourceSelector selector = (DataSourceSelector) arg0.lookup(DataSourceSelector.ROLE);
        setDataSourceSelector(selector);
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {
        if (repos != null) {
            setDataSourceComponent((DataSourceComponent) selector.select(repos));
            File sqlFile = AvalonContextUtilities.getFile(context, sqlFileUrl);
            analyzer.initSqlQueries(component.getConnection(), sqlFile.getAbsolutePath());
        }
    }

    /**
     * @see org.apache.avalon.framework.context.Contextualizable#contextualize(Context)
     */
    public void contextualize(Context arg0) throws ContextException {
        this.context = arg0;
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration arg0) throws ConfigurationException {
        Configuration reposPath = arg0.getChild("repositoryPath",false);
        if (reposPath != null) {
            setRepositoryPath(reposPath.getValue());
        }
    }
    
    /**
     * Set the repository path 
     * 
     * @param repositoryPath Thre repositoryPath
     */
    public void setRepositoryPath(String repositoryPath) {
        repos = repositoryPath.substring(5);
    }
    
    /**
     * Set the DatasourceSekector
     * 
     * @param selector The DataSourceSelector
     */
    public void setDataSourceSelector (DataSourceSelector selector) {
        this.selector = selector;
    }
    
    /**
     * Set the DataSourceComponent
     * 
     * @param component The DataSourceComponent
     */
    public void setDataSourceComponent(DataSourceComponent component) {
        this.component = component;
    }
    
    /**
     * @see org.apache.james.services.BayesianAnalyzerManagementService#addHamFromDir(String)
     */
    public int addHamFromDir(String dir) throws BayesianAnalyzerManagementException {
        if (repos == null) throw new BayesianAnalyzerManagementException("RepositoryPath not configured");
        
        return feedBayesianAnalyzerFromDir(dir,HAM);
    }

    /**
     * @see org.apache.james.services.BayesianAnalyzerManagementService#addSpamFromDir(String)
     */
    public int addSpamFromDir(String dir) throws BayesianAnalyzerManagementException {
        if (repos == null) throw new BayesianAnalyzerManagementException("RepositoryPath not configured");
        
        return feedBayesianAnalyzerFromDir(dir,SPAM);
    }
    
    /**
     * @see org.apache.james.services.BayesianAnalyzerManagementService#addHamFromMbox(String)
     */
    public int addHamFromMbox(String file) throws BayesianAnalyzerManagementException {
        if (repos == null) throw new BayesianAnalyzerManagementException("RepositoryPath not configured");
        return feedBayesianAnalyzerFromMbox(file,HAM);
    }

    /**
     * @see org.apache.james.services.BayesianAnalyzerManagementService#addSpamFromMbox(String)
     */
    public int addSpamFromMbox(String file) throws BayesianAnalyzerManagementException {
        if (repos == null) throw new BayesianAnalyzerManagementException("RepositoryPath not configured");
        return feedBayesianAnalyzerFromMbox(file,SPAM);
    }

    /**
     * Helper method to train the BayesianAnalysis from directory which contain mails
     *
     * @param dir The directory which contains the emails which should be used to feed the BayesianAnalysis
     * @param type The type to train. HAM or SPAM
     * @return count The count of trained messages
     * @throws BayesianAnalyzerManagementException
     * @throws IllegalArgumentException Get thrown if the directory is not valid
     */
    private int feedBayesianAnalyzerFromDir(String dir, String type) throws BayesianAnalyzerManagementException {

        //Clear out any existing word/counts etc..
        analyzer.clear();

        File tmpFile = new File(dir);
        int count = 0;

        synchronized(JDBCBayesianAnalyzer.DATABASE_LOCK) {

            // check if the provided dir is really a directory
            if (tmpFile.isDirectory()) {
                File[] files = tmpFile.listFiles();

                for (int i = 0; i < files.length; i++) {
                    BufferedReader stream = null;
                    try {
                        stream = new BufferedReader(new FileReader(files[i]));
                    } catch (FileNotFoundException e) {
                        throw new BayesianAnalyzerManagementException("acessing mail file failed.", e);
                    }
                    addMailToCorpus(type, stream);
                    count++;
                }

                updateTokens(type);

            } else {
               throw new IllegalArgumentException("Please provide an valid directory");
            }
        }

        return count;
    }

    /**
     * Update the tokens 
     * 
     * @param type The type whichs tokens should be updated. Valid types are HAM or SPAM
     * @throws BayesianAnalyzerManagementException
     */
    private void updateTokens(String type) throws BayesianAnalyzerManagementException {
        //Update storage statistics.
        try {
            Connection connection = component.getConnection();
            if (type.equalsIgnoreCase(HAM)) {
                analyzer.updateHamTokens(connection);
            } else if (type.equalsIgnoreCase(SPAM)) {
                analyzer.updateSpamTokens(connection);
            }
        } catch (SQLException e) {
            throw new BayesianAnalyzerManagementException("updating tokens failed.", e);
        }
    }

    /**
     * Add mail to corpus 
     * 
     * @param type The type to add to corpus. Valid types are HAM or SPAM
     * @param stream The stream which is used to transfer the data
     * @throws BayesianAnalyzerManagementException
     */
    private void addMailToCorpus(String type, BufferedReader stream) throws BayesianAnalyzerManagementException {
        try {
            if (type.equalsIgnoreCase(HAM)) {
                analyzer.addHam(stream);
            } else if (type.equalsIgnoreCase(SPAM)) {
                analyzer.addSpam(stream);
            }
        } catch (IOException e) {
            throw new BayesianAnalyzerManagementException("adding to corpus failed.", e);
        }
    }


    /**
     * Helper method to train the BayesianAnalysis from mbox file
     *
     * @param mboxFile The mbox file
     * @param type The type to train. HAM or SPAM
     * @return count The count of trained messages
     * @throws BayesianAnalyzerManagementException
     */
    private int feedBayesianAnalyzerFromMbox(String mboxFile, String type) throws BayesianAnalyzerManagementException {
        int count = 0;

        //Clear out any existing word/counts etc..
        analyzer.clear();

        File tmpFile = new File(mboxFile);

        if (MboxFile.isValid(tmpFile)) {
            MboxFile mbox = new MboxFile(tmpFile,MboxFile.READ_ONLY);

            synchronized(JDBCBayesianAnalyzer.DATABASE_LOCK) {
                int messageCount = 0;
                try {
                    messageCount = mbox.getMessageCount();
                } catch (IOException e) {
                    throw new BayesianAnalyzerManagementException(e);
                }
                for (int i = 0; i < messageCount; i++) {
                    InputStream message = null;
                    try {
                        message = mbox.getMessageAsStream(i);
                    } catch (IOException e) {
                        throw new BayesianAnalyzerManagementException("could not access mail from mbox streanm", e);
                    }
                    BufferedReader stream = new BufferedReader(new InputStreamReader(message));
                    addMailToCorpus(type, stream);
                    count++;
                }

                //Update storage statistics.
                updateTokens(type);
            }
        } else {
            throw new IllegalArgumentException("Please provide an valid mbox file");
        }

        return count;
    }
    
    /**
     * @see org.apache.james.services.BayesianAnalyzerManagementService#exportData(String)
     */
    public void exportData(String file) throws BayesianAnalyzerManagementException {
        if (repos == null) throw new BayesianAnalyzerManagementException("RepositoryPath not configured");

        synchronized(JDBCBayesianAnalyzer.DATABASE_LOCK) {
            try {
                analyzer.loadHamNSpam(component.getConnection());
            } catch (SQLException e) {
                throw new BayesianAnalyzerManagementException("loading ham and spam failed.", e);
            }

            int hamMessageCount = analyzer.getHamMessageCount();
            int spamMessageCount = analyzer.getSpamMessageCount();
            Map hamTokenCounts = analyzer.getHamTokenCounts();
            Map spamTokenCounts = analyzer.getSpamTokenCounts();

            XStream xstream = new XStream(new DomDriver());
            xstream.alias("bayesianAnalyzer", BayesianAnalyzerXml.class);
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                throw new BayesianAnalyzerManagementException("opening export file failed", e);
            }
            PrintWriter printwriter = new PrintWriter(fileOutputStream);
            printwriter.println(xstream.toXML(new BayesianAnalyzerXml(hamMessageCount,spamMessageCount,hamTokenCounts,spamTokenCounts)));
            printwriter.close();
        }
    }
    
    /**
     * @see org.apache.james.services.BayesianAnalyzerManagementService#importData(String)
     */
    public void importData(String file) throws BayesianAnalyzerManagementException {
        if (repos == null) throw new BayesianAnalyzerManagementException("RepositoryPath not configured");

        synchronized(JDBCBayesianAnalyzer.DATABASE_LOCK){
            XStream xstream = new XStream(new DomDriver());

            FileReader fileReader = null;
            try {
                fileReader = new FileReader(file);
            } catch (FileNotFoundException e) {
                throw new BayesianAnalyzerManagementException("opening input file failed", e);
            }
            BayesianAnalyzerXml bAnalyzerXml = (BayesianAnalyzerXml) xstream.fromXML(fileReader);

            // clear old data
            analyzer.clear();
            analyzer.tokenCountsClear();

            //TODO: Drop old corpus in database;

            // add the new data
            analyzer.setHamMessageCount(bAnalyzerXml.getHamMessageCount());
            analyzer.setSpamMessageCount(bAnalyzerXml.getSpamMessageCount());
            analyzer.setHamTokenCounts(bAnalyzerXml.getHamTokenCounts());
            analyzer.setSpamTokenCounts(bAnalyzerXml.getSpamTokenCounts());
            updateTokens(HAM);
            updateTokens(SPAM);
        }

    }
    
    private JDBCBayesianAnalyzer analyzer = new JDBCBayesianAnalyzer() {
        protected void delegatedLog(String logString) {
            // no logging
        }
    };
    

    /**
     * @see org.apache.james.services.BayesianAnalyzerManagementService#resetData()
     */
    public void resetData() throws BayesianAnalyzerManagementException {
        synchronized(JDBCBayesianAnalyzer.DATABASE_LOCK) {
            try {
                analyzer.resetData(component.getConnection());
            } catch (SQLException e) {
                throw new BayesianAnalyzerManagementException(e.getMessage());
            }
        }
    
    }
    
    /**
     * Inner class to represent the data in an xml file
     */
    private static class BayesianAnalyzerXml {
        private int hamMessageCount = 0;
        private int spamMessageCount = 0;
        private Map hamTokenCounts = new HashMap();
        private Map spamTokenCounts = new HashMap();
    
        public BayesianAnalyzerXml(int hamMessageCount, int spamMessageCount, Map hamTokenCounts, Map spamTokenCounts) {
            this.hamMessageCount = hamMessageCount;
            this.spamMessageCount = spamMessageCount;
            this.hamTokenCounts = hamTokenCounts;
            this.spamTokenCounts = spamTokenCounts;
        }
    
        public int getHamMessageCount() {
            return hamMessageCount;
        }
    
        public int getSpamMessageCount() {
            return spamMessageCount;
        }
    
        public Map getHamTokenCounts() {
            return hamTokenCounts;
        }
    
        public Map getSpamTokenCounts() {
            return spamTokenCounts;
        }
    
    }

}
