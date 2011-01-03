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
import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.container.spring.context.JamesServerApplicationContext;

/**
 * Bootstraps James using a Spring container.
 */
public class Main {

    private static Log log = LogFactory.getLog(Main.class.getName());

    public static void main(String[] args) throws IOException {
        
        long start = Calendar.getInstance().getTimeInMillis();
        
        final JamesServerApplicationContext context = new JamesServerApplicationContext(new String[] { "context/james-server-context.xml" });
        context.registerShutdownHook();
        
        long end = Calendar.getInstance().getTimeInMillis();

        log.info("Apache James Server is successfully started in " + (end-start) + " milliseconds.");
        
    }

}
