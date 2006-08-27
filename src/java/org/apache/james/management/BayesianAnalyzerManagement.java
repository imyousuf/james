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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;

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

/**
 * Management for BayesianAnalyzer
 */
public class BayesianAnalyzerManagement implements BayesianAnalyzerManagementService, Serviceable, Initializable, Contextualizable, Configurable {

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
    public int addHamFromDir(String dir) throws FileNotFoundException, IllegalArgumentException, IOException, SQLException, BayesianAnalyzerManagementException {
        if (repos == null) throw new BayesianAnalyzerManagementException("RepositoryPath not configured");
        
        return feedBayesianAnalyzerFromDir(dir,HAM);
    }

    /**
     * @see org.apache.james.services.BayesianAnalyzerManagementService#addSpamFromDir(String)
     */
    public int addSpamFromDir(String dir) throws FileNotFoundException, IllegalArgumentException, IOException, SQLException, BayesianAnalyzerManagementException {
        if (repos == null) throw new BayesianAnalyzerManagementException("RepositoryPath not configured");
        
        return feedBayesianAnalyzerFromDir(dir,SPAM);
    }
    
    /**
     * @see org.apache.james.services.BayesianAnalyzerManagementService#addHamFromMbox(String)
     */
    public int addHamFromMbox(String file) throws FileNotFoundException, IllegalArgumentException, IOException, SQLException, BayesianAnalyzerManagementException {
    if (repos == null) throw new BayesianAnalyzerManagementException("RepositoryPath not configured");
    return feedBayesianAnalyzerFromMbox(file,HAM);
    }

    /**
     * @see org.apache.james.services.BayesianAnalyzerManagementService#addSpamFromMbox(String)
     */
    public int addSpamFromMbox(String file) throws FileNotFoundException, IllegalArgumentException, IOException, SQLException, BayesianAnalyzerManagementException {
    if (repos == null) throw new BayesianAnalyzerManagementException("RepositoryPath not configured");
    return feedBayesianAnalyzerFromMbox(file,SPAM);
    }

    /**
     * Helper method to train the BayesianAnalysis from directory which contain mails
     *
     * @param dir The directory which contains the emails which should be used to feed the BayesianAnalysis
     * @param type The type to train. HAM or SPAM
     * @return count The count of trained messages
     * @throws IOException 
     * @throws FileNotFoundException 
     * @throws SQLException 
     * @throws IllegalArgumentException Get thrown if the directory is not valid
     */
    private int feedBayesianAnalyzerFromDir(String dir, String type) throws FileNotFoundException, IOException, SQLException, IllegalArgumentException {
    
        //Clear out any existing word/counts etc..
        analyzer.clear();
        
        File tmpFile = new File(dir);
        int count = 0;
        
        synchronized(JDBCBayesianAnalyzer.DATABASE_LOCK) {

            // check if the provided dir is really a directory
            if (tmpFile.isDirectory()) {
                File[] files = tmpFile.listFiles();
        
                for (int i = 0; i < files.length; i++) {
                    if (type.equalsIgnoreCase(HAM)) {
                        analyzer.addHam(new BufferedReader(new FileReader(files[i])));
                        count++;
                    } else if (type.equalsIgnoreCase(SPAM)) {
                        analyzer.addSpam(new BufferedReader(new FileReader(files[i])));
                        count++;
                    }  
                }
              
                //Update storage statistics.
                if (type.equalsIgnoreCase(HAM)) {
                    analyzer.updateHamTokens(component.getConnection());
                } else if (type.equalsIgnoreCase(SPAM)) {
                    analyzer.updateSpamTokens(component.getConnection());
                } 
    
            } else {
               throw new IllegalArgumentException("Please provide an valid directory");
            }
        }
        
        return count;
    }
    

    /**
     * Helper method to train the BayesianAnalysis from mbox file
     *
     * @param mboxFile The mbox file
     * @param type The type to train. HAM or SPAM
     * @return count The count of trained messages
     * @throws IOException 
     * @throws FileNotFoundException 
     * @throws SQLException 
     * @throws IllegalArgumentException Get thrown if the file is not a valid mbox file
     */
    private int feedBayesianAnalyzerFromMbox(String mboxFile, String type) throws FileNotFoundException, IOException, SQLException, IllegalArgumentException {
        int count = 0;
        
        //Clear out any existing word/counts etc..
        analyzer.clear();
        
        File tmpFile = new File(mboxFile);
        
        if (MboxFile.isValid(tmpFile) == true ) { 
            MboxFile mbox = new MboxFile(tmpFile,MboxFile.READ_ONLY);
        
            synchronized(JDBCBayesianAnalyzer.DATABASE_LOCK) {
                for (int i = 0; i < mbox.getMessageCount(); i++) {
                    if (type.equalsIgnoreCase(HAM)) {
                
                        analyzer.addHam(new BufferedReader(new InputStreamReader(mbox.getMessageAsStream(i))));
                        count++;
                    } else if (type.equalsIgnoreCase(SPAM)) {
                        analyzer.addSpam(new BufferedReader(new InputStreamReader(mbox.getMessageAsStream(i))));
                        count++;
                    }  
                }
              
                //Update storage statistics.
                if (type.equalsIgnoreCase(HAM)) {
                    analyzer.updateHamTokens(component.getConnection());
                } else if (type.equalsIgnoreCase(SPAM)) {
                    analyzer.updateSpamTokens(component.getConnection());
                } 
            }
        } else {
            throw new IllegalArgumentException("Please provide an valid mbox file");
        }
        
        return count;
    }
    
    private JDBCBayesianAnalyzer analyzer = new JDBCBayesianAnalyzer() {
        protected void delegatedLog(String logString) {
            // no logging
        }
    };    
}
