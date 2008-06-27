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

import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.james.test.util.Util;

public class JamesTestConfiguration extends DefaultConfiguration {
    
    public JamesTestConfiguration() {
        super("James");
    }

    public void init() {

        //setAttribute("enabled", true);

        DefaultConfiguration serverNamesConfig = new DefaultConfiguration("servernames");
        serverNamesConfig.setAttribute("autodetect", false);
        serverNamesConfig.addChild(Util.getValuedConfiguration("servername", "localhost"));
        addChild(serverNamesConfig);

        DefaultConfiguration inboxRepositoryConfig = new DefaultConfiguration("inboxRepository");

        DefaultConfiguration repositoryConfig = new DefaultConfiguration("repository");
        repositoryConfig.setAttribute("destinationURL", "db://maildb/inbox/");
        repositoryConfig.setAttribute("type", "MAIL");
        inboxRepositoryConfig.addChild(repositoryConfig);

        addChild(inboxRepositoryConfig);
    }

}
