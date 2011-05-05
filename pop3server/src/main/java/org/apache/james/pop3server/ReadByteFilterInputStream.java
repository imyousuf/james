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

package org.apache.james.pop3server;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * {@link FilterInputStream} which delagates every {@link #read(byte[])} and {@link #read(byte[], int, int)} to the {@link #read()} method
 * 
 *
 */
public class ReadByteFilterInputStream extends FilterInputStream {

    protected ReadByteFilterInputStream(InputStream in) {
        super(in);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int i;
        for (i = 0; i < len; i++) {
            int a = read();
            if (i == 0 && a == -1) {
                return -1;
            } else {
                if (a == -1) {
                    break;
                } else {
                    b[off++] = (byte) a;
                }
            }
        }
        return i;

    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

}
