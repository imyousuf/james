/***********************************************************************
 * Copyright (c) 1999-2004 The Apache Software Foundation.             *
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
import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;

/**
 * Stores incoming Mail in the specified Repository.
 * If the "passThrough" in confs is true the mail will be returned untouched in
 * the pipe. If false will be destroyed.
 * @version 1.0.0, 24/04/1999
 *
 * @version This is $Revision: 1.8.4.4 $
 */
public class ToRepository extends GenericMailet {

    /**
     * The repository where this mailet stores mail.
     */
    private MailRepository repository;

    /**
     * Whether this mailet should allow mails to be processed by additional mailets
     * or mark it as finished.
     */
    private boolean passThrough = false;

    /**
     * The path to the repository
     */
    private String repositoryPath;

    /**
     * Initialize the mailet, loading configuration information.
     */
    public void init() {
        repositoryPath = getInitParameter("repositoryPath");
        try {
            passThrough = new Boolean(getInitParameter("passThrough")).booleanValue();
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
     * Store a mail in a particular repository.
     *
     * @param mail the mail to process
     */
    public void service(Mail genericmail) throws javax.mail.MessagingException {
        MailImpl mail = (MailImpl)genericmail;
        StringBuffer logBuffer =
            new StringBuffer(160)
                    .append("Storing mail ")
                    .append(mail.getName())
                    .append(" in ")
                    .append(repositoryPath);
        log(logBuffer.toString());
        repository.store(mail);
        if (!passThrough) {
            mail.setState(Mail.GHOST);
        }
    }

    /**
     * Return a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "ToRepository Mailet";
    }
}
