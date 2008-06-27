/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.mailet;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

/**
 * Wrap a MimeMessage with routing information (from SMTP) such
 * as SMTP specified recipients, sender, and ip address and hostname
 * of sending server.  It also contains its state which represents
 * which processor in the mailet container it is currently running.
 * Special processor names are "root" and "error".
 *
 * @author Federico Barbieri <scoobie@systemy.it>
 * @author Serge Knystautas <sergek@lokitech.com>
 * @version 0.9
 */
public interface Mail extends Serializable, Cloneable {
    String GHOST = "ghost";
    String DEFAULT = "root";
    String ERROR = "error";
    String TRANSPORT = "transport";

    /**
     * Returns the MimeMessage stored in this message
     *
     * @return the MimeMessage that this Mail object wraps
     * @throws MessagingException - an error occured while loading this object
     */
    MimeMessage getMessage() throws MessagingException;

    /**
     * Returns a Collection of MailAddress objects that are recipients of this message
     *
     * @return a Collection of MailAddress objects that are recipients of this message
     */
    Collection getRecipients();

    /**
     * The sender of the message, as specified by the MAIL FROM header, or internally defined
     *
     * @return a MailAddress of the sender of this message
     */
    MailAddress getSender();

    /**
     * The current state of the message, such as GHOST, ERROR, or DEFAULT
     *
     * @return the state of this message
     */
    String getState();

    /**
     * The remote hostname of the server that connected to send this message
     *
     * @return a String of the hostname of the server that connected to send this message
     */
    String getRemoteHost();

    /**
     * The remote ip address of the server that connected to send this message
     *
     * @return a String of the ip address of the server that connected to send this message
     */
    String getRemoteAddr();

    /**
     * The error message, if any, associated with this message.  Not sure why this is needed.
     *
     * @return a String of a descriptive error message
     */
    String getErrorMessage();

    /**
     * Sets the error message associated with this message.  Not sure why this is needed.
     *
     * @param msg - a descriptive error message
     */
    void setErrorMessage(String msg);

    /**
     * Sets the MimeMessage associated with this message via an inputstream.  The Mail
     * object will parse out the inputstream and construct a MimeMessage object.
     *
     * @param in - the inputstream to read to construct the MimeMessage
     * @throws MessagingException - if there was an error parsing the inputstream
     */
    void setMessage(InputStream in) throws MessagingException;

    /**
     * Sets the MimeMessage associated with this message via the object.
     *
     * @param message - the new MimeMessage that this Mail object will wrap
     */
    void setMessage(MimeMessage message);

    /**
     * Sets the state of this message.
     *
     * @param state - the new state of this message
     */
    void setState(String state);
}
