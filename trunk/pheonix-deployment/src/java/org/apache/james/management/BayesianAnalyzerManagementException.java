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
 * The Exception get thrown if an error accour in BayesianAnalyzerManagment
 */
public class BayesianAnalyzerManagementException extends ManagementException {

    /**
     * @see java.lang.Exception#Exception()
     */
    public BayesianAnalyzerManagementException() {
        super();
    }

    /**
     * @see java.lang.Exception#Exception(String)
     */
    public BayesianAnalyzerManagementException(String message) {
        super(message);
    }

    /**
     * @see java.lang.Exception#Exception(Throwable)
     */
    public BayesianAnalyzerManagementException(Exception e) {
        super(e);
    }

    /**
     * @see java.lang.Exception#Exception(String, Throwable)
     */
    public BayesianAnalyzerManagementException(String message, Exception e) {
        super(message, e);
    }

}
