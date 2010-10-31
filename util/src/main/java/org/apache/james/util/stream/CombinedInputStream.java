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

import java.io.IOException;
import java.io.InputStream;

/**
 * Combine an array of {@link InputStream} and read from them
 * 
 *
 */
public class CombinedInputStream extends InputStream{

    private InputStream[] streams;
    private int current = 0;
    public CombinedInputStream(InputStream[] streams) {
        this.streams = streams;
    }
    
    
    @Override
    public int read() throws IOException {
        int i = streams[current].read();
        if (i == -1 && current < streams.length -1) {
            i = streams[++current].read();
        }
        return i;
    }


    @Override
    public void close() throws IOException {
        for (int i = 0 ; i < streams.length; i++) {
            streams[i].close();
        }
    }


    @Override
    public boolean markSupported() {
        return false;
    }

}
