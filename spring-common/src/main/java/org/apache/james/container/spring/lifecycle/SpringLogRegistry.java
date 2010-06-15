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
package org.apache.james.container.spring.lifecycle;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.Log4JLogger;
import org.apache.james.container.spring.Registry;
import org.springframework.beans.factory.InitializingBean;

/**
 * Provide a Log object for components
 * 
 *
 */
public class SpringLogRegistry implements Registry<Log>, InitializingBean {

    private final Map<String,Log> logMap = new HashMap<String,Log>();
    private Map<String, String> logs;
    private final static String PREFIX = "james.";

    /*
     * (non-Javadoc)
     * @see org.apache.james.container.spring.Registry#getForComponent(java.lang.String)
     */
	public Log getForComponent(String componentname) throws RegistryException{
	    Log log = logMap.get(componentname);
	    if (log != null) {
	        return log;
	    } else {
	        return new Log4JLogger(PREFIX + componentname);
	    }
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.apache.james.container.spring.Registry#registerForComponent(java.lang.String, java.lang.Object)
	 */
    public void registerForComponent(String name, Log log) {
        logMap.put(name, log);
    }

    public void setLogMappings(Map<String,String> logs) {
        this.logs = logs;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        if (logs != null) {
            Iterator<String> it = logs.keySet().iterator();
            while(it.hasNext()) {
                String key = it.next();
                String value = logs.get(key);
                
                logMap.put(key, new Log4JLogger(PREFIX + value));
            }
        }
    }
}
