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

package org.apache.james;

/**
 * An interface to expose James management functionality through JMX.  At
 * the time of this writing, this interface is just an example.
 * 
 * @phoenix:mx-topic name="MainJAMESServerManagement"
 */
public interface JamesMBean {

    /**
     * Adds a user to this mail server.
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description Add a new user
     *
     * @param userName The name of the user being added
     * @param password The password of the user being added
     */
    boolean addUser(String userName, String password);
}
