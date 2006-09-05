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



package org.apache.james.services;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.james.management.BayesianAnalyzerManagementException;

public interface BayesianAnalyzerManagementService {

    public static final String ROLE = "org.apache.james.services.BayesianAnalyzerManagementService";
    
    /**
     * Feed the BayesianAnalyter with spam. The given directory  contain the mail files
     * 
     * @param dir The directory in which the spam is located
     * @return count The count of added spam
     * @throws FileNotFoundException If the directory not exists
     * @throws IllegalArgumentException If the provided directory is not valid
     * @throws IOException 
     * @throws SQLException
     * @throws BayesianAnalyzerManagementException If the service is not configured
     */
    public int addSpamFromDir(String dir) throws BayesianAnalyzerManagementException;
    
    /**
     * Feed the BayesianAnalyzer with ham. The given directory  contain the mail files
     * 
     * @param dir The directory in which the ham is located
     * @return count The count of added ham
     * @throws FileNotFoundException If the directory not exists
     * @throws IllegalArgumentException If the provided directory is not valid
     * @throws IOException 
     * @throws SQLException
     * @throws BayesianAnalyzerManagementException If the service is not configured
     */
    public int addHamFromDir(String dir) throws  BayesianAnalyzerManagementException;
    
    /**
     * Feed the BayesianAnalyzer with ham. The given file must be a valid mbox file
     * 
     * @param file The mbox file
     * @return count The count of added ham
     * @throws FileNotFoundException If the directory not exists
     * @throws IllegalArgumentException If the provided file is not a valid mbox file
     * @throws IOException 
     * @throws SQLException
     * @throws BayesianAnalyzerManagementException If the service is not configured
     */
    public int addSpamFromMbox(String file) throws  BayesianAnalyzerManagementException;
    
    /**
     * Feed the BayesianAnalyzer with ham. The given file must be a valid mbox file
     * 
     * @param file The mbox file
     * @return count The count of added ham
     * @throws FileNotFoundException If the directory not exists
     * @throws IllegalArgumentException If the provided file is not a valid mbox file
     * @throws IOException 
     * @throws SQLException
     * @throws BayesianAnalyzerManagementException If the service is not configured
     */
    public int addHamFromMbox(String file) throws BayesianAnalyzerManagementException;

    /**
     * Export the data to a xml file
     * 
     * @param file The filename to store the data
     * @throws IOException 
     * @throws BayesianAnalyzerManagementException If the service is not configured
     * @throws SQLException
     */
    public void exportData(String file) throws BayesianAnalyzerManagementException;
    
    /**
     * Import the data from a xml file
     * 
     * @param file The filename to export data from
     * 
     * @throws IOException
     * @throws BayesianAnalyzerManagementException IF the service is not configured
     * @throws SQLException
     */
    public void importData(String file) throws  BayesianAnalyzerManagementException;
}
