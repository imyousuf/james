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



package org.apache.james.services;

import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.util.Collection;

/**
 * The interface for Phoenix blocks to the James MailServer
 *
 *
 * @version This is $Revision$
 */
public interface MailServer
{
    /**
     * The component role used by components implementing this service
     */
    String ROLE = "org.apache.james.services.MailServer";

    /**
     * Reserved user name for the mail delivery agent for multi-user mailboxes
     */
    String MDA = "JamesMDA";

    /**
     * Reserved user name meaning all users for multi-user mailboxes
     */
    String ALL = "AllMailUsers";

    /**
     * Pass a MimeMessage to this MailServer for processing
     *
     * @param sender - the sender of the message
     * @param recipients - a Collection of String objects of recipients
     * @param msg - the MimeMessage of the headers and body content of
     * the outgoing message
     * @throws MessagingException - if the message fails to parse
     * 
     * @deprecated You can use MailetContext service for this purpose
     */
    void sendMail(MailAddress sender, Collection<MailAddress> recipients, MimeMessage msg)
        throws MessagingException;

    /**
     * Pass a MimeMessage to this MailServer for processing
     *
     * @param sender - the sender of the message
     * @param recipients - a Collection of String objects of recipients
     * @param msg - an InputStream containing the headers and body content of
     * the outgoing message
     * @throws MessagingException - if the message fails to parse
     * 
     * @deprecated You can use MailetContext service for this purpose
     */
    void sendMail(MailAddress sender, Collection<MailAddress> recipients, InputStream msg)
        throws MessagingException;

    /**
     *  Pass a Mail to this MailServer for processing
     *  
     * @param mail the Mail to be processed
     * @throws MessagingException
     */
    void sendMail(Mail mail)
        throws MessagingException;
        
    /**
     * Pass a MimeMessage to this MailServer for processing
     * 
     * @param message the message
     * @throws MessagingException
     * 
     * @deprecated You can use MailetContext service for this purpose
     */
    void sendMail(MimeMessage message)
        throws MessagingException;        

    /**
     * Retrieve the primary mailbox for userName. For POP3 style stores this
     * is their (sole) mailbox.
     *
     * @param userName - the name of the user
     * @return a reference to an initialised mailbox
     */
    MailRepository getUserInbox(String userName);

    /**
     * Generate a new identifier/name for a mail being processed by this server.
     *
     * @return the new identifier
     */
    String getId();

    /**
     * Adds a new user to the mail system with userName. For POP3 style stores
     * this may only involve adding the user to the UsersStore.
     *
     * @param userName - the name of the user
     * @return a reference to an initialised mailbox
     * 
     * @deprecated addUser should not be considered a property of a MailServer
     * We could have readonly userbases providing full MailServer implementations.
     * Look at the UsersRepository.addUser(username, password) method.
     */
    boolean addUser(String userName, String password);

    /**
     * Checks if a server is serviced by mail context
     *
     * @param serverName - name of server.
     * @return true if server is local, i.e. serviced by this mail context
     */
    boolean isLocalServer(String serverName);
    
    /**
     * Return true if virtualHosting support is enabled, otherwise false
     * 
     * @return true or false
     */
    boolean supportVirtualHosting();
    
    /**
     * Return the default domain which will get used to deliver mail to if only the localpart
     * was given on rcpt to.
     * 
     * @return the defaultdomain
     */
    String getDefaultDomain();
    
    /**
     * Return the helloName which should use for all services by default
     * 
     * @return the helloName
     */
    String getHelloName();
}
