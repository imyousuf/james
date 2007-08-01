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

import org.apache.james.services.FileSystem;

public class FileSystemBridge implements FileSystem {

	public File getBasedir() throws FileNotFoundException {
		return new File(".");
	}

    public File getFile(String fileURL) throws FileNotFoundException {
        if (fileURL.startsWith("file://")) {
            if (fileURL.startsWith("file://conf/")) {
                return new File("./src/trunk/config/"+fileURL.substring(12));
            } else {
            	return new File("./"+fileURL.substring(7));
            }
        } else {
            throw new UnsupportedOperationException("getFile: "+fileURL);
        }
    }

}
