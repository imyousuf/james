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

package org.apache.james.transport.mailets;

import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.james.Constants;
import org.apache.james.core.MailImpl;
import org.apache.james.services.MailRepository;
import org.apache.james.services.MailStore;
import org.apache.james.util.RFC2822Headers;
import org.apache.mailet.GenericMailet;
import org.apache.mailet.MailAddress;
import org.apache.mailet.Mail;
import javax.mail.internet.InternetAddress;
import javax.mail.MessagingException;
import java.util.Iterator;

/**
 * Re-spools Mail found in the specified Repository.
 *
 * &lt;mailet match="RecipientIs=respool@localhost" class="FromRepository"&gt;
 *    &lt;repositoryPath&gt; <i>repository path</i> &lt;/repositoryPath&gt;
 *    &lt;processor&gt; <i>target processor</i> &lt;/repositoryPath&gt;
 *    &lt;delete&t; [true|<b>false</b>] &lt;/delete&gt;
 * &lt;/mailet&gt;
 *
 * @version This is $Revision: 1.1.2.4 $
 */
public class FromRepository extends GenericMailet {

    /**
     * The repository from where this mailet spools mail.
     */
    private MailRepository repository;

    /**
     * Whether this mailet should delete messages after being spooled
     */
    private boolean delete = false;

    /**
     * The path to the repository
     */
    private String repositoryPath;

    /**
     * The processor that will handle the re-spooled message(s)
     */
    private String processor;

    /**
     * Initialize the mailet, loading configuration information.
     */
    public void init() {
        repositoryPath = getInitParameter("repositoryPath");
        processor = (getInitParameter("processor") == null) ? Mail.DEFAULT : getInitParameter("processor");

        try {
            delete = (getInitParameter("delete") == null) ? false : new Boolean(getInitParameter("delete")).booleanValue();
        } catch (Exception e) {
            // Ignore exception, default to false
        }

        ComponentManager compMgr = (ComponentManager)getMailetContext().getAttribute(Constants.AVALON_COMPONENT_MANAGER);
        try {
            MailStore mailstore = (MailStore) compMgr.lookup("org.apache.james.services.MailStore");
            DefaultConfiguration mailConf
                = new DefaultConfiguration("repository", "generated:ToRepository");
            mailConf.setAttribute("destinationURL", repositoryPath);
            mailConf.setAttribute("type", "MAIL");
            repository = (MailRepository) mailstore.select(mailConf);
        } catch (ComponentException cnfe) {
            log("Failed to retrieve Store component:" + cnfe.getMessage());
        } catch (Exception e) {
            log("Failed to retrieve Store component:" + e.getMessage());
        }
    }

    /**
     * Spool mail from a particular repository.
     *
     * @param triggering e-mail (eventually parameterize via the
     * trigger message)
     */
    public void service(Mail trigger) throws MessagingException {
        trigger.setState(Mail.GHOST);
        java.util.Collection processed = new java.util.ArrayList();
        Iterator list = repository.list();
        while (list.hasNext()) {
            String key = (String) list.next();
            try {
                MailImpl mail =  repository.retrieve(key);
                if (mail != null && mail.getRecipients() != null) {
                    log((new StringBuffer(160).append("Spooling mail ").append(mail.getName()).append(" from ").append(repositoryPath)).toString());

                    /*
                    log("Return-Path: " + mail.getMessage().getHeader(RFC2822Headers.RETURN_PATH, ", "));
                    log("Sender: " + mail.getSender());
                    log("To: " + mail.getMessage().getHeader(RFC2822Headers.TO, ", "));
                    log("Recipients: ");
                    for (Iterator i = mail.getRecipients().iterator(); i.hasNext(); ) {
                        log("    " + ((MailAddress)i.next()).toString());
                    };
                    */

                    mail.setAttribute("FromRepository", Boolean.TRUE);
                    mail.setState(processor);
                    getMailetContext().sendMail(mail);
                    if (delete) processed.add(key);
                }
            } catch (MessagingException e) {
                log((new StringBuffer(160).append("Unable to re-spool mail ").append(key).append(" from ").append(repositoryPath)).toString(), e);
            }
        }
        if (delete) repository.remove(processed);
    }

    /**
     * Return a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "FromRepository Mailet";
    }
}
