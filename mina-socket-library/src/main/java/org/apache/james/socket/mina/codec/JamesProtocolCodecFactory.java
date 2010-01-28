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

package org.apache.james.socket.mina.codec;

import java.nio.charset.Charset;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.textline.LineDelimiter;
import org.apache.mina.filter.codec.textline.TextLineEncoder;

public class JamesProtocolCodecFactory  implements ProtocolCodecFactory {
    
    private final ProtocolEncoder encoder = new TextLineEncoder(Charset.forName("US-ASCII"), LineDelimiter.CRLF);
    private final ProtocolDecoder decoder = new CRLFTerminatedLineDecoder();
    
    /*
     * (non-Javadoc)
     * @see org.apache.mina.filter.codec.ProtocolCodecFactory#getEncoder(org.apache.mina.core.session.IoSession)
     */
    public ProtocolEncoder getEncoder(IoSession arg0) throws Exception {
        return encoder;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.mina.filter.codec.ProtocolCodecFactory#getDecoder(org.apache.mina.core.session.IoSession)
     */
    public ProtocolDecoder getDecoder(IoSession arg0) throws Exception {
        return decoder;
    }
}
