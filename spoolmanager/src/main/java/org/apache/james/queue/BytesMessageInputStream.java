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
package org.apache.james.queue;

import java.io.IOException;
import java.io.InputStream;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.MessageEOFException;

/**
 * Provide an {@link InputStream} around a {@link BytesMessage}
 * 
 *
 */
public class BytesMessageInputStream extends InputStream {

    private BytesMessage message;
    public BytesMessageInputStream(BytesMessage message) {
        this.message = message;
        
    }
    


    @Override
    public boolean markSupported() {
        return false;
    }


    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int r = 0;
        for (int i = 0; i < len; i++) {
            int a = read();
            if (a == -1) {
                if (i == 0) {
                    return -1;
                } else {
                    break;
                }
            }
            
            r += a;
            b[off + i] = (byte) a;
        }
        return r;
    }


    @Override
    public int read(byte[] b) throws IOException {
        try {
            int i =  message.readBytes(b);
            
            return i;
        } catch (JMSException e) {
            throw new IOException("Unable to read from message", e);
        }
    }

    @Override
    public int read() throws IOException {
        try {
            int i =  message.readByte();
           
            return i;
        } catch (MessageEOFException e) {
            return -1;
        } catch (JMSException e) {
            throw new IOException("Unable to read from message", e);
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
