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
 * This class provides a hasReachedEnd() method to keep trak of "/r/n". If
 * al least one "/r/n" has been read the method returns true.
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class SMTPInputStream extends FilterInputStream {
    
    private static final String EOM = "\r\n.\r\n";
    private int match;
    
    public SMTPInputStream(InputStream in) {
        super(in);
    }
    
    public int read()
    throws IOException {
        int read = super.read();
        if ((byte) read == EOM.charAt(match)) {
            match++;
        } else if ((byte) read == EOM.charAt(0)) {
            match = 1;
        } else {
            match = 0;
        }
        return read;
    }
    
    public boolean hasReachedEnd() {
        return match == 5;
    }
}
    
