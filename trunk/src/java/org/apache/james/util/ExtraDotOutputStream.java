/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Adds extra dot if dot occurs in message body at beginning of line (according to RFC1939)
 * Compare also org.apache.james.smtpserver.SMTPInputStream
 *
 */
public class ExtraDotOutputStream extends FilterOutputStream {

    /**
     * Counter for number of last (0A or 0D).
     */
    protected int countLast0A0D;

    /**
     * Constructor that wraps an OutputStream.
     *
     * @param out the OutputStream to be wrapped
     */
    public ExtraDotOutputStream(OutputStream out) {
        super(out);
        countLast0A0D = 2; // we already assume a CRLF at beginning (otherwise TOP would not work correctly !)
    }

    /**
     * Writes a byte to the stream, adding dots where appropriate.
     *
     * @param b the byte to write
     *
     * @throws IOException if an error occurs writing the byte
     */
    public void write(int b) throws IOException {
        out.write(b);

        switch (b) {
            case '.':
                if (countLast0A0D == 2) {
                    // add extra dot
                    out.write('.');
                }
                countLast0A0D = 0;
                break;
            case '\r':
                countLast0A0D = 1;
                break;
            case '\n':
                if (countLast0A0D == 1) {
                    countLast0A0D = 2;
                } else {
                    countLast0A0D = 0;
                }
                break;
            default:
				// we're  no longer at the start of a line
                countLast0A0D = 0;
                break;
        }
    }
}

