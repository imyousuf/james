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
package org.apache.james.container.osgi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.container.spring.lifecycle.api.LogProvider;


/**
 * Return {@link Log} for using within OSGI container
 *
 */
public class OsgiLogProvider implements LogProvider{

    /*
     * (non-Javadoc)
     * @see org.apache.james.container.spring.lifecycle.LogProvider#getLog(java.lang.String)
     */
    public Log getLog(String beanName) {
        return LogFactory.getLog(beanName);
    }

    public void registerLog(String beanName, Log log) {
        // TODO Auto-generated method stub
        
    }

}
