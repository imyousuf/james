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

package org.apache.james.fetchpop;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Vector;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.avalon.cornerstone.services.scheduler.Target;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.commons.net.pop3.POP3Client;
import org.apache.commons.net.pop3.POP3MessageInfo;
import org.apache.james.services.MailServer;
/**
 *
 * A class which fetches mail from a single POP account and inserts it
 * into the incoming spool<br>
 *
 * @version 1.0.0, 18/06/2000
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
                MimeMessage message = null;
                try {
                    message = new MimeMessage(null, in);
                    in.close();
                    message.addHeader("X-fetched-from", fetchTaskName);
                    message.saveChanges();
                    try {
                        server.sendMail(message);
                        getLogger().debug("Sent message " + message.toString());
                        received.add(messages[i]);
                    } catch (MessagingException innerE) {
                        getLogger().error("can't insert message " + message.toString() + "created from "+messages[i].identifier);
                    }
                } catch (MessagingException outerE) {
                    getLogger().error("can't create message out of fetched message "+messages[i].identifier);
                }
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
        }
    }
    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager)
     */
    public void compose(final ServiceManager componentManager) throws ServiceException {
        try {
            server = (MailServer) componentManager.lookup(MailServer.ROLE);
        } catch (ClassCastException cce) {
            StringBuffer errorBuffer =
                new StringBuffer(128).append("Component ").append(MailServer.ROLE).append(
                    "does not implement the required interface.");
            throw new ServiceException(errorBuffer.toString());
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
            getLogger().info("Configured FetchPOP fetch task " + fetchTaskName);
        }
    }
}
