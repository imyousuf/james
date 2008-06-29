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
package org.apache.james.container.spring.adaptor;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.File;

/**
 * A resource loader supporting JAMES' conf/var specific behaviours and
 * the "classpath:" prefix.
 */
public class JamesResourceLoader implements ResourceLoader, ApplicationContextAware {

    private ApplicationContext applicationContext;
    
    private static final String FILE_PROTOCOL = "file://";
    private static final String FILE_PROTOCOL_AND_CONF = "file://conf/";
    private static final String FILE_PROTOCOL_AND_VAR = "file://var/";
    
    public ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    public Resource getResource(String fileURL) {
        Resource r = null;
        if (fileURL.startsWith("classpath:")) {
            String resourceName = fileURL.substring("classpath:".length());
            r = new ClassPathResource(resourceName);
        } else if (fileURL.startsWith(FILE_PROTOCOL)) {
            File file = null;
            if (fileURL.startsWith(FILE_PROTOCOL_AND_CONF)) {
                file = new File("../conf/" + fileURL.substring(FILE_PROTOCOL_AND_CONF.length()));
            } else if (fileURL.startsWith(FILE_PROTOCOL_AND_VAR)) {
                file = new File("../var/" + fileURL.substring(FILE_PROTOCOL_AND_VAR.length()));
            } else {
                file = new File("./" + fileURL.substring(FILE_PROTOCOL.length()));
            }
            r = new FileSystemResource(file);
        } else {
            r = applicationContext.getResource(fileURL);
        }
        return r;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
