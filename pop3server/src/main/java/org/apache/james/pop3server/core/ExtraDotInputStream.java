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

import java.io.IOException;
import java.io.InputStream;

import org.apache.james.pop3server.ReadByteFilterInputStream;

/**
 * Adds extra dot if dot occurs in message body at beginning of line (according
 * to RFC1939)
 */
public class ExtraDotInputStream extends ReadByteFilterInputStream {

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
    public synchronized int read() throws IOException {
        if (end)
            return -1;

        if (startLine) {
            int i = 0;
            // check if we still have something in the buffer
            // if so we need to copy it so we don't lose data

            // See JAMES-1152
            if (pos != -1 && pos != 0) {
                byte[] tmpBuf = new byte[3];
                while (pos < buf.length) {
                    tmpBuf[i++] = buf[pos++];
                }

                buf = tmpBuf;
            }
            while (i < buf.length) {
                buf[i++] = (byte) in.read();
            }
            if (buf[0] == '.' && buf[1] == '\r' && buf[2] == '\n') {
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
