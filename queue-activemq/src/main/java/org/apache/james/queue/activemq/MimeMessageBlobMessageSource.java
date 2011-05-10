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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.mail.internet.SharedInputStream;

import org.apache.james.core.MimeMessageSource;
import org.apache.james.lifecycle.api.Disposable;

/**
 *
 */
public class MimeMessageBlobMessageSource extends MimeMessageSource implements ActiveMQSupport, Disposable {

    private SharedInputStream in;
    private String sourceId;
    private long size;
    private List<InputStream> streams = new ArrayList<InputStream>();

    public MimeMessageBlobMessageSource(SharedInputStream in, long size, String sourceId) {
        this.in = in;
        this.size = size;
        this.sourceId = sourceId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.core.MimeMessageSource#getInputStream()
     */
    public synchronized InputStream getInputStream() throws IOException {
        InputStream sin = in.newStream(0, -1);
        streams.add(sin);
        return sin;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.core.MimeMessageSource#getSourceId()
     */
    public String getSourceId() {
        return sourceId;
    }

    @Override
    public long getMessageSize() throws IOException {
        // if the size is < 1 we seems to not had it stored in the property, so
        // fallback to super implementation
        if (size == -1) {
            super.getMessageSize();
        }
        return size;
    }

    /**
     * Call dispose on the {@link InputStream}
     */
    public synchronized void dispose() {

        try {
            ((InputStream) in).close();
        } catch (IOException e) {
            // ignore on dispose
        }
        in = null;
        for (int i = 0; i < streams.size(); i++) {
            InputStream s = streams.get(i);
            try {
                s.close();
            } catch (IOException e) {
                // ignore on dispose
            }
            s = null;
        }
        streams.clear();
    }
}
