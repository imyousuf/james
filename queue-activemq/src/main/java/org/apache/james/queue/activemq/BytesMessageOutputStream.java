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
package org.apache.james.queue.activemq;

import java.io.IOException;
import java.io.OutputStream;

import javax.jms.BytesMessage;
import javax.jms.JMSException;

/**
 * Provide an {@link OutputStream} over an {@link BytesMessage}
 * 
 *
 */
public class BytesMessageOutputStream extends OutputStream {

    private BytesMessage message;
   
    public BytesMessageOutputStream(BytesMessage message) {
        this.message = message;
    }
    
    
    @Override
    public void write(int b) throws IOException {
        try {
            message.writeInt(b);
        } catch (JMSException e) {
            throw new IOException("Unable to write to message");
        }
    }
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        try {
            message.writeBytes(b, off, len);
        } catch (JMSException e) {
            throw new IOException("Unable to write to message");
        }
    }
    @Override
    public void write(byte[] b) throws IOException {
        try {
            message.writeBytes(b);
        } catch (JMSException e) {
            throw new IOException("Unable to write to message");
        }
    }
    
    /**
     * Return the underlying {@link BytesMessage}
     * 
     * @return message
     */
    public BytesMessage getMessage() {
        return message;
    }
    
}


