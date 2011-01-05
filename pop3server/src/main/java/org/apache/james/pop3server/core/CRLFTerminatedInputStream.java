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
 * This {@link FilterInputStream} makes sure that the last chars of the stream are \r\n
 *
 */
public class CRLFTerminatedInputStream extends ReadByteFilterInputStream{

    private int last;
    private byte[] extraData;
    private int pos = 0;

    private boolean endOfStream = false;
    
    public CRLFTerminatedInputStream(InputStream in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        if (endOfStream == false) {
            int i = super.read();
            if (i == -1) {
                endOfStream = true;
                if (last != '\r') {
                    extraData = new byte[2];
                    extraData[0] = '\r';
                    extraData[1] = '\n';
                } else if (last != '\n') {
                    extraData = new byte[1];
                    extraData[0] = '\r';
                } else {
                    extraData = null;
                }

            } else {
                last = i;
            }
            return i;

        } else {
            if (extraData == null || extraData.length == pos) {
                return -1;
            } else {
                return extraData[pos++];
            }
        }
    }
}
