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

import org.apache.james.services.FileSystem;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class FileSystemBridge implements FileSystem {

    public File getBasedir() throws FileNotFoundException {
        return new File(".");
    }
    
    private ResourceLoader resourceLoader = null;

    /**
     * loads resources from classpath or file system 
     */
    public InputStream getResource(String url) throws IOException {
        return resourceLoader.getResource(url).getInputStream();
    }

    /**
     * @see org.apache.james.services.FileSystem#getFile(String filURL) 
     */
    public File getFile(String fileURL) throws FileNotFoundException {
        try {
            return resourceLoader.getResource(fileURL).getFile();
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    protected synchronized ResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    public synchronized void setResourceLoader(ResourceLoader provider) {
        this.resourceLoader = provider;
    }

}
