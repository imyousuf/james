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

import java.util.Calendar;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.james.container.spring.context.JamesServerApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bootstraps James using a Spring container.
 */
public class Main implements Daemon {

    private static Logger log = LoggerFactory.getLogger(Main.class.getName());
    private JamesServerApplicationContext context;

    public static void main(String[] args) throws Exception {

        long start = Calendar.getInstance().getTimeInMillis();

        Main main = new Main();
        main.init(null);

        long end = Calendar.getInstance().getTimeInMillis();

        log.info("Apache James Server is successfully started in " + (end - start) + " milliseconds.");

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.commons.daemon.Daemon#destroy()
     */
    public void destroy() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.commons.daemon.Daemon#init(org.apache.commons.daemon.DaemonContext
     * )
     */
    public void init(DaemonContext arg0) throws Exception {
        context = new JamesServerApplicationContext(new String[] { "context/james-server-context.xml" });
        context.registerShutdownHook();
        context.start();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.commons.daemon.Daemon#start()
     */
    public void start() throws Exception {
        context.start();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.commons.daemon.Daemon#stop()
     */
    public void stop() throws Exception {
        if (context != null) {
            context.stop();
        }
    }

}
