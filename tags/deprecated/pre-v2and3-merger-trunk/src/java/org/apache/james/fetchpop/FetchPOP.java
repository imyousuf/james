/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

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
            throw new ServiceException("",errorBuffer.toString());
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
