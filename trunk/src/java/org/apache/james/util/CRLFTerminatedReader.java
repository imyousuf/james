/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.util;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * A Reader for use with SMTP or other protocols in which lines
 * must end with CRLF.  Extends BufferedReader and overrides its 
 * readLine() method.  The BufferedReader readLine() method cannot
 * serve for SMTP because it ends lines with either CR or LF alone. 
 */
public class CRLFTerminatedReader extends BufferedReader {

    public class TerminationException extends IOException {
        private int where;
        public TerminationException(int where) {
            super();
            this.where = where;
        }

        public TerminationException(String s, int where) {
            super(s);
            this.where = where;
        }

        public int position() {
            return where;
        }
    }

    /**
     * Constructs this CRLFTerminatedReader.
     * @param in an InputStream
     * @param charsetName the String name of a supported charset.  
     * "ASCII" is common here.
     * @throws UnsupportedEncodingException if the named charset
     * is not supported
     */
    public CRLFTerminatedReader(InputStream in, String charsetName)
            throws UnsupportedEncodingException {
        super(new InputStreamReader(in, charsetName));
    }

    private StringBuffer lineBuffer = new StringBuffer();
    private final int
            EOF = -1,
            CR  = 13,
            LF  = 10;

    private int tainted = -1;

    /**
     * Read a line of text which is terminated by CRLF.  The concluding
     * CRLF characters are not returned with the String, but if either CR
     * or LF appears in the text in any other sequence it is returned
     * in the String like any other character.  Some characters at the 
     * end of the stream may be lost if they are in a "line" not
     * terminated by CRLF.
     * 
     * @return either a String containing the contents of a 
     * line which must end with CRLF, or null if the end of the 
     * stream has been reached, possibly discarding some characters 
     * in a line not terminated with CRLF. 
     * @throws IOException if an I/O error occurs.
     */
    public String readLine() throws IOException{

        //start with the StringBuffer empty
        lineBuffer.delete(0, lineBuffer.length());

        /* This boolean tells which state we are in,
         * depending upon whether or not we got a CR
         * in the preceding read().
         */ 
        boolean cr_just_received = false;

        while (true){
            int inChar = read();

            if (!cr_just_received){
                //the most common case, somewhere before the end of a line
                switch (inChar){
                    case CR  :  cr_just_received = true;
                                break;
                    case EOF :  return null;   // premature EOF -- discards data(?)
                    case LF  :  //the normal ending of a line
                        if (tainted == -1) tainted = lineBuffer.length();
                        // intentional fall-through
                    default  :  lineBuffer.append((char)inChar);
                }
            }else{
                // CR has been received, we may be at end of line
                switch (inChar){
                    case LF  :  // LF without a preceding CR
                        if (tainted != -1) {
                            int pos = tainted;
                            tainted = -1;
                            throw new TerminationException("\"bare\" CR or LF in data stream", pos);
                        }
                        return lineBuffer.toString();
                    case EOF :  return null;   // premature EOF -- discards data(?)
                    case CR  :  //we got two (or more) CRs in a row
                        if (tainted == -1) tainted = lineBuffer.length();
                        lineBuffer.append((char)CR);
                        break;
                    default  :  //we got some other character following a CR
                        if (tainted == -1) tainted = lineBuffer.length();
                        lineBuffer.append((char)CR);
                        lineBuffer.append((char)inChar);
                        cr_just_received = false;
                }
            }
        }//while
    }//method readLine()
}
