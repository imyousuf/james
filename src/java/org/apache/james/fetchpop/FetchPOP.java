/**
 * FetchPOP.java
 * 
 * Copyright (C) 24-Sep-2002 The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file. 
 *
 * Danny Angus
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
import org.apache.avalon.framework.component.DefaultComponentManager;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.Logger;
import org.apache.commons.net.pop3.POP3Client;
import org.apache.commons.net.pop3.POP3MessageInfo;
import org.apache.james.services.MailServer;
/**
 *
 * A class which fetches mail from a single POP account and inserts it into the incoming spool<br>
 * <br>$Id: FetchPOP.java,v 1.1 2002/09/24 15:36:30 danny Exp $
 * @author <A href="mailto:danny@apache.org">Danny Angus</a>
 * 
 */
public class FetchPOP implements Target {
    private Configuration conf;
    private DefaultComponentManager compMgr;
    private MailServer server;
    private String popHost;
    private String popUser;
    private String popPass;
    private String popName;
    private Logger logger;
    /**
     * @see org.apache.avalon.cornerstone.services.scheduler.Target#targetTriggered(String)
     */
    public void targetTriggered(String arg0) {
        getLogger().debug(popName + " fetcher starting fetch");
        POP3Client pop = new POP3Client();
        try {
            pop.connect(popHost);
            pop.login(popUser, popPass);
            getLogger().debug("login:" + pop.getReplyString());
            pop.setState(POP3Client.TRANSACTION_STATE);
            POP3MessageInfo[] messages = pop.listMessages();
            getLogger().debug("list:" + pop.getReplyString());
            Vector recieved = new Vector();
            for (int i = 0; i < messages.length; i++) {
                InputStream in = new ReaderInputStream(pop.retrieveMessage(messages[i].number));
                getLogger().debug("retrieve:" + pop.getReplyString());
                MimeMessage message = new MimeMessage(null, in);
                in.close();
                message.addHeader("X-fetchpop", "fetched by james");
                message.saveChanges();
                logger.debug("sent message " + message.toString());
                server.sendMail(message);
                recieved.add(messages[i]);
            }
            Enumeration enum = recieved.elements();
            while (enum.hasMoreElements()) {
                POP3MessageInfo element = (POP3MessageInfo) enum.nextElement();
                pop.deleteMessage(element.number);
                getLogger().debug("delete:" + pop.getReplyString());
            }
            pop.logout();
            getLogger().debug("logout:" + pop.getReplyString());
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
     * Method configure.
     * &lt;fetchpop enabled="false"&gt;<br>
     *  &lt;!-- you can have as many fetch tasks as you want to        --&gt;<br>
     *  &lt;!-- but each must have a unique name to identify itself by --&gt;<br>
     *  &lt;fetch name="mydomain.com"&gt;<br>
     *      &lt;!-- host name or IP address --&gt;<br>
     *      &lt;host&gt;mail.mydomain.com&lt;/host&gt;<br>
     *      &lt;!-- acount login username --&gt;<br>
     *      &lt;user&gt;username&lt;/user&gt;<br>
     *      &lt;!-- account login password --&gt;<br>
     *      &lt;password&gt;pass&lt;/password&gt;<br>
     *      &lt;!-- Interval to check this account in milliseconds, 60000 is every ten minutes --&gt;<br>
     *      &lt;interval&gt;600000&lt;/interval&gt;<br>
     *  &lt;/fetch&gt;<br>
     *  &lt;/fetchpop&gt;<br>
     *  @param conf configuration element for this fetcher:<br>
     * @param server mailserver which can spool fetched mail
     * @param logger child logger of FetchScheduler
     * @throws ConfigurationException
     */
    public void configure(Configuration conf, MailServer server, Logger logger)
        throws ConfigurationException {
        logger.debug("configured fetch");
        this.conf = conf;
        this.server = server;
        this.logger = logger;
        this.popHost = conf.getChild("host").getValue();
        this.popUser = conf.getChild("user").getValue();
        this.popPass = conf.getChild("password").getValue();
        this.popName = conf.getAttribute("name");
    }
    private Logger getLogger() {
        return logger;
    }
}
