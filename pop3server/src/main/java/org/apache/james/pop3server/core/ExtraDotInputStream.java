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

package org.apache.james.pop3server.core;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Adds extra dot if dot occurs in message body at beginning of line (according to RFC1939)
 */
public class ExtraDotInputStream extends FilterInputStream{

    byte[] buf = new byte[3];
    int pos = 0;
    boolean end = false;
    boolean extraDot = false;
    boolean startLine;
    int last;
    
    public ExtraDotInputStream(InputStream in) {
        super(in);
        startLine = true;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int i;
        for (i = 0; i < len; i++) {
            int a = read();
            if (i == 0 && a == - 1) {
                return -1;
            } else {
                if (a == -1) {
                    break;
                } else {
                    b[off++] =  (byte) a;
                }
            }
        }
        return i;
        
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public synchronized int read() throws IOException {
        if (end) return -1;
        
        if (startLine) {
            int i = 0;
            while (i < 3) {
                buf[i++] = (byte) in.read();
            }
            if (buf[0] == '.' && buf[1] == '\r' && buf[2] ==  '\n') {
                extraDot = true;
            }
            startLine = false;
            pos = 0;
        }
        
        int a;
        if (pos == -1) {
            a = in.read();
        } else {
            if (extraDot) {
                extraDot = false;
                return '.';
            } else {
                a = buf[pos++];
                
                if (pos == buf.length) {
                    pos = -1;
                }
                if (a == -1) {
                    end = true;
                }
            }
            
        }
        if (last == '\r' && a == '\n') {
            startLine = true;
        }
        last = a;
        return a;
       
    }

}
