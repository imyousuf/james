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

/**
 * Expose spool management functionality through JMX.
 *
 * @phoenix:mx-topic name="BayesianAnalyzerAdministration"
 */
public interface BayesianAnalyzerManagementMBean {

    /**
     * adds data to existing corpus by importing all the mails contained in the given dir as ham
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description adds data to existing corpus by importing all the mails contained in the given dir as ham
     *
     * @param dir full path to the directory containing mail files
     * @return number of processes mails
     *
     * @throws BayesianAnalyzerManagementException
     */
    int addHamFromDir(String dir) throws BayesianAnalyzerManagementException;

    /**
     * adds data to existing corpus by importing all the mails contained in the given dir as spam
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description adds data to existing corpus by importing all the mails contained in the given dir as spam
     *
     * @param dir full path to the directory containing mail files
     * @return number of processes mails
     *
     * @throws BayesianAnalyzerManagementException
     */
    int addSpamFromDir(String dir) throws BayesianAnalyzerManagementException;

    /**
     * adds data to existing corpus by importing all the mails contained in the given mbox as ham
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description adds data to existing corpus by importing all the mails contained in the given mbox as ham
     *
     * @param file path to the mbox file
     * @return number of processes mails
     *
     * @throws BayesianAnalyzerManagementException
     */
    int addHamFromMbox(String file) throws BayesianAnalyzerManagementException;

    /**
     * adds data to existing corpus by importing all the mails contained in the given mbox as spam
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description adds data to existing corpus by importing all the mails contained in the given mbox as spam
     *
     * @param file path to the mbox file
     * @return number of processes mails
     *
     * @throws BayesianAnalyzerManagementException
     */
    int addSpamFromMbox(String file) throws BayesianAnalyzerManagementException;

    /**
     * exports the corpus to a file
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description exports the corpus to a file
     *
     * @param file path to the mbox file
     *
     * @throws BayesianAnalyzerManagementException
     */
    void exportData(String file) throws BayesianAnalyzerManagementException;

    /**
     * imports the corpus from a file
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description imports the corpus from a file
     *
     * @param file path to the mbox file
     *
     * @throws BayesianAnalyzerManagementException
     */
    void importData(String file) throws BayesianAnalyzerManagementException;
}
