/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

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
