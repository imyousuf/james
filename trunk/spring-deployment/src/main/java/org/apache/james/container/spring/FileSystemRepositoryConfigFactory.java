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
import java.io.FileNotFoundException;

import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.james.services.FileSystem;

/**
 * Factory which use the {@link FileSystem} to lookup the configuration file and root directory to build a 
 * {@link RepositoryConfig}
 * 
 *
 */
public class FileSystemRepositoryConfigFactory{

    /**
     * Create a new {@link RepositoryConfig} 
     * 
     * @param config
     * @param root
     * @param fs
     * @return repositoryConfig
     * @throws ConfigurationException
     */
    public static RepositoryConfig create(String config, String root, FileSystem fs) throws ConfigurationException {
        try {
            File configFile = fs.getFile(config);
            File rootDir = fs.getFile(root);
            
            // create the rootDir if it not exist already
            if (rootDir.exists() == false) {
                rootDir.mkdirs();
            }
            
            return RepositoryConfig.create(configFile, rootDir);

        } catch (FileNotFoundException e) {
            throw new ConfigurationException("Unable to load configurationFile ", e);
        }


    }
}
