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
package org.apache.james.queue.activemq;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.james.filesystem.api.FileSystem;

public class ActiveMQMailQueueBlobTest extends ActiveMQMailQueueTest {
    public final static String BASE_DIR = "file://target/james-test";

    private MyFileSystem fs;

    protected ActiveMQConnectionFactory createConnectionFactory() {
        ActiveMQConnectionFactory factory = super.createConnectionFactory();

        FileSystemBlobTransferPolicy policy = new FileSystemBlobTransferPolicy();
        policy.setFileSystem(fs);
        policy.setDefaultUploadUrl(BASE_DIR);
        factory.setBlobTransferPolicy(policy);

        return factory;
    }

    @Override
    public void setUp() throws Exception {
        fs = new MyFileSystem();

        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (fs != null) {
            fs.destroy();
        }
    }

    @Override
    protected boolean useBlobMessages() {
        return true;
    }

    private final class MyFileSystem implements FileSystem {

        public InputStream getResource(String url) throws IOException {
            return null;
        }

        public File getFile(String fileURL) throws FileNotFoundException {
            if (fileURL.startsWith("file://")) {
                return new File(fileURL.substring("file://".length()));

            } else if (fileURL.startsWith("file:/")) {
                return new File(fileURL.substring("file:".length()));

            }
            throw new FileNotFoundException();
        }

        public File getBasedir() throws FileNotFoundException {
            throw new FileNotFoundException();
        }

        public void destroy() throws FileNotFoundException {
            getFile(BASE_DIR).delete();
        }
    }

}
