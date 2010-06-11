/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/


package org.apache.mailet;

import java.util.Collection;
import java.util.Iterator;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.apache.james.services.SpoolRepository;


/**
 * Defines a set of methods that a mailet or matcher uses to communicate
 * with its mailet container, for example, to send a new message, to
 * deliver a message locally, or write to a log file.
 *
 * The MailetContext object is contained within the MailetConfig and
 * MatcherConfig objects, which the mailet container provides to the
 * mailets and matchers when they are initialized.
 *
 * @version 1.0.0, 24/04/1999
 */
public interface MailetContext {

    /**
     * Bounces the message using a standard format with the given message.
     * The message will be sent back to the sender from the postmaster as specified for
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
     * <p>TODO: This needs to be made a more specific ordered subtype of Collection.</p>
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
     * Returns the major version of the Mailet API that this mailet
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
     * @param mailAddress - address of the account to be checked.
     * @return true if the account is a local account
     * 
     * @since James 2.4.0
     */
    boolean isLocalEmail(MailAddress mailAddress);

    /**
     * Writes the specified message to a mailet log file, usually an event
     * log.  The name and type of the mailet log file is specific to the mailet
     * container.
     *
     * @param message - a String specifying the message to be written to the log file
     */
    void log(String message);

    /**
     * Writes an explanatory message and a stack trace for a given Throwable
     * exception to the mailet log file.
     *
     * @param message - a String that describes the error or exception
     * @param t - the Throwable error or exception
     */
    void log(String message, Throwable t);

    

    /**
     * Send an outgoing message to the top of this mailet container's root queue.
     * This is the equivalent of opening an SMTP session to localhost.
     * This uses sender and recipients as specified in the message itself.
     *
     * @param msg - the MimeMessage of the headers and body content of the outgoing message
     * @throws MessagingException - if the message fails to parse
     */
    void sendMail(MimeMessage msg)
        throws MessagingException;

    /**
     * Send an outgoing message to the top of this mailet container's root queue.
     * This is the equivalent of opening an SMTP session to localhost.
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
     * This is a String that names a processor for which the message will be queued
     * @throws MessagingException - if the message fails to parse
     */
    void sendMail(MailAddress sender, Collection recipients, MimeMessage msg, String state)
        throws MessagingException;

    /**
     * Send an outgoing message to the top of this mailet container's root queue.
     * This is the equivalent of opening an SMTP session to localhost.
     * The Mail object provides all envelope and content information
     *
     * @param mail - the message that is to sent
     * @throws MessagingException - if the message fails to spool
     */
    void sendMail(Mail mail)
            throws MessagingException;

    
    

    /**
     * Returns an Iterator over HostAddress, a specialized subclass of
     * javax.mail.URLName, which provides location information for
     * servers that are specified as mail handlers for the given
     * hostname.  This is done using MX records, and the HostAddress
     * instances are returned sorted by MX priority.  If no host is
     * found for domainName, the Iterator returned will be empty and the
     * first call to hasNext() will return false.
     *
     * @since Mailet API v2.2.0a16-unstable
     * @param domainName - the domain for which to find mail servers
     * @return an Iterator over HostAddress instances, sorted by priority
     */
    Iterator getSMTPHostAddresses(String domainName);
    
    
    /**
     * 
     * Returns the MailRepository associated with the URL
     * 
     * @param repoURL
     * @return 
     * @throws MailetException
     */
    public MailRepository getMailRepository(String repoURL) throws MailetException;
    
    /**
     * @return a MailFactory implementation
     */
    public MailFactory getMailFactory();
    
/**
 * @param repoURL
 * @return
 * @throws MailetException
 */
public UsersRepository getUsersRepository(String repoURL) throws MailetException;

/**
 * @param recipient
 * @return
 * @throws MailetException 
 */
public MailRepository getMailRepository(MailAddress recipient) throws MailetException;

/**
 * @param outgoingPath
 * @return
 * @throws MailetException 
 */
public SpoolRepository getSpoolRepository(String outgoingPath)throws MailetException;
    
   }
