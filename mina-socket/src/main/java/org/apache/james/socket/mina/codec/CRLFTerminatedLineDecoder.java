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


package org.apache.james.socket.mina.codec;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

/**
 * LineDecoder which buffer the input till a CRLF was found. It will throw an exception if a maxlinelength was 
 * reached to prevent an DOS attack. The default is 2048 chars (including CRLF).
 *
 */
public class CRLFTerminatedLineDecoder extends CumulativeProtocolDecoder {

    private int maxLineLength;
    
    public static int DEFAULT_MAX_LINE_LENTH = 2048;
    
    /**
     * Construct new instance with a max line length given (chars per line). 
     * 
     * @param maxLineLength the maximal chars per line. If the length is exceed it will throw
     *                      an LineLengthExceededException while decoding   
     */
    public CRLFTerminatedLineDecoder(int maxLineLength) {
        this.maxLineLength = maxLineLength;
    }
    
    /**
     * Construct a new instance with a max line length of 2048 chars
     */
    public CRLFTerminatedLineDecoder() {
        this(DEFAULT_MAX_LINE_LENTH);
    }
    
    
    /*
     * (non-Javadoc)
     * @see org.apache.mina.filter.codec.CumulativeProtocolDecoder#doDecode(org.apache.mina.core.session.IoSession, org.apache.mina.core.buffer.IoBuffer, org.apache.mina.filter.codec.ProtocolDecoderOutput)
     */
    protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
       
      

        // Remember the initial position.
        int start = in.position();

        
        // Now find the first CRLF in the buffer.
        byte previous = 0;
        int count = 0;
        while (in.hasRemaining()) {
            byte current = in.get();
            count++;
            
            // max line lenth exceed. Throw exception to prevent DOS
            if (maxLineLength != -1 && count > maxLineLength) {
                throw new LineLengthExceededException(maxLineLength, in.capacity());
            }
            
            if (previous == '\r' && current == '\n') {
                // Remember the current position and limit.
                int position = in.position();
                int limit = in.limit();
                try {
                    in.position(start);
                    in.limit(position);
                    // The bytes between in.position() and in.limit()
                    // now contain a full CRLF terminated line.
                    out.write(in.slice());
                } finally {
                    // Set the position to point right after the
                    // detected line and set the limit to the old
                    // one.
                    in.position(position);
                    in.limit(limit);
                }
                // Decoded one line; CumulativeProtocolDecoder will
                // call me again until I return false. So just
                // return true until there are no more lines in the
                // buffer.
                return true;
            }

            previous = current;
        }

        // Could not find CRLF in the buffer. Reset the initial
        // position to the one we recorded above.
        in.position(start);

        return false;
    }

}
