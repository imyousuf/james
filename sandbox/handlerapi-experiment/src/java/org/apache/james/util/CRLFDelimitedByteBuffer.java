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

import java.io.IOException;
import java.io.InputStream;

/*
 * A simple, synchronized, queue of CRLF-delimited lines.
 *
 * NOTA BENE: as bytes arrive, they are buffered internally up to a
 * configured maximum.  The maximum is intended to reflect a line length
 * limiter, but there is a potential corner case that should be
 * understood.  If the working buffer is almost full, and a new packet
 * arrives with enough data to overflow the buffer, the code will
 * consider that an error and discard the entire working buffer, even
 * though the beginning of the new packet might have terminated the
 * currently accumulating line.  And the reported position of the
 * overflow is based upon the buffer position before scanning for lines,
 * not an actualy line length (perhaps we need not even report the
 * position).  Since the purpose for this maximum is to prevent memory
 * flooding attacks, this does not appear to be a particularly critical
 * corner case.  We simply need to make sure that the working buffer is
 * at least twice the size of the maximum desired line length.
 *
 * After buffering the incoming data, it is scanned for CRLF.  As each
 * line is found, it is moved to an ArrayList of Line objects.  When all
 * data has been scanned, any remaining bytes are shifted within the
 * working buffer to prepare for the next packet.
 *
 * The code enforces CRLF pairing (RFC 2821 #2.7.1).  The Line object,
 * which is for internal use only, can hold the bytes for each line or
 * record an exception (line termination or line length) associated
 * with processing the data that would otherwise have.  The exceptions
 * are rethrown during the read operation, rather than during the write
 * operation, so that the order of responses perserves the order of
 * input.
 *
 * This code does not handle dot stuffing.  Dot Stuffing, Message size
 * limiting, and buffering of the message in a file are all expected to
 * be performed by the I/O handler associated with the DATA accumulation
 * state.
 */

public class CRLFDelimitedByteBuffer {
    static public class TerminationException extends java.io.IOException {
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

    static public class LineLengthExceededException extends java.io.IOException {
        public LineLengthExceededException(String s) {
            super(s);
        }
    }

    private InputStream input;

    public CRLFDelimitedByteBuffer(InputStream input) {
        this(input, 2048);
    }

    public CRLFDelimitedByteBuffer(InputStream input, int maxLineLength) {
        this.input = input; 
        lines = new java.util.ArrayList();
        workLine = new byte[maxLineLength];
    }

    synchronized public boolean isEmpty() {
        return lines.isEmpty();
    }

    synchronized public byte[] read() throws IOException, LineLengthExceededException, TerminationException {
        byte[] buffer = new byte[1000];
        while (isEmpty()) {
            int length = input.read(buffer);
            write(buffer, length);
        }
        return lines.isEmpty() ? null : ((Line) lines.remove(0)).getBytes();
    }

    synchronized public String readString() throws IOException, LineLengthExceededException, TerminationException {
        byte[] buffer = new byte[1000];
        int length;
        while (lines.isEmpty() && isEmpty() && (length = input.read(buffer))!=-1) {
            write(buffer, length);
        }
        if (lines.isEmpty()) return null;
        else {
            byte[] bytes = ((Line) lines.remove(0)).getBytes();
            try {
                return new String(bytes, "US-ASCII");
            } catch (java.io.UnsupportedEncodingException uee) {
                return new String(bytes);
            }
        }
    }

    synchronized public void write(byte[] data, int length) {
        if (canFit(length)) {
            System.arraycopy(data, 0, workLine, writeindex, length);
            writeindex += length;
            buildlines();
        }
    }

    synchronized public void write(byte data) {
        if (canFit(1)) {
            workLine[writeindex++] = data;
            buildlines();
        }
    }

    synchronized public void write(String s) {
        write(s.getBytes(), s.getBytes().length);
    }

    private boolean canFit(int length) {
        if (writeindex + length > workLine.length) {
            reset();
            lines.add(new Line(new LineLengthExceededException("Exceeded maximum line length")));
            return false;
        } else return true;
    }

    static private class Line {
        java.io.IOException e;
        byte[] bytes;

        public Line(byte[] data) {
            bytes = data;
        }

        public Line(String data) {
            bytes = data.getBytes();
        }

        public Line(java.io.IOException e) {
            this.e = e;
        }

        public Line(byte[] data, int offset, int length) {
            bytes = new byte[length];
            System.arraycopy(data, offset, bytes, 0, length);
        }

        public byte[] getBytes() throws LineLengthExceededException, TerminationException {
            if (e != null) {
                if (e instanceof LineLengthExceededException) throw (LineLengthExceededException) e;
                else  throw (TerminationException) e;
            }
            return bytes;
        }
    }

    private java.util.ArrayList lines;

    private byte[] workLine;
    private int writeindex = 0;
    private int readindex = 0;
    private int scanindex = 0;      // current index for matching within the working buffer

    private void reset() {
        writeindex = 0;
        readindex = 0;
        scanindex = 0;
    }

    private void shift() {
        System.arraycopy(workLine, readindex, workLine, 0, writeindex - readindex);
        writeindex -= readindex;
        scanindex -= readindex;
        readindex = 0;
    }

    private void buildlines() {
        for (; scanindex < writeindex; scanindex++) {
            if (workLine[scanindex] == '\n') {
                final int pos = scanindex;
                reset();
                lines.add(new Line(new TerminationException("\"bare\" LF in data stream.", pos)));
                break;
            } else if (workLine[scanindex] == '\r') {
                if (scanindex+1 == writeindex) break;
                else if (workLine[++scanindex] == '\n') {
                    lines.add(new Line(workLine, readindex, scanindex - readindex + 1));
                    readindex = scanindex + 1;
                } else {
                    final int pos = scanindex - 1;
                    reset();
                    lines.add(new Line(new TerminationException("\"bare\" CR in data stream.", pos)));
                    break;
                }
            }
        }

        if (readindex != 0) shift();
    }

    /*** THE CODE BELOW IS PURELY FOR TESTING ***/
    /*
    synchronized private void status() {
        System.out.println("\n--------------------------------------------------\n");
        if (lines.isEmpty()) System.out.println("Lines: None");
        else {
            System.out.println("Lines:");
            java.util.Iterator i = lines.iterator();
            while (i.hasNext()) {
                Line line = (Line) i.next();
                try {
                    System.out.println("\tData[" + line.getBytes().length + "]: " + new String(line.getBytes(), 0, line.getBytes().length, "US-ASCII"));
                } catch (java.io.UnsupportedEncodingException uee) {
                } catch (TerminationException te) {
                    System.out.println("\tSyntax error at character position " + te.position() + ". CR and LF must be CRLF paired.  See RFC 2821 #2.7.1.");
                } catch (LineLengthExceededException llee) {
                    System.out.println("\tLine length exceeded. See RFC 2821 #4.5.3.1.");
                }
            }
        }

        System.out.println("Buffer Status:");
        System.out.println("\tworkline length: " + workLine.length);
        System.out.println("\tnext read index: " + readindex);
        System.out.println("\tnext scan index: " + scanindex);
        System.out.println("\tnext write index: " + writeindex);

        try {
            System.out.println("\tOld data: " + new String(workLine, 0, readindex, "US-ASCII"));
            System.out.println("\tData: " + new String(workLine, readindex, writeindex - readindex, "US-ASCII"));
        } catch (java.io.UnsupportedEncodingException uee) {
            System.err.println(uee);
        }
        System.out.println("\n--------------------------------------------------\n");
    }


    static public void main(String[] args) throws java.io.IOException {
        CRLFDelimitedByteBuffer dbb = new CRLFDelimitedByteBuffer();
        dbb.status();
        dbb.write("Hello"); dbb.status();
        dbb.write(" "); dbb.status();
        dbb.write("World."); dbb.status();
        dbb.write("\r"); dbb.status();
        dbb.write("\n"); dbb.status();
        dbb.write("\r\n"); dbb.status();
        dbb.write("\n"); dbb.status();
        dbb.write("\r\r"); dbb.status();
        dbb.write("stuff\n"); dbb.status();
        dbb.write("morestuff\r\r"); dbb.status();
        for (int i = 0; i < 2500; i++) dbb.write("\0"); dbb.status();

        while (!dbb.isEmpty()) {
            try {
                byte[] line = dbb.read();
                System.out.println("Read line[" + line.length + "]: " + new String(line, 0, line.length, "US-ASCII"));
            } catch (java.io.UnsupportedEncodingException uee) {
            } catch (TerminationException te) {
                System.out.println("Syntax error at character position " + te.position() + ". CR and LF must be CRLF paired.  See RFC 2821 #2.7.1.");
            } catch (LineLengthExceededException llee) {
                System.out.println("Line length exceeded. See RFC 2821 #4.5.3.1.");
            }
        }
        dbb.status();

        dbb.write("This is a test.\015\012.... Three dots\015\012.\015\012");
        dbb.status();

        while (!dbb.isEmpty()) {
            try {
                byte[] line = dbb.read();
                System.out.println("Read line[" + line.length + "]: " + new String(line, 0, line.length, "US-ASCII"));
            } catch (java.io.UnsupportedEncodingException uee) {
            } catch (TerminationException te) {
                System.out.println("Syntax error at character position " + te.position() + ". CR and LF must be CRLF paired.  See RFC 2821 #2.7.1.");
            } catch (LineLengthExceededException llee) {
                System.out.println("Line length exceeded. See RFC 2821 #4.5.3.1.");
            }
        }
        dbb.status();

        dbb.write("This is"); dbb.status();
        dbb.write(" a tes"); dbb.status();
        dbb.write("t.\015"); dbb.status();
        dbb.write("\012..."); dbb.status();
        dbb.write(". Three dot"); dbb.status();
        dbb.write("s\015\012.\015\012"); dbb.status();

        while (!dbb.isEmpty()) {
            try {
                String text = dbb.readString();
                System.out.println ("read : " + text);
                dbb.status();
            } catch (TerminationException te) {
                System.out.println("Syntax error at character position " + te.position() + ". CR and LF must be CRLF paired.  See RFC 2821 #2.7.1.");
            } catch (LineLengthExceededException llee) {
                System.out.println("Line length exceeded. See RFC 2821 #4.5.3.1.");
            }
        }
    }
    */
}
