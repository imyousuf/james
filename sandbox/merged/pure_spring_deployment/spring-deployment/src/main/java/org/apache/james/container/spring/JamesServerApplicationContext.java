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

import java.io.File;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

public class JamesServerApplicationContext extends ClassPathXmlApplicationContext{

    private static final String FILE_PROTOCOL = "file://";
    private static final String FILE_PROTOCOL_ABSOLUTE = "file:///";

    private static final String FILE_PROTOCOL_AND_CONF = "file://conf/";
    private static final String FILE_PROTOCOL_AND_VAR = "file://var/";
    
    public JamesServerApplicationContext(String[] configs) {
    	super(configs);
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
            } else if (fileURL.startsWith(FILE_PROTOCOL_ABSOLUTE)) {
            	file = new File("/" + fileURL.substring(FILE_PROTOCOL_ABSOLUTE.length()));
            } else {
            	// move to the root folder of the spring deployment
                file = new File("../" + fileURL.substring(FILE_PROTOCOL.length()));
            }
            r = new FileSystemResource(file);
        } else {
            r = super.getResource(fileURL);
        }
        return r;
    }

}
