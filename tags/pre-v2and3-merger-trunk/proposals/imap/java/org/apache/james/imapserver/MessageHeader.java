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

package org.apache.james.imapserver;

import java.io.Serializable;

/**
 * Class for holding the name-value pairs of an RFC822 or MIME header.
 * Like javax.mail.Header but serializable
 *
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1 on 14 Dec 2000
 */
public class MessageHeader implements Serializable {
    public static final String CRLF =  "\r\n";
    public static final String CRLFHTAB =  "\r\n\t";
    public static final String CRLFWS = "\r\n ";

    private final String name;
    private final String value;

    public MessageHeader(String headerLine) {
        int colon = headerLine.indexOf(":");
        name = headerLine.substring(0, colon);
        StringBuffer unwrapped = new StringBuffer(headerLine.length());
        boolean finished = false;
        int pos = colon + 1;
        while (!finished) {
            int crlf = headerLine.indexOf(CRLF, pos);
            if (crlf == -1) {
                unwrapped.append(headerLine.substring(pos));
                finished = true;
            } else {
                unwrapped.append(headerLine.substring(pos, crlf));
                unwrapped.append(" ");
                pos = crlf +3;
            }
        }
        value = unwrapped.toString();
    }

    public MessageHeader(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Get the name, aka field name, of this header.
     *
     * @return a String
     */
    public String getName() {
        return name;
    }

    /**
     * Get the value, aka field value, of this Header
     *
     * @return String
     */
    public String getValue() {
        return value;
    }
}


