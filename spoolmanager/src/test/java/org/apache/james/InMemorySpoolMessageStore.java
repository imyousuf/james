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
package org.apache.james;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.james.SpoolMessageStore;
import org.apache.james.core.MimeMessageSource;

public class InMemorySpoolMessageStore implements SpoolMessageStore{

    private Map<String, ByteArrayOutputStream> map = new HashMap<String, ByteArrayOutputStream>();
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.SpoolMessageStore#getMessage(java.lang.String)
     */
    public MimeMessageSource getMessage(final String key) {
        return new MimeMessageSource() {
            private InputStream in = new ByteArrayInputStream(map.get(key).toByteArray());
            @Override
            public InputStream getInputStream() throws IOException {
                return in;
            }

            @Override
            public String getSourceId() {
                return key;
            }
            
        };
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.SpoolMessageStore#saveMessage(java.lang.String)
     */
    public OutputStream saveMessage(String key) {
        ByteArrayOutputStream out = map.get(key);
        if (out == null) {
            out = new ByteArrayOutputStream();
            map.put(key, out);
        } else {
            out.reset();
        }
        return out;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.SpoolMessageStore#remove(java.lang.String)
     */
    public void remove(String key) {
        map.remove(key);
    }

}
