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
package org.apache.james.transport.mailets.listservcommands;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.james.services.UsersRepository;
import org.apache.james.transport.mailets.ICommandListservManager;
import org.apache.james.util.RFC2822Headers;
import org.apache.james.util.XMLResources;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetContext;

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
 * @version CVS $Revision: 1.1.2.2 $ $Date: 2003/07/06 11:53:56 $
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
