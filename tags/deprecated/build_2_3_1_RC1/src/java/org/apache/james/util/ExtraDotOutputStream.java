/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Adds extra dot if dot occurs in message body at beginning of line (according to RFC1939)
 * Compare also org.apache.james.smtpserver.SMTPInputStream
 */
public class ExtraDotOutputStream extends FilterOutputStream {

    /*
    static public void main(String[] args) throws IOException
    {
        String data = ".This is a test\r\nof the thing.\r\nWe should not have much trouble.\r\n.doubled?\r\nor not?\n.doubled\nor not?\r\n\r\n\n\n\r\r\r\n";

        OutputStream os = new ExtraDotOutputStream(System.out);
        os.write(data.getBytes());
    }
    */

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
     * Also fixes any naked CR or LF to the RFC 2821 mandated CFLF
     * pairing.
     *
     * @param b the byte to write
     *
     * @throws IOException if an error occurs writing the byte
     */
    public void write(int b) throws IOException {
        switch (b) {
            case '.':
                if (countLast0A0D == 2) {
                    // add extra dot (the first of the pair)
                    out.write('.');
                }
                countLast0A0D = 0;
                break;
            case '\r':
                if (countLast0A0D == 1) out.write('\n'); // two CR in a row, so insert an LF first
                countLast0A0D = 1;
                break;
            case '\n':
                /* RFC 2821 #2.3.7 mandates that line termination is
                 * CRLF, and that CR and LF must not be transmitted
                 * except in that pairing.  If we get a naked LF,
                 * convert to CRLF.
                 */
                if (countLast0A0D != 1) out.write('\r');
                countLast0A0D = 2;
                break;
            default:
                // we're  no longer at the start of a line
                countLast0A0D = 0;
                break;
        }
        out.write(b);
    }
    
    /**
     * Ensure that the stream is CRLF terminated.
     * 
     * @throws IOException  if an error occurs writing the byte
     */
    public void checkCRLFTerminator() throws IOException {
        if (countLast0A0D != 2) {
            write('\n');
        }
    }
}
