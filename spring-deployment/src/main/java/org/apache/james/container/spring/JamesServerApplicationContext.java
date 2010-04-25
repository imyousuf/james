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

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;

/**
 * {@link ApplicationContext} which loads all needed beans for JAMES
 *
 */
public class JamesServerApplicationContext extends ClassPathXmlApplicationContext{

    /**
     * The resourceloader to use
     */
    private final static JamesResourceLoader resourceLoader = new JamesResourceLoader() {

        @Override
        protected String getRootPath() {
            return "..";
        }

        public ClassLoader getClassLoader() {
            return this.getClassLoader();
        }
        
    };
    
    
    public JamesServerApplicationContext(String[] configs) {
    	super(configs);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.core.io.DefaultResourceLoader#getResource(java.lang.String)
     */
    public Resource getResource(String fileURL) {
        // delegate the loading to the resourceloader
        Resource r = resourceLoader.getResource(fileURL);
        if (r == null) {
            r = super.getResource(fileURL);
        }
        return r;
    }

}
