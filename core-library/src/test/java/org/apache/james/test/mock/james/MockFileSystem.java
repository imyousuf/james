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

package org.apache.james.test.mock.james;

import org.apache.james.services.FileSystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileInputStream;

public class MockFileSystem implements FileSystem {
    public File getBasedir() throws FileNotFoundException {
        return new File(".");
    }

    public InputStream getResource(String url) throws IOException {
        return new FileInputStream(getFile(url));
    }

    public File getFile(String fileURL) throws FileNotFoundException {
        try {
            if (fileURL.startsWith("file://")) {
                if (fileURL.startsWith("file://conf/")) {
                    return new File(MockFileSystem.class.getClassLoader().getResource("./"+fileURL.substring(12)).getFile());
                    // return new File("./src"+fileURL.substring(6));
                } else {
                    throw new UnsupportedOperationException("getFile: "+fileURL);
                }
            } else {
                throw new UnsupportedOperationException("getFile: "+fileURL);
            }
        } catch (NullPointerException npe) {
            throw new FileNotFoundException("NPE on: "+fileURL);
        }
    }
}
