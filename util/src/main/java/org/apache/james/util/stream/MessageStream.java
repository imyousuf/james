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

package org.apache.james.util.stream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Helper class to write to temporary file for transfer data. This is mostly useful for writing
 * large chunk of data back the the client. This Object is designed for one time use..
 * 
 *
 */
public class MessageStream {
    private File file;

    /**
     * Create new MessageStream on a temporary File
     * 
     * @throws IOException 
     */
    public MessageStream() throws IOException {
        this.file = File.createTempFile("messagestream", ".ms");
    }

    /**
     * Return the {@link OutputStream} of the temporary file
     * 
     * @return out
     * @throws IOException
     */
    public OutputStream getOutputStream() throws IOException {
        return new FileOutputStream(file);
    }
    
    /**
     * Return the {@link DisposeOnCloseInputStream} of the temporary file
     * 
     * @return in
     * @throws IOException
     */
    public DisposeOnCloseInputStream getInputStream() throws IOException {
        return new DisposeOnCloseInputStream(new DisposableFileInputStream(file));
    }
}
