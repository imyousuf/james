/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.smtpserver;

import java.io.*;

/**
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class SMTPInputStream extends InputStream {
    
    private InputStream in;
    
    private static final char[] EOM = {'\r','\n','.','\r','\n'};
    
    private int match;
    
    public SMTPInputStream (InputStream in) {
        super();
        this.in = new BufferedInputStream(in);
        match = 0;
    }
    
    public int read()
    throws IOException {
        if (match == 5) return -1;
        char next = (char) in.read();
        if (next == EOM[match]) {
            match++;
        } else if (next == EOM[0]) {
            match = 1;
        } else {
            match = 0;
        }
        return (int) next;
    }
}
    
