/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.util;

import java.io.*;

/**
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class CharTerminatedInputStream extends InputStream {

    private InputStream in;
    private int match[];
    private int buffer[];
    private int pos = 0;
    private boolean endFound = false;

    public CharTerminatedInputStream(InputStream in, char[] terminator) {
        match = new int[terminator.length];
        buffer = new int[terminator.length];
        for (int i = 0; i < terminator.length; i++) {
            match[i] = (int)terminator[i];
            buffer[i] = (int)terminator[i];
        }
        this.in = in;
    }

    public int read() throws IOException {
        if (endFound) {
            //We've found the match to the terminator
            return -1;
        }
        if (pos == 0) {
            //We have no data... read in a record
            int b = in.read();
            if (b == -1) {
                //End of stream reached
                endFound = true;
                return -1;
            }
            if (b != match[0]) {
                //this char is not the first char of the match
                return b;
            }
            //this is a match...put this in the first byte of the buffer,
            // and fall through to matching logic
            buffer[0] = b;
            pos++;
        } else {
            if (buffer[0] != match[0]) {
                //Maybe from a previous scan, there is existing data,
                // and the first available char does not match the
                // beginning of the terminating string.
                return topChar();
            }
            //we have a match... fall through to matching logic.
        }
        //MATCHING LOGIC

        //The first character is a match... scan for complete match,
        // reading extra chars as needed, until complete match is found
        for (int i = 0; i < match.length; i++) {
            if (i >= pos) {
                int b = in.read();
                if (b == -1) {
                    //end of stream found, so match cannot be fulfilled.
                    // note we don't set endFound, because otherwise
                    // remaining part of buffer won't be returned.
                    return topChar();
                }
                //put the read char in the buffer
                buffer[pos] = b;
                pos++;
            }
            if (buffer[i] != match[i]) {
                //we did not find a match... return the top char
                return topChar();
            }
        }
        //A complete match was made...
        endFound = true;
        return -1;
    }

    private int topChar() {
        int b = buffer[0];
        if (pos > 1) {
            //copy down the buffer to keep the fresh data at top
            System.arraycopy(buffer, 1, buffer, 0, pos - 1);
        }
        pos--;
        return b;
    }
}

