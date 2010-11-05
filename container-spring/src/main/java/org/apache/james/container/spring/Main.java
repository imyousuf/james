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
package org.apache.james.container.spring;

import java.io.IOException;
import java.rmi.server.RMISocketFactory;

/**
 * Bootstraps James using a Spring container
 */
public class Main {

    public static void main(String[] args) throws IOException {
        // Make sure we can bind jmx sockets to a specific ipaddress
        // https://issues.apache.org/jira/browse/JAMES-1104
        RMISocketFactory.setSocketFactory(new RestrictingRMISocketFactory()); 
        
        final JamesServerApplicationContext context = new JamesServerApplicationContext(new String[] { "spring-beans.xml" });
        context.registerShutdownHook();
    }

}
