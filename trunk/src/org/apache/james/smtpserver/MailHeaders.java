/*
 * Copyright (c) 1998 Stefano Mazzocchi, Pierpaolo Fumagalli.  All rights reserved.
 */
 
package org.apache.james.smtpserver;

import org.apache.java.io.CharTerminatedInputStream;
import java.io.*;
import java.text.*;
import java.util.*;

/**
 * This interface defines a container for mail headers. Each header must use
 * MIME format: <pre>name: value</pre>.
 *
 * @author Federico Barbieri <scoobie@systemy.it>
 */
public class MailHeaders implements Serializable, Cloneable {

    private static final String EMPTY = "";
    private static final char[] headerTerminator = {'\r','\n','\r','\n'};
    private Vector headerNames;
    private Vector headerValues;
    
    public MailHeaders() {
        headerNames = new Vector();
        headerValues = new Vector();
    }
    
    public MailHeaders(InputStream in) {
        this();
        parse(new CharTerminatedInputStream(in, headerTerminator));
    }
    
    private void parse(InputStream in) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            for (String next = reader.readLine(); next != null && !next.equals(""); ) {
                StringBuffer buffer = new StringBuffer(next);
                next = reader.readLine();
                while (next.startsWith(" ") || next.startsWith("\t")) {
                    buffer.append(next);
                    next = reader.readLine();
                }
                String header = buffer.toString();
                int index = header.indexOf(':');
                if (index < 0) {
                    headerNames.addElement(header);
                    headerValues.addElement(EMPTY);
                } else {
                    headerNames.addElement(header.substring(0, index));
                    headerValues.addElement(header.substring(index + 1));
                }
            }
        } catch (Exception e) {
System.out.println("Exception parsing headers");
        }
    }

    public void writeTo(PrintStream writer) {
        for (int i = 0; i < headerNames.size(); i++) {
            writer.println(headerNames.elementAt(i) + ":" + headerValues.elementAt(i));
        }
    }
    
    public void writeTo(OutputStream out) {
        writeTo(new PrintStream(out));
    }
    
    public byte[] toByteArray() {
        ByteArrayOutputStream headersBytes = new ByteArrayOutputStream();
        writeTo(headersBytes);
        return headersBytes.toByteArray();
    }
    
    public String getHeader(String name) {
        int index = headerNames.indexOf(name);
        if (index < 0) {
            return null;
        } else {
            return (String) headerValues.elementAt(index);
        }
    }
    
    public void addHeader(String name, String value) {
        headerNames.addElement(name);
        value = (value != null ? value : "");
        headerValues.addElement(" " + value);
    }
    
    public void setHeader(String name, String value) {
        int index = headerNames.indexOf(name);
        if (index < 0) {
            addHeader(name, value);
        } else {
            headerNames.set(index, name);
            value = (value != null ? value : "");
            headerValues.set(index, " " + value);
        }
    }
    
    public boolean isSet(String name) {
        return headerNames.contains(name);
    }
}