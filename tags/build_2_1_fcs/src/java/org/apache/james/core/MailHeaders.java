/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.core;

import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;
import java.io.*;
import java.util.Enumeration;

import org.apache.james.util.RFC2822Headers;

/**
 * This interface defines a container for mail headers. Each header must use
 * MIME format: <pre>name: value</pre>.
 *
 * @author Federico Barbieri <scoobie@systemy.it>
 */
public class MailHeaders extends InternetHeaders implements Serializable, Cloneable {

    /**
     * No argument constructor
     *
     * @throws MessagingException if the super class cannot be properly instantiated
     */
    public MailHeaders() throws MessagingException {
        super();
    }

    /**
     * Constructor that takes an InputStream containing the contents
     * of the set of mail headers.
     *
     * @param in the InputStream containing the header data
     *
     * @throws MessagingException if the super class cannot be properly instantiated
     *                            based on the stream
     */
    public MailHeaders(InputStream in) throws MessagingException {
        super(in);
    }

// TODO: Overloading error.  This is extremely dangerous, as the overloaded call
//       does not behave like an overridden call.  Specifically, the choice of
//       which method to invoke is made at compile time, not at runtime.
//       Potentially very, very bad if the behaviors diverge.

    /**
     * Write the headers to an PrintStream
     *
     * @param writer the stream to which to write the headers
     */
    public void writeTo(PrintStream writer) {
        for (Enumeration e = super.getAllHeaderLines(); e.hasMoreElements(); ) {
            writer.println((String) e.nextElement());
        }
        writer.println("");
    }

    /**
     * Write the headers to an output stream
     *
     * @param out the stream to which to write the headers
     */
    public void writeTo(OutputStream out) {
        writeTo(new PrintStream(out));
    }

    /**
     * Generate a representation of the headers as a series of bytes.
     *
     * @return the byte array containing the headers
     */
    public byte[] toByteArray() {
        ByteArrayOutputStream headersBytes = new ByteArrayOutputStream();
        writeTo(headersBytes);
        return headersBytes.toByteArray();
    }

    /**
     * Check if a particular header is present.
     *
     * @return true if the header is present, false otherwise
     */
    public boolean isSet(String name) {
        String[] value = super.getHeader(name);
        return (value != null && value.length != 0);
    }

    /**
     * Check if all REQUIRED headers fields as specified in RFC 822
     * are present.
     *
     * @return true if the headers are present, false otherwise
     */
    public boolean isValid() {
        return (isSet(RFC2822Headers.DATE) && isSet(RFC2822Headers.TO) && isSet(RFC2822Headers.FROM));
    }
}
