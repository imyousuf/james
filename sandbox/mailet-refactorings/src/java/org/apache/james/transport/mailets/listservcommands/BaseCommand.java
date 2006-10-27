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



package org.apache.james.transport.mailets.listservcommands;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.james.transport.mailets.ICommandListservManager;
import org.apache.mailet.RFC2822Headers;
import org.apache.james.util.XMLResources;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetContext;
import org.apache.mailet.UsersRepository;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Properties;

/**
 * BaseCommand is a convience base class for any class that wishes to implement {@link IListServCommand}.
 * It provides some functions like:
 * <ul>
 *  <li>{@link #log}
 *  <li>{@link #sendStandardReply}
 *  <li>{@link #generateMail}
 * </ul>
 *
 * <br />
 * <br />
 *
 * @version CVS $Revision$ $Date$
 * @since 2.2.0
 * @see org.apache.james.transport.mailets.CommandListservManager
 */
public abstract class BaseCommand implements IListServCommand {

    protected Configuration configuration;
    protected ICommandListservManager commandListservManager;
    protected String commandName;
    protected MailetContext mailetContext;

    /**
     * Perform any required initialization
     * @param configuration
     * @throws ConfigurationException
     */
    public void init(ICommandListservManager commandListservManager, Configuration configuration) throws ConfigurationException {
        this.commandListservManager = commandListservManager;
        this.configuration = configuration;
        commandName = configuration.getAttribute("name");
        mailetContext = this.commandListservManager.getMailetConfig().getMailetContext();
        log("Initialized listserv command: [" + commandName + ", " + getClass().getName() + "]");
    }

    /**
     * The name of this command
     * @see IListServCommand#getCommandName
     */
    public String getCommandName() {
        return commandName;
    }

    /**
     * @see Configuration
     */
    protected Configuration getConfiguration() {
        return configuration;
    }

    /**
     * The list serv manager
     * @return {@link ICommandListservManager}
     */
    protected ICommandListservManager getCommandListservManager() {
        return commandListservManager;
    }

    /**
     * The current mailet context
     * @return {@link MailetContext}
     */
    protected MailetContext getMailetContext() {
        return mailetContext;
    }

    /**
     * @see ICommandListservManager#getUsersRepository
     */
    protected UsersRepository getUsersRepository() {
        return commandListservManager.getUsersRepository();
    }

    /**
     * Writes the specified message to a mailet log file, prepended by
     * the mailet's name.
     *
     * @param message - a String specifying the message to be written to the log file
     */
    protected void log(String message) {
        StringBuffer logBuffer =
                new StringBuffer(256)
                .append(getCommandName())
                .append(": ")
                .append(message);
        mailetContext.log(logBuffer.toString());
    }

    /**
     * Writes an explanatory message and a stack trace for a given Throwable
     * exception to the mailet log file, prepended by the mailet's name.
     *
     * @param message - a String that describes the error or exception
     * @param t - the java.lang.Throwable error or exception
     */
    protected void log(String message, Throwable t) {
        StringBuffer logBuffer =
                new StringBuffer(256)
                .append(getCommandName())
                .append(": ")
                .append(message);
        mailetContext.log(logBuffer.toString(), t);
    }

    /**
     * Produces a standard response replyAddress to the sender
     * @param origMail
     * @param subject
     * @param message
     * @param replyAddress an optional custom replyAddress address
     * @throws MessagingException
     *
     * @see #generateMail
     * @see MailetContext#sendMail
     */
    protected void sendStandardReply(Mail origMail, String subject, String message, String replyAddress) throws MessagingException {
        MailAddress senderAddress = origMail.getSender();
        try {
            MimeMessage mimeMessage = generateMail(senderAddress.toString(),
                    senderAddress.getUser(),
                    getCommandListservManager().getListOwner(),
                    getCommandListservManager().getListName(true),
                    subject,
                    message);
            if (replyAddress != null) {
                mimeMessage.setHeader(RFC2822Headers.REPLY_TO, replyAddress);
            }

            getMailetContext().sendMail(mimeMessage);
        } catch (Exception e) {
            throw new MessagingException(e.getMessage(), e);
        }
    }

    /**
     * Use this to get standard properties for future calls to {@link org.apache.james.util.XMLResources}
     * @return properties with the "LIST_NAME" and the "DOMAIN_NAME" properties
     */
    protected Properties getStandardProperties() {
        return commandListservManager.getStandardProperties();
    }

    /**
     * Send mail
     *
     * @param destEmailAddr the destination email addr: user@server.com
     * @param destDisplayName the display name
     * @param fromEmailAddr
     * @param fromDisplayName
     * @param emailSubject
     * @param emailPlainText
     * @throws Exception
     */
    protected MimeMessage generateMail(String destEmailAddr,
                                       String destDisplayName,
                                       String fromEmailAddr,
                                       String fromDisplayName,
                                       String emailSubject,
                                       String emailPlainText) throws Exception {
        MimeMessage message = new MimeMessage(Session.getDefaultInstance(System.getProperties(), null));

        InternetAddress[] toAddrs = InternetAddress.parse(destEmailAddr, false);
        toAddrs[0].setPersonal(destDisplayName);
        InternetAddress from = new InternetAddress(fromEmailAddr);
        from.setPersonal(fromDisplayName);

        message.setRecipients(Message.RecipientType.TO, toAddrs);
        message.setFrom(from);
        message.setSubject(emailSubject);
        message.setSentDate(new java.util.Date());

        MimeMultipart msgbody = new MimeMultipart();
        MimeBodyPart html = new MimeBodyPart();
        html.setDataHandler(new DataHandler(new MailDataSource(emailPlainText, "text/plain")));
        msgbody.addBodyPart(html);
        message.setContent(msgbody);
        return message;
    }

    protected XMLResources[] initXMLResources(String[] names) throws ConfigurationException {
        return commandListservManager.initXMLResources(names);
    }
}
