/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.mailrepository;

import org.apache.avalon.cornerstone.services.store.StreamRepository;
import org.apache.james.core.MimeMessageSource;

import java.io.IOException;
import java.io.InputStream;

public class MimeMessageAvalonSource extends MimeMessageSource {

    //Define how to get to the data
    
    /**
     * The stream repository used by this data source.
     */
    StreamRepository sr = null;

    /**
     * The name of the repository
     */
    String repositoryName = null;

    /**
     * The key for the particular stream in the stream repository
     * to be used by this data source.
     */
    String key = null;

    public MimeMessageAvalonSource(StreamRepository sr, String repositoryName, String key) {
        this.sr = sr;
        this.repositoryName = repositoryName;
        this.key = key;
    }

    /**
     * Returns a unique String ID that represents the location from where 
     * this source is loaded.  This will be used to identify where the data 
     * is, primarily to avoid situations where this data would get overwritten.
     *
     * @return the String ID
     */
    public String getSourceId() {
        StringBuffer sourceIdBuffer =
            new StringBuffer(128)
                    .append(repositoryName)
                    .append("/")
                    .append(key);
        return sourceIdBuffer.toString();
    }

    public InputStream getInputStream() throws IOException {
        return sr.get(key);
    }

}
