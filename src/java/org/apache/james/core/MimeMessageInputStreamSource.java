/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE file.
 */
package org.apache.james.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * MimeMessageInputStreamSource.java
 *
 *
 * Created:
 *
 * @author
 * @version
 *
 * Modified by <a href="mailto:okidz@pindad.com">Oki DZ</a>
 * Thu Oct  4 15:15:27 WIT 2001
 *
 */
public class MimeMessageInputStreamSource extends MimeMessageSource {

    String key = null;
    InputStream in = null;
    File file = null;

    //If you try to access this size first, it will load it into a temp file
    //  and work from there.

    public MimeMessageInputStreamSource(String key, InputStream in) {
        this.key = key;
        this.in = in;
    }

    /**
     * Return an input stream to the data
     */
    public synchronized InputStream getInputStream() throws IOException {
        if (file == null) {
            return in;
        } else {
            return new BufferedInputStream(new FileInputStream(file));
        }
    }

    /**
     * If not already, read the stream into a temp file
     */
    public synchronized long getMessageSize() throws IOException {
        if (file == null) {
            //Create a temp file and channel the input stream into it
            file = File.createTempFile(key, ".m64");
            OutputStream fout = new BufferedOutputStream(new FileOutputStream(file));
            int b = -1;
            while ((b = in.read()) != -1) {
                fout.write(b);
            }
            fout.close();
            in.close();
            file.deleteOnExit();
        }
        return file.length();
    }

    public void finalize() {
        try {
            if (file != null && file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            //ignore
        }
    }
}
