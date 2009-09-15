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


package org.apache.james.smtpserver.core.filter.fastfail;

import java.util.Iterator;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.james.dsn.DSNStatus;
import org.apache.james.smtpserver.Configurable;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.smtpserver.hook.HookResult;
import org.apache.james.smtpserver.hook.HookReturnCode;
import org.apache.james.smtpserver.hook.MessageHook;
import org.apache.james.util.scanner.SpamAssassinInvoker;
import org.apache.mailet.Mail;

/**
 * This MessageHandler could be used to check message against spamd before
 * accept the email. So its possible to reject a message on smtplevel if a
 * configured hits amount is reached. The handler add the follow attributes to
 * the mail object:<br>
 * org.apache.james.spamassassin.status - Holds the status
 * org.apache.james.spamassassin.flag - Holds the flag <br>
 * 
 * Sample Configuration: <br>
 * <br>
 * &lt;handler class="org.apache.james.smtpserver.SpamAssassinHandler"&gt;
 * &lt;spamdHost&gt;localhost&lt;/spamdHost&gt;
 * &lt;spamdPort&gt;783&lt;/spamdPort&gt; <br>
 * &lt;spamdRejectionHits&gt;15.0&lt;/spamdRejectionHits&gt;
 * &lt;checkAuthNetworks&gt;false&lt;/checkAuthNetworks&gt; &lt;/handler&gt;
 */
public class SpamAssassinHandler implements MessageHook, Configurable {

    /**
     * The port spamd is listen on
     */
    private int spamdPort = 783;

    /**
     * The host spamd is runnin on
     */
    private String spamdHost = "localhost";

    /**
     * The hits on which the message get rejected
     */
    private double spamdRejectionHits = 0.0;

    private boolean checkAuthNetworks = false;

    /**
     * @see org.apache.james.smtpserver.Configurable#configure(org.apache.commons.configuration.Configuration)
     */
    public void configure(Configuration config) throws ConfigurationException {
        setSpamdHost(config.getString("spamdHost","localhost"));
        setSpamdPort(config.getInt("spamdPort",783));
        setSpamdRejectionHits(config.getDouble("spamdRejectionHits", 0.0));
        setCheckAuthNetworks(config.getBoolean("checkAuthNetworks", false));
    }

    /**
     * Set to true if AuthNetworks should be included in the EHLO check
     * 
     * @param checkAuthNetworks
     *            Set to true to enable
     */
    public void setCheckAuthNetworks(boolean checkAuthNetworks) {
        this.checkAuthNetworks = checkAuthNetworks;
    }

    /**
     * Set the host the spamd daemon is running at
     * 
     * @param spamdHost
     *            The spamdHost
     */
    public void setSpamdHost(String spamdHost) {
        this.spamdHost = spamdHost;
    }

    /**
     * Set the port the spamd damon is listen on
     * 
     * @param spamdPort
     *            the spamdPort
     */
    public void setSpamdPort(int spamdPort) {
        this.spamdPort = spamdPort;
    }

    /**
     * Set the hits on which the message will be rejected.
     * 
     * @param spamdRejectionHits
     *            The hits
     */
    public void setSpamdRejectionHits(double spamdRejectionHits) {
        this.spamdRejectionHits = spamdRejectionHits;

    }

    /**
     * @see org.apache.james.smtpserver.hook.MessageHook#onMessage(org.apache.james.smtpserver.SMTPSession, org.apache.mailet.Mail)
     */
    public HookResult onMessage(SMTPSession session, Mail mail) {

        // Not scan the message if relaying allowed
        if (session.isRelayingAllowed() && !checkAuthNetworks) {
            return null;
        }

        try {
            MimeMessage message = mail.getMessage();
            SpamAssassinInvoker sa = new SpamAssassinInvoker(spamdHost,
                    spamdPort);
            sa.scanMail(message);

            Iterator headers = sa.getHeadersAsAttribute().keySet().iterator();

            // Add the headers
            while (headers.hasNext()) {
                String key = headers.next().toString();

                mail.setAttribute(key, (String) sa.getHeadersAsAttribute().get(
                        key));
            }

            // Check if rejectionHits was configured
            if (spamdRejectionHits > 0) {
                try {
                    double hits = Double.parseDouble(sa.getHits());

                    // if the hits are bigger the rejectionHits reject the
                    // message
                    if (spamdRejectionHits <= hits) {
                        StringBuffer buffer = new StringBuffer(256).append(
                                "Rejected message from ").append(
                                session.getState().get(SMTPSession.SENDER)
                                        .toString()).append(" from host ")
                                .append(session.getRemoteHost()).append(" (")
                                .append(session.getRemoteIPAddress()).append(") This message reach the spam hits treshold. Required rejection hits: ")
                                .append(spamdRejectionHits).append(" hits: ")
                                .append(hits);
                        session.getLogger().info(buffer.toString());

                        // Message reject .. abort it!
                        return new HookResult(HookReturnCode.DENY,DSNStatus.getStatus(DSNStatus.PERMANENT,
                                        DSNStatus.SECURITY_OTHER) + " This message reach the spam hits treshold. Please contact the Postmaster if the email is not SPAM. Message rejected");
                    }
                } catch (NumberFormatException e) {
                    // hits unknown
                }
            }
        } catch (MessagingException e) {
            session.getLogger().error(e.getMessage());
        }
        return new HookResult(HookReturnCode.DECLINED);
    }
}
