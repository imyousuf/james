/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.util;

import java.io.*;
import javax.mail.internet.*;

/**
 * Simple Base64 string decoding function
 * @author Jason Borden <jborden@javasense.com>  
 */

public class Base64 {

    public static BufferedReader decode(String b64string) throws Exception {
        return new BufferedReader(new InputStreamReader(MimeUtility.decode(new ByteArrayInputStream(b64string.getBytes()), "base64")));
    }
}
