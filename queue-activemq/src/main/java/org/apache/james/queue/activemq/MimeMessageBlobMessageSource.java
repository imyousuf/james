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

import javax.jms.JMSException;

import org.apache.activemq.BlobMessage;
import org.apache.activemq.Disposable;
import org.apache.james.core.MimeMessageSource;
import org.apache.james.core.NonClosingSharedInputStream;
import org.apache.james.lifecycle.LifecycleUtil;

/**
 * {@link MimeMessageSource} which use a {@link BlobMessage} as input. Be aware that {@link BlobMessage} must contain
 * a {@link NonClosingSharedInputStream} for this implementation!
 *
 */
@SuppressWarnings("unchecked")
public class MimeMessageBlobMessageSource extends MimeMessageSource implements ActiveMQSupport, Disposable{

    private NonClosingSharedInputStream in;
    private String sourceId;
    private long size;

    public MimeMessageBlobMessageSource(BlobMessage message) throws JMSException, IOException {
        this.sourceId = message.getJMSMessageID();
        this.size = message.getLongProperty(JAMES_MAIL_MESSAGE_SIZE);
        this.in = (NonClosingSharedInputStream)message.getInputStream();
    }
    

    /*
     * (non-Javadoc)
     * @see org.apache.james.core.MimeMessageSource#getInputStream()
     */
    public InputStream getInputStream() throws IOException {
        return in.newStream(0, -1);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.core.MimeMessageSource#getSourceId()
     */
    public String getSourceId() {
        return sourceId;
    }
    
    @Override
    public long getMessageSize() throws IOException {
        // if the size is < 1 we seems to not had it stored in the property, so fallback to super implementation
        if (size == -1) {
            super.getMessageSize();
        }
        return size;
    }
    
    /**
     * Call dispose on the {@link InputStream}
     */
    public void dispose() {
        LifecycleUtil.dispose(in);
    }
}
