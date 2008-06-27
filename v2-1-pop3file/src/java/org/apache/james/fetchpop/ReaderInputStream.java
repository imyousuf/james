/**
 * ReaderInputStream.java
 * 
 * Copyright (C) 24-Sep-2002 The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file. 
 *
 * Danny Angus
 */
package org.apache.james.fetchpop;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
/**
 * 
 * 
 * Simple class to allow a cast from a java.io.Reader to a java.io.InputStream<br>
 * <br>$Id: ReaderInputStream.java,v 1.1 2002/09/24 15:36:30 danny Exp $
 * @author <A href="mailto:danny@apache.org">Danny Angus</a>
 * 
 */
public class ReaderInputStream extends InputStream {
    private Reader reader = null;
    public ReaderInputStream(Reader reader) {
        this.reader = reader;
    }
    /**
     * @see java.io.InputStream#read()
     */
    public int read() throws IOException {
        return reader.read();
    }
}
