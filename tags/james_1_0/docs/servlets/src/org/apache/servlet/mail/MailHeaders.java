/*
 * Copyright (c) 1998 Stefano Mazzocchi, Pierpaolo Fumagalli.  All rights reserved.
 */
 
package org.apache.servlet.mail;

import javax.servlet.*;
import java.io.*;
import java.text.*;
import java.util.*;

/**
 * This interface defines a container for mail headers. Each header must use
 * MIME format: <pre>name: value</pre>.
 *
 * @author Stefano Mazzocchi <stefano@apache.org>
 * @author Pierpaolo Fumagalli <p.fumagalli@fumagalli.org>
 * @version pre-draft 1.0 (submitted for review)
 */
public abstract class MailHeaders implements Serializable {

    /**
     * Get the header value associated with the given name. If multiple values
     * are found the first one is returned. If header is not found null is
     * returned.
     */
    public abstract String getHeader(String name);

    /**
     * Get the header values associated with the given name. If header is not
     * found null is returned.
     */
    public abstract String[] getHeaderValues(String name);

    /**
     * Returns the list of all header names.
     */
    public abstract Enumeration getHeaderNames();

    /**
     * Sets the value of the header specified by name. If name is already in
     * use, another header with the same name is created.
     */
    public void setHeader(String name, String value) {
        this.setHeader(name, value, false);
    }

    /**
     * Sets the value of the header specified by name. If name is already in
     * use, another header with the same name is created.
     */
    public abstract void setHeader(String name, String value, boolean overwrite);

    /**
     * Removes all headers associated with the specified name.
     */
    public void removeHeader(String name) {
        this.removeHeader(name, null);
    }

    /**
     * Removes the header with specified name and value. If value is null all
     * headers matching the specified name are removed.
     */
    public abstract void removeHeader(String name, String value);

    /**
     * Creates a string representation of this container. Each header must
     * be formatted as in MIME specification.
     */
    public String toString() {
        Enumeration names = this.getHeaderNames();
        StringBuffer buffer = new StringBuffer();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            String[] values = this.getHeaderValues(name);
            for (int i = 0; i < values.length; i++) {
                buffer.append(name);
                buffer.append(": ");
                buffer.append(values[i]);
                buffer.append("\n");
            }
        }
        return buffer.toString();
    }

    /** 
     * Returns the date associated with the mail message
     * using standard date format.
     */
    public Date getDate() throws ParseException {
        // NOTE: see below in setDate()
        return this.getDate("EEE, dd MMM yyyy hh:mm:ss zzzz");
    }

    /**
     * Returns the date associated with the mail message
     * using given date format.
     */
    public Date getDate(String format) throws ParseException {
        String date = this.getHeader("Date");
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.parse(date);
    }

    /** 
     * Sets the date using standard date format.
     */
    public void setDate(Date date) {
        // Normally, date is written like this example
        //      Mon, 10 Oct 1998 10:34:49 -0700
        // As specified in SimpleDateFormat, "z" is expanded
        // as "PDT", so we should find a way to be compliant.
        this.setDate(date, "EEE, dd MMM yyyy hh:mm:ss z");
    }

    /** 
     * Sets the date using given date format.
     */
    public void setDate(Date date, String format) {
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        this.setHeader("Date", formatter.format(date), true);
    }

    /** 
     * Returns the mail address associated to the reply-to field.
     */
    public MailAddress getReplyTo() {
        return new MailAddress(this.getHeader("Reply-To"));
    }

    /** 
     * Sets the mail address that should be used as the reply address.
     */
    public void setReplyTo(MailAddress replyto) {
        this.setHeader("Reply-To", replyto.toString(), true);
    }

    /** 
     * Returns the subject of the mail message.
     */
    public String getSubject() {
        return this.getHeader("Subject");
    }

    /** 
     * Sets the subject of the mail message.
     */
    public void setSubject(String subject) {
        this.setHeader("Subject", subject, true);
    }
    
    // other setXXX yet to be defined...
}