/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;

import org.apache.james.core.MimeMessageSource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MimeMessageFileSource extends MimeMessageSource {

    //Define how to get to the data
    String filename = null;
    int uid;
    static int global_uid = 0;

    public MimeMessageFileSource(String filename) {
        this.filename = filename;
        this.uid = global_uid++;
    }

    public String getSourceId() {
        System.out.println("Marcs MimeMessageFileSource Unique ID #"+uid);
        return "Marcs MimeMessageFileSource Unique ID #"+uid;
    }

    public InputStream getInputStream() throws IOException {
        return new FileInputStream(filename);
    }

}
