/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.mailet;
import java.util.Collection;
import java.util.Iterator;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
/**
 * Defines a set of methods that a mailet or matcher uses to communicate
 * with its mailet container, for example, to send a new message, to
 * deliver a message locally, or write to a log file.
 *
 * The MailetContext object is contained within the MailetConfig and
 * MatcherConfig objects, which the mailet container provides the
 * mailets and matchers when they are initialized.
 *
 * @version 1.0.0, 24/04/1999
 */
public interface MailetContext {
    /**
     * Bounces the message using a standard format with the given message.
     * Will be sent back to the sender from the postmaster as specified for
     * this mailet context, adding message to top of mail server queue using
     * sendMail().
     *
     * @param mail - the message that is to be bounced and sender to whom to return the message
     * @param message - a descriptive message as to why the message bounced
     */
    void bounce(Mail mail, String message) throws MessagingException;
    /**
     * Bounces the email message using the provided email address as the
     * sender of the bounce.
     *
     * @param mail - the message that is to be bounced and sender to whom to return the message
     * @param message - a descriptive message as to why the message bounced
     * @param bouncer - the address to give as the sender of the bounced message
     */
    void bounce(Mail mail, String message, MailAddress bouncer) throws MessagingException;
    /**
     * Returns a Collection of Strings of hostnames or ip addresses that
     * are specified as mail server listeners for the given hostname.
     * This is done using MX records, and the hostnames or ip addresses
     * are returned sorted by MX priority.
     *
     * @param host - the domain name for which to find mail servers
     * @return a Collection of Strings of hostnames, sorted by priority
     */
    Collection getMailServers(String host);
    /**
     * Returns the postmaster's address for this mailet context.
     *
     * @return a MailAddress of the Postmaster's address
     */
    MailAddress getPostmaster();
    /**
     * Returns the mailet container attribute with the given name, or null
     * if there is no attribute by that name.  An attribute allows a mailet container
     * to give the mailet additional information not already provided by this interface.
     * See * your server documentation for information about its attributes. A list of
     * supported attributes can be retrieved using getAttributeNames.
     * <p>
     * The attribute is returned as a java.lang.Object or some subclass. Attribute
     * names should follow the same convention as package names. The Java Mailet API
     * specification reserves names matching java.*, javax.*, and sun.*
     *
     * @param name - a String specifying the name of the attribute
     * @return an Object containing the value of the attribute, or null if no attribute
     *      exists matching the given name
     */
    Object getAttribute(String name);
    /**
     * Returns an Iterator containing the attribute names available within
     * this mailet context.  Use the getAttribute(java.lang.String) method with an
     * attribute name to get the value of an attribute.
     *
     * @return an Iterator of attribute names
     */
    Iterator getAttributeNames();
    /**
     * Returns the minor version of the Mailet API that this mailet
     * container supports. All implementations that comply with Version 1.2 must have
     * this method return the integer 1.
     *
     * @return 1
     */
    int getMajorVersion();
    /**
     * Returns the minor version of the Mailet API that this mailet
     * container supports.  All implementations that comply with Version 1.2 must have
     * this method return the integer 2.
     *
     * @return 2
     */
    int getMinorVersion();
    /**
     * Returns the name and version of the mailet container on which
     * the mailet is running.
     * <p>
     * The form of the returned string is servername/versionnumber. For example,
     * JAMES may return the string JAMES/1.2.
     * <p>
     * The mailet container may return other optional information after the primary
     * string in parentheses, for example, JAMES/1.2 (JDK 1.3.0; Windows NT 4.0 x86).
     *
     * @return a String containing at least the mailet container name and version number
     */
    String getServerInfo();
    /**
     * Checks if a server is serviced by mail context
     *
     * @param serverName - name of server.
     * @return true if server is local, i.e. serviced by this mail context
     */
    boolean isLocalServer(String serverName);
    /**
     * Checks if a user account is exists in the mail context.
     *
     * @param userAccount - user identifier.
     * @return true if the acount is a local account
     */
    boolean isLocalUser(String userAccount);
    /**
     * Writes the specified message to a mailet log file, usually an event
     * log.  The name and type of the mailet log file is specific to the mailet
     * container.
     *
     * @param msg - a String specifying the message to be written to the log file
     */
    void log(String message);
    /**
     * Writes an explanatory message and a stack trace for a given Throwable
     * exception to the mailet log file.
     *
     * @param message - a String that describes the error or exception
     * @param throwable - the Throwable error or exception
     */
    void log(String message, Throwable t);
    /**
     * Removes the attribute with the given name from the mailet context.  After
     * removal, subsequent calls to getAttribute(java.lang.String) to retrieve
     * the attribute's value will return null.
     *
     * @param name - a String specifying the name of the attribute to be removed
     */
    void removeAttribute(String name);
    /**
     * Send an outgoing message to the top of this mailet container's root queue.
     * This is the equivalent of opening an SMTP session to localhost.
     * This uses sender and recipients as specified in the message itself.
     *
     * @param msg - the MimeMessage of the headers and body content of the outgoing message
     * @throws MessagingException - if the message fails to parse
     */
    void sendMail(MimeMessage msg) throws MessagingException;
    /**
     * Send an outgoing message to the top of this mailet container's root queue.
     * Is the equivalent of opening an SMTP session to localhost.
     *
     * @param sender - the sender of the message
     * @param recipients - a Collection of MailAddress objects of recipients
     * @param msg - the MimeMessage of the headers and body content of the outgoing message
     * @throws MessagingException - if the message fails to parse
     */
    void sendMail(MailAddress sender, Collection recipients, MimeMessage msg)
        throws MessagingException;
    /**
     * Send an outgoing message to the top of this mailet container queue for the
     * appropriate processor that is specified.
     *
     * @param sender - the sender of the message
     * @param recipients - a Collection of MailAddress objects of recipients
     * @param msg - the MimeMessage of the headers and body content of the outgoing message
     * @param state - the state of the message, indicates which processor to use
     * @throws MessagingException - if the message fails to parse
     */
    void sendMail(MailAddress sender, Collection recipients, MimeMessage msg, String state)
        throws MessagingException;
    /**
     * Binds an object to a given attribute name in this mailet context.  If the name
     * specified is already used for an attribute, this method will remove the old
     * attribute and bind the name to the new attribute.
     * <p>
     * Attribute names should follow the same convention as package names. The Java
     * Mailet API specification reserves names matching java.*, javax.*, and sun.*.
     *
     * @param name - a String specifying the name of the attribute
     * @param object - an Object representing the attribute to be bound
     */
    void setAttribute(String name, Object object);
    /**
     * Stores mail into local accounts (POP3 by default)
     *
     * @param sender - the sender of the incoming message
     * @param recipient - the user who is receiving this message (as a complete email address)
     * @param msg - the MimeMessage to store in a local mailbox
     * @throws MessagingException - if the message fails to parse
     */
    void storeMail(MailAddress sender, MailAddress recipient, MimeMessage msg)
        throws MessagingException;
    /**
     * Method getMailRepository.
     * @param specificationURL
     * @return MailRepository
     * @throws MessagingException
     */
    MailRepository getMailRepository(String specificationURL) throws MessagingException;
    /**
     * Method getMailSpool.
     * @param specificationURL
     * @return SpoolRepository
     * @throws MessagingException
     */
    SpoolRepository getMailSpool(String specificationURL) throws MessagingException;
    /**
     * Method getUserRepository.
     * @param repositoryName
     * @return UsersRepository
     * @throws MessagingException
     */
    UsersRepository getUserRepository(String repositoryName) throws MessagingException;

}
