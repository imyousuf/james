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

package org.apache.james.security.openpgp.bc;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

import org.apache.james.security.openpgp.Writable;

public class WritableFileContent implements Writable {

    private final File file;
    
    public WritableFileContent(String fileName) throws Exception {
        this(new File(fileName));
    }
        
    public WritableFileContent(final File file) {
        super();
        this.file = file;
    }

    public void write(OutputStream out) throws Exception {
        FileInputStream in = new FileInputStream(file);
        int nextByte = in.read();
        while(nextByte >= 0) {
            out.write(nextByte);
            nextByte = in.read();
        }
    }

}
