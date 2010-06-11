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
    StreamRepository sr = null;
    String repositoryName = null;
    String key = null;

    public MimeMessageAvalonSource(StreamRepository sr, String repositoryName, String key) {
        this.sr = sr;
        this.repositoryName = repositoryName;
        this.key = key;
    }

    public String getSourceId() {
        return repositoryName + "/" + key;
    }

    public InputStream getInputStream() throws IOException {
        return sr.get(key);
    }

}
