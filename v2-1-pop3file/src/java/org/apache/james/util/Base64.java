/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.util;

import javax.mail.internet.MimeUtility;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;


/**
 * Simple Base64 string decoding function
 * @author Jason Borden <jborden@javasense.com>
 *
 * @version This is $Revision: 1.5 $
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
