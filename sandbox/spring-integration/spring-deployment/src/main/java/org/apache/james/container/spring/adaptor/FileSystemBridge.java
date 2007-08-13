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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileInputStream;

import org.apache.james.services.FileSystem;

public class FileSystemBridge implements FileSystem {
    private static final String FILE_PROTOCOL = "file://";
    private static final String FILE_PROTOCOL_AND_CONF = "file://conf/";

    public File getBasedir() throws FileNotFoundException {
        return new File(".");
    }

    /**
     * loads resources from classpath or file system 
     */
    public InputStream getResource(String url) throws IOException {
        if (url.startsWith("classpath:")) {
            String resourceName = url.substring("classpath:".length());
            InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
            if (resourceAsStream==null) {
                throw new IOException("Resource '" + resourceName + "' not found in the classpath!");
            }
            return resourceAsStream;
        }
        return new FileInputStream(getFile(url));
    }

    /**
     * @see org.apache.james.services.FileSystem#getFile(String filURL) 
     */
    public File getFile(String fileURL) throws FileNotFoundException {
        if (fileURL.startsWith(FILE_PROTOCOL)) {
            File file = null;
            if (fileURL.startsWith(FILE_PROTOCOL_AND_CONF)) {
                file = new File("./src/main/config/" + fileURL.substring(FILE_PROTOCOL_AND_CONF.length()));
            } else {
                file = new File("./" + fileURL.substring(FILE_PROTOCOL.length()));
            }
            if (!file.exists()) {
                throw new FileNotFoundException("cannot access file " + file.toString());
            }
            return file;
        } else {
            throw new UnsupportedOperationException("getFile: " + fileURL);
        }
    }

}
