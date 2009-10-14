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
package org.apache.james.container.spring.beanfactory;

/**
 * content of a "provide" XML element from an assembly file.
 * occurs within a block element like this:
 *     <provide name="domainlist" role="org.apache.james.api.domainlist.DomainList"/>
 *
 */
public class AvalonServiceReference {
    
    private String name;
    private String rolename; // in James, this is the service interface name (per convention)

    public AvalonServiceReference(String name, String rolename) {
        this.name = name;
        this.rolename = rolename;
    }

    public String getName() {
        return name;
    }

    public String getRolename() {
        return rolename;
    }
}
