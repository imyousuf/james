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

package org.apache.james.mailboxmanager.impl;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.Namespace;
import org.apache.james.mailboxmanager.Namespaces;
import org.apache.james.mailboxmanager.mailbox.MailboxSession;
import org.apache.james.mailboxmanager.manager.MailboxManager;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;
import org.apache.james.services.User;

public class DefaultMailboxManagerProvider extends AbstractLogEnabled implements MailboxManagerProvider, Configurable, Serviceable, Initializable {

    public static final char HIERARCHY_DELIMITER = '.';

    public static final String USER_NAMESPACE = "#mail";

    public static final String INBOX = "INBOX";

    private MailboxManager mailboxManager;

    private ServiceManager serviceManager;


    public MailboxSession getInboxSession(User user) throws MailboxManagerException {
        return getMailboxManagerInstance().getMailboxSession(getInboxName(user),true);
    }

    public MailboxManager getMailboxManagerInstance()
            throws MailboxManagerException {
        return mailboxManager;
    }

    public MailboxSession getMailboxSession(User authUser, String mailboxName,
            boolean autoCreate) throws MailboxManagerException {
        return getMailboxManagerInstance().getMailboxSession(
                mailboxName, autoCreate);
    }

    public Namespaces getNamespaces(User forUser) {
        NamespacesImpl nameSpaces = new NamespacesImpl();
        nameSpaces.setShared(new Namespace[0]);
        Namespace userNamespace = new NamespaceImpl("" + HIERARCHY_DELIMITER,
                USER_NAMESPACE);
        nameSpaces.setUser(new Namespace[] { userNamespace });
        Namespace personalDefault = getPersonalDefaultNamespace(forUser);
        nameSpaces.setPersonal(new Namespace[] { personalDefault });
        nameSpaces.setPersonalDefault(personalDefault);
        return nameSpaces;
    }

    public Namespace getPersonalDefaultNamespace(User forUser) {
        return new NamespaceImpl("" + HIERARCHY_DELIMITER, USER_NAMESPACE
                + HIERARCHY_DELIMITER + forUser.getUserName());
    }

    String getInboxName(User user) {
        Namespace nameSpace=getPersonalDefaultNamespace(user);
        return nameSpace.getName()+nameSpace.getHierarchyDelimter()+INBOX;
    }

    public void configure(Configuration conf) throws ConfigurationException {
        Configuration factoryConf=conf.getChild("factory",false);
        String className=factoryConf.getAttribute("class");
        MailboxManager factory= (MailboxManager) getClassInstace(className);
        ContainerUtil.enableLogging(factory, getLogger());
        ContainerUtil.configure(factory, factoryConf);
        setMailboxManagerInstance(factory);
    }
    
    public void setMailboxManagerInstance(MailboxManager mailboxManager) {
        this.mailboxManager = mailboxManager;
    }

    private static Object getClassInstace(String name) {
        Object object=null;
        try {
            Class clazz = Thread.currentThread().getContextClassLoader().loadClass(name);
            object=clazz.newInstance();
            return object;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
    }

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager=serviceManager;
        
    }

    public void initialize() throws Exception {
        ContainerUtil.service(mailboxManager, serviceManager);
        ContainerUtil.initialize(mailboxManager);
    }
}
