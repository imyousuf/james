/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.mailrepository;

import java.io.IOException;
import java.io.InputStream;
import org.apache.avalon.cornerstone.services.store.StreamRepository;
import org.apache.james.core.JamesMimeMessageInputStream;

public class FileMimeMessageInputStream 
    extends JamesMimeMessageInputStream {

    //Define how to get to the data
    StreamRepository sr = null;
    String key = null;

    public FileMimeMessageInputStream( StreamRepository sr, String key) throws IOException {
        this.sr = sr;
        this.key = key;
    }

    public StreamRepository getStreamStore() {
        return sr;
    }

    public String getKey() {
        return key;
    }

    protected synchronized InputStream openStream() throws IOException {
        return sr.get(key);
    }

    public boolean equals(Object obj) {
        if (obj instanceof FileMimeMessageInputStream) {
            FileMimeMessageInputStream in = (FileMimeMessageInputStream)obj;
            return in.getStreamStore().equals(sr) && in.getKey().equals(key);
        }
        return false;
    }
}
