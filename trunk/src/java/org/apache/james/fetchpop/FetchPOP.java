/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.fetchpop;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Vector;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.avalon.cornerstone.services.scheduler.Target;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.component.DefaultComponentManager;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.commons.net.pop3.POP3Client;
import org.apache.commons.net.pop3.POP3MessageInfo;
import org.apache.james.services.MailServer;
/**
 *
 * A class which fetches mail from a single POP account and inserts it 
 * into the incoming spool<br>
 *
 * <br>$Id: FetchPOP.java,v 1.3 2002/09/27 14:06:55 danny Exp $
 * @author <A href="mailto:danny@apache.org">Danny Angus</a>
 * 
 */
public class FetchPOP extends AbstractLogEnabled implements Configurable, Target {

    /**
     * The MailServer service
     */
    private MailServer server;

    /**
     * The unique, identifying name for this task
     */
    private String fetchTaskName;

    /**
     * The POP3 server host name for this fetch task
     */
    private String popHost;

    /**
     * The POP3 user name for this fetch task
     */
    private String popUser;

    /**
     * The POP3 user password for this fetch task
     */
    private String popPass;

    /**
     * @see org.apache.avalon.cornerstone.services.scheduler.Target#targetTriggered(String)
     */
    public void targetTriggered(String arg0) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug(fetchTaskName + " fetcher starting fetch");
        }
        POP3Client pop = new POP3Client();
        try {
            pop.connect(popHost);
            pop.login(popUser, popPass);
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Login:" + pop.getReplyString());
            }
            pop.setState(POP3Client.TRANSACTION_STATE);
            POP3MessageInfo[] messages = pop.listMessages();
            getLogger().debug("List:" + pop.getReplyString());
            Vector received = new Vector();
            for (int i = 0; i < messages.length; i++) {
                InputStream in = new ReaderInputStream(pop.retrieveMessage(messages[i].number));
                getLogger().debug("Retrieve:" + pop.getReplyString());
                MimeMessage message = new MimeMessage(null, in);
                in.close();
                message.addHeader("X-fetched-from", fetchTaskName);
                message.saveChanges();
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("Sent message " + message.toString());
                }
                server.sendMail(message);
                received.add(messages[i]);
            }
            Enumeration enum = received.elements();
            while (enum.hasMoreElements()) {
                POP3MessageInfo element = (POP3MessageInfo) enum.nextElement();
                pop.deleteMessage(element.number);
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("Delete:" + pop.getReplyString());
                }
            }
            pop.logout();
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("logout:" + pop.getReplyString());
            }
            pop.disconnect();
        } catch (SocketException e) {
            getLogger().error(e.getMessage());
        } catch (IOException e) {
            getLogger().error(e.getMessage());
        } catch (MessagingException e) {
            getLogger().error(e.getMessage());
        }
    }

    /**
     * Pass the <code>ComponentManager</code> to the instance.
     * The instance uses the specified <code>ComponentManager</code> to 
     * acquire the components it needs for execution.
     *
     * @param componentManager The <code>ComponentManager</code> which this
     *                <code>Composable</code> uses.
     * @throws ComponentException if an error occurs
     */
    public void compose( final ComponentManager componentManager )
        throws ComponentException {
        try {
            server = (MailServer) componentManager.lookup(MailServer.ROLE);
        } catch (ClassCastException cce) {
            StringBuffer errorBuffer =
                new StringBuffer(128)
                        .append("Component ")
                        .append(MailServer.ROLE)
                        .append("does not implement the required interface.");
            throw new ComponentException(errorBuffer.toString());
        }
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration conf) throws ConfigurationException {
        this.popHost = conf.getChild("host").getValue();
        this.popUser = conf.getChild("user").getValue();
        this.popPass = conf.getChild("password").getValue();
        this.fetchTaskName = conf.getAttribute("name");
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Configured FetchPOP");
        }
    }
}
