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
package org.apache.james.core;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.mail.internet.SharedInputStream;

import org.apache.james.lifecycle.Disposable;


/**
 * A wrapper around classes which implements {@link SharedInputStream} and {@link InputStream}. This class will not close the underlying
 * stream on {@link #close()} call. It will only closs the stream when {@link #dispose()} is called
 *
 */
public class NonClosingSharedInputStream<E extends InputStream & SharedInputStream> extends FilterInputStream implements Disposable, SharedInputStream{

    public NonClosingSharedInputStream(E in) throws IOException {
        super(in);
    }

    @Override
    public void close() throws IOException {
        // do nothing
    }

    /**
     * Close the stream and so all streams which share the same file
     */
    public void dispose() {
        try {
            super.close();
        } catch (IOException e) {
            // ignore on close
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.mail.internet.SharedInputStream#getPosition()
     */
    public long getPosition() {
        return ((SharedInputStream)in).getPosition();
    }

    /*
     * (non-Javadoc)
     * @see javax.mail.internet.SharedInputStream#newStream(long, long)
     */
    public InputStream newStream(long arg0, long arg1) {
        return ((SharedInputStream)in).newStream(arg0, arg1);
    }

}
