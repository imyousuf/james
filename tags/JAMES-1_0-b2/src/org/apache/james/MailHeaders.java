/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
 
package org.apache.james;

import java.io.*;
import java.text.*;
import java.util.*;
import javax.mail.internet.*;
import javax.mail.MessagingException;

/**
 * This interface defines a container for mail headers. Each header must use
 * MIME format: <pre>name: value</pre>.
 *
 * @author Federico Barbieri <scoobie@systemy.it>
 */
public class MailHeaders extends InternetHeaders implements Serializable, Cloneable {
    
    public MailHeaders()
    throws MessagingException {
        super();
    }

    public MailHeaders(InputStream in)
    throws MessagingException {
        super(in);
    }

    public void writeTo(PrintStream writer) {
        for (Enumeration e = super.getAllHeaderLines(); e.hasMoreElements(); ) {
            writer.println((String) e.nextElement());
        }
        writer.println("");
    }
    
    public void writeTo(OutputStream out) {
        writeTo(new PrintStream(out));
    }
    
    public byte[] toByteArray() {
        ByteArrayOutputStream headersBytes = new ByteArrayOutputStream();
        writeTo(headersBytes);
        return headersBytes.toByteArray();
    }
    
    public boolean isSet(String name) {
        return super.getHeader(name).length != 0;
    }
    
    public boolean isValid() {
            // Check if MimeMessage contains REQUIRED headers fields as specified in RFC 822.
        if (super.getHeader("Date").length == 0) return false;
        if (super.getHeader("To").length == 0) return false;
        if (super.getHeader("From").length == 0) return false;
        return true;
    }
}