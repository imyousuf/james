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

import javax.mail.internet.MimeUtility;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;


/**
 * Simple Base64 string decoding function
 *
 * @version This is $Revision$
 */

public class Base64 {

    public static BufferedReader decode(String b64string) throws Exception {
        return new BufferedReader(
                   new InputStreamReader(
                       MimeUtility.decode(
                            new ByteArrayInputStream(
                                b64string.getBytes()), "base64")));
    }

    public static String decodeAsString(String b64string) throws Exception {
        if (b64string == null) {
            return b64string;
        }
        String returnString = decode(b64string).readLine();
        if (returnString == null) {
            return returnString;
        }
        return returnString.trim();
    }

    public static ByteArrayOutputStream encode(String plaintext)
            throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] in = plaintext.getBytes();
        ByteArrayOutputStream inStream = new ByteArrayOutputStream();
        inStream.write(in, 0, in.length);
        // pad
        if ((in.length % 3 ) == 1){
            inStream.write(0);
            inStream.write(0);
        } else if((in.length % 3 ) == 2){
            inStream.write(0);
        }
        inStream.writeTo( MimeUtility.encode(out, "base64")  );
        return out;
    }

    public static String encodeAsString(String plaintext) throws Exception {
        return  encode(plaintext).toString();
    }


}
