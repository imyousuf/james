/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.mailet;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.ParseException;


/**
 * A representation of an email address.
 * <p>This class encapsulates functionalities to access to different
 * parts of an email address without dealing with its parsing.
 * <p>
 * A MailAddress is an address specified in the MAIL FROM and
 * RCPT TO commands in SMTP sessions.  These are either passed by
 * an external server to the mailet-compliant SMTP server, or they
 * are created programmatically by the mailet-compliant server to
 * send to another (external) SMTP server.  Mailets and matchers
 * use the MailAddress for the purpose of evaluating the sender
 * and recipient(s) of a message.
 * <p>
 * MailAddress parses an email address as defined in RFC 821
 * (SMTP) p. 30 and 31 where addresses are defined in BNF convention.
 * As the mailet API does not support the aged "SMTP-relayed mail"
 * addressing protocol, this leaves all addresses to be a <mailbox>,
 * as per the spec.  The MailAddress's "user" is the <local-part> of
 * the <mailbox> and "host" is the <domain> of the mailbox.
 * <p>
 * This class is a good way to validate email addresses as there are
 * some valid addresses which would fail with a simpler approach
 * to parsing address.  It also removes parsing burden from
 * mailets and matchers that might not realize the flexibility of an
 * SMTP address.  For instance, "serge@home"@lokitech.com is a valid
 * SMTP address (the quoted text serge@home is the user and
 * lokitech.com is the host).  This means all current parsing to date
 * is incorrect as we just find the first @ and use that to separate
 * user from host.
 * <p>
 * This parses an address as per the BNF specification for <mailbox>
 * from RFC 821 on page 30 and 31, section 4.1.2. COMMAND SYNTAX.
 * http://www.freesoft.org/CIE/RFC/821/15.htm
 *
 * @version 1.0
 * @author Roberto Lo Giacco <rlogiacco@mail.com>
 * @author Serge Knystautas <sergek@lokitech.com>
 */
public class MailAddress implements java.io.Serializable {
    private final static char[] SPECIAL = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
            11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 127,
            '<', '>', '(', ')', '[', ']', '\\', '.', ',', ';', ':', '@', '\"'};

    private String user = null;
    private String host = null;

    /**
     * Construct a MailAddress parsing the provided <code>String</code> object.
     * <p>The <code>personal</code> variable is left empty.
     *
     * @param   address the email address compliant to the RFC822 format
     * @throws  ParseException    if the parse failed
     */
    public MailAddress(String address) throws ParseException {
        /* NEEDS TO BE COMPLETELY REWORKED */
        int pos = address.indexOf("@");
        if (pos == -1) {
            throw new ParseException("invalid address");
        }
        user = address.substring(0, pos);
        host = address.substring(pos + 1);
    }

    /**
     * Construct a MailAddress with the provided personal name and email
     * address.
     *
     * @param   user        the username or account name on the mail server
     * @param   host        the server that should accept messages for this user
     * @throws  ParseException    if the parse failed
     */
    public MailAddress(String newUser, String newHost) {
        /* NEEDS TO BE REWORKED TO VALIDATE EACH CHAR */
        user = newUser;
        host = newHost;
    }

    /**
     * Return the host part.
     *
     * @return  a <code>String</code> object representing the host part
     *          of this email address.
     * @throws  AddressException    if the parse failed
     */
    public String getHost() {
        return host;
    }

    /**
     * Return the user part.
     *
     * @return  a <code>String</code> object representing the user part
     *          of this email address.
     * @throws  AddressException    if the parse failed
     */
    public String getUser() {
        return user;
    }

    public String toString() {
        return user + "@" + host;
    }

    public boolean equals(Object object) {
        if (object instanceof MailAddress || object instanceof String) {
            return object.toString().equals(toString());
        } else {
            return false;
        }
    }

    public InternetAddress toInternetAddress() {
        try {
            return new InternetAddress(toString());
        } catch (javax.mail.internet.AddressException ae) {
            //impossible really
            return null;
        }
    }
}