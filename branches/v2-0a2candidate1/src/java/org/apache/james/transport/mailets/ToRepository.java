/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.transport.mailets;

import java.util.*;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.james.*;
import org.apache.james.core.*;
import org.apache.james.services.MailRepository;
import org.apache.james.services.MailStore;
import org.apache.james.transport.*;
import org.apache.mailet.*;

/**
 * Stores incoming Mail in the specified Repository.
 * If the "passThrough" in confs is true the mail will be returned untouched in
 * the pipe. If false will be destroyed.
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 *
 * This is $Revision: 1.3 $
 * Committed on $Date: 2001/06/21 16:04:54 $ by: $Author: charlesb $ 
 */
public class ToRepository extends GenericMailet {

    private MailRepository repository;
    private boolean passThrough = false;
    private String repositoryPath;

    public void init() {
        repositoryPath = getInitParameter("repositoryPath");
        try {
            passThrough = new Boolean(getInitParameter("passThrough")).booleanValue();
        } catch (Exception e) {
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

    public void service(Mail genericmail) {
        MailImpl mail = (MailImpl)genericmail;
        log("Storing mail " + mail.getName() + " in " + repositoryPath);
        repository.store(mail);
        if (!passThrough) {
            mail.setState(Mail.GHOST);
        }
    }

    public String getMailetInfo() {
        return "ToRepository Mailet";
    }
}
