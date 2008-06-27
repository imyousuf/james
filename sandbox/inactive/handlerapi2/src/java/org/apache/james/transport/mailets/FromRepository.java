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



package org.apache.james.transport.mailets;

import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.Constants;
import org.apache.james.services.MailRepository;
import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;

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
 * @version This is $Revision$
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

        ServiceManager compMgr = (ServiceManager)getMailetContext().getAttribute(Constants.AVALON_COMPONENT_MANAGER);
        try {
            Store mailstore = (Store) compMgr.lookup(Store.ROLE);
            DefaultConfiguration mailConf
                = new DefaultConfiguration("repository", "generated:ToRepository");
            mailConf.setAttribute("destinationURL", repositoryPath);
            mailConf.setAttribute("type", "MAIL");
            repository = (MailRepository) mailstore.select(mailConf);
        } catch (ServiceException cnfe) {
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
                Mail mail =  repository.retrieve(key);
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
                    ContainerUtil.dispose(mail);
                }
            } catch (MessagingException e) {
                log((new StringBuffer(160).append("Unable to re-spool mail ").append(key).append(" from ").append(repositoryPath)).toString(), e);
            }
        }

        if (delete) {
            Iterator delList = processed.iterator();
            while (delList.hasNext()) {
                repository.remove((String)delList.next());
            }
        }
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
