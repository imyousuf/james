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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.james.lifecycle.Disposable;

/**
 * This defines a reusable datasource that can supply an input stream with
 * MimeMessage data.  This allows a MimeMessageWrapper or other classes to
 * grab the underlying data.
 *
 * @see MimeMessageWrapper
 */
public abstract class MimeMessageSource implements Disposable{
    private final List<MimeMessageSource> shares = new ArrayList<MimeMessageSource>();
    
    
    public MimeMessageSource() {
        shares.add(this);
    }
    
    /**
     * Returns a unique String ID that represents the location from where 
     * this file is loaded.  This will be used to identify where the data 
     * is, primarily to avoid situations where this data would get overwritten.
     *
     * @return the String ID
     */
    public abstract String getSourceId();

    /**
     * Get an input stream to retrieve the data stored in the datasource
     *
     * @return a <code>InputStream</code> containing the data
     *
     * @throws IOException if an error occurs while generating the
     *                     InputStream
     */
    public abstract InputStream getInputStream() throws IOException;

    /**
     * Return the size of all the data.
     * Default implementation... others can override to do this much faster
     *
     * @return the size of the data represented by this source
     * @throws IOException if an error is encountered while computing the message size
     */
    public long getMessageSize() throws IOException {
        int size = 0;
        InputStream in = null;
        try {
            in = getInputStream();
            int read = 0;
            byte[] data = new byte[1024];
            while ((read = in.read(data)) > 0) {
                size += read;
            }
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ioe) {
                // Exception ignored because logging is
                // unavailable
            }
        }
        return size;
    }
    
    /**
     * Share this instance and increase the share count
     * 
     * @return instance
     */
    public final SharedMimeMessageSource share() {
        synchronized (shares) {
            SharedMimeMessageSource share = new SharedMimeMessageSource(this);
            return share;
        }
       
    }

    /**
     * Dispose this instance if its not shared anymore
     */
    public final void dispose() {
        synchronized (shares) {
            if (shares.size() == 0) {
                disposeSource();
            }
        }
    }
    
    /**
     * Get called by {@link #dispose()} if this instance is not shared anymore
     */
    protected abstract void disposeSource();

    
    public final class SharedMimeMessageSource extends MimeMessageSource {

        private MimeMessageSource source;

        public SharedMimeMessageSource(MimeMessageSource source) {
            super();
            this.source = source;
        }
        
        @Override
        protected void disposeSource() {
            synchronized (shares) {
                if (shares.remove(SharedMimeMessageSource.this)) {
                    source.dispose();
                }
            }
           
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return source.getInputStream();
        }

        @Override
        public String getSourceId() {
            return source.getSourceId();
        }
        
        public MimeMessageSource getWrapped() {
            return source;
        }
        
    }
}
