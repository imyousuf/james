/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE file.
 */
package org.apache.james.core;

import java.io.IOException;
import java.io.InputStream;

/**
 * A source of a MimeMessage.
 */
public abstract class MimeMessageSource {
    /**
     * Return an input stream to the data
     */
    public abstract InputStream getInputStream() throws IOException;

    /**
     * Return the size of all the data.
     * Default implementation... others can override to do this much faster
     */
    public long getMessageSize() throws IOException {
        int size = 0;
        InputStream in = getInputStream();
        int read = 0;
        byte[] data = new byte[1024];
        while ((read = in.read(data)) > 0) {
            size += read;
        }
        return size;
    }
}
