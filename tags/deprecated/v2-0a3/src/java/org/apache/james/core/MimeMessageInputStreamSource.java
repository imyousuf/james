/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE file.
 */
package org.apache.james.core;

import java.io.*;

/**
 * Takes an input stream and creates a repeatable input stream source
 * for a MimeMessageWrapper.  It does this by completely reading the
 * input stream and saving that to a temporary file that should delete on exit,
 * or when this object is GC'd.
 *
 * @see MimeMessageWrapper
 *
 *
 * @author <a href="mailto:sergek@lokitech.com>">Serge Knystautas</a>
 *
 * Modified by <a href="mailto:okidz@pindad.com">Oki DZ</a>
 * Thu Oct  4 15:15:27 WIT 2001
 *
 */
public class MimeMessageInputStreamSource extends MimeMessageSource {

    File file = null;
    String sourceId = null;

    public MimeMessageInputStreamSource(String key, InputStream in) {
        //We want to immediately read this into a temporary file
        //Create a temp file and channel the input stream into it
        try {
            file = File.createTempFile(key, ".m64");
            OutputStream fout = new BufferedOutputStream(new FileOutputStream(file));
            int b = -1;
            while ((b = in.read()) != -1) {
                fout.write(b);
            }
            fout.close();
            in.close();
            file.deleteOnExit();

            sourceId = file.getCanonicalPath();
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to retrieve the data: " + ioe.getMessage());
        }
    }

    /**
     * Returns the unique identifier of this input stream source
     */
    public String getSourceId() {
        return sourceId;
    }

    /**
     * Return an input stream to the data
     */
    public synchronized InputStream getInputStream() throws IOException {
        return new BufferedInputStream(new FileInputStream(file));
    }

    /**
     * Return the size of the temp file
     */
    public long getMessageSize() throws IOException {
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
