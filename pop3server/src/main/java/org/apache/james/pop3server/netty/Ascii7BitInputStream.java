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
package org.apache.james.pop3server.netty;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.james.pop3server.ReadByteFilterInputStream;

/**
 *
 *  {@link FilterInputStream} which replace every char which is not a 7-bit ASCII char 
 *  in the stream. Thats makes sure we don't try to send a char over the wire which is not supported
 *  by the POP3 rfc.
 *  
 *
 */
public class Ascii7BitInputStream extends ReadByteFilterInputStream{

    private final char replacementChar;
    public final static char DEFAULT_REPLACEMENT_CHAR = '?';
    
    public Ascii7BitInputStream(InputStream in, char replacementChar) {
        super(in);
        this.replacementChar = replacementChar;
    }

    public Ascii7BitInputStream(InputStream in) {
        this(in, DEFAULT_REPLACEMENT_CHAR);
    }

    @Override
    public int read() throws IOException {
        int i = in.read();
        // check if we need to replace it with the replacement char
        if (i > 127) {
            return replacementChar;
        } else {
            return i;
        }
    }

}
