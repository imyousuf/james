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
 * @author Stephan Schiessling <s@rapi.com>
 */
public class ExtraDotOutputStream extends FilterOutputStream {

    /**
     * Counter for number of last (0A or 0D).
     */
    protected int countLast0A0D;

    public ExtraDotOutputStream(OutputStream out) {
        super(out);
        countLast0A0D = 2; // we already assume a CRLF at beginning (otherwise TOP would not work correctly !)
    }

    public void write(int b) throws IOException {
        out.write(b);
        if (b == '.') {
            if (countLast0A0D > 1) {
                // add extra dot
                out.write('.');
            }
            countLast0A0D = 0;
        } else {
            if (b == '\r' || b == '\n') {
                countLast0A0D++;
            } else {
                countLast0A0D = 0;
            }
        }
    }

}

