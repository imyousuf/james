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

import java.util.ArrayList;
import java.util.Collection;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.Constants;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.api.user.UsersStore;
import org.apache.james.api.vut.ErrorMappingException;
import org.apache.james.api.vut.VirtualUserTable;
import org.apache.mailet.MailAddress;

/**
 * Receives a Mail from JamesSpoolManager and takes care of delivery of the
 * message to local inboxes.
 * 
 * Available configurations are:
 * 
 * <enableAliases>true</enableAliases>: specify wether the user aliases should
 * be looked up or not. Default is false.
 * 
 * <enableForwarding>true</enableForwarding>: enable the forwarding. Default to
 * false.
 * 
 * <usersRepository>LocalAdmins</usersRepository>: specific users repository
 * name. Default to empty. If empty does lookup the default userRepository.
 */
public class UsersRepositoryAliasingForwarding extends AbstractVirtualUserTableMailet {

    /**
     * The user repository for this mail server. Contains all the users with
     * inboxes on this server.
     */
    private UsersRepository usersRepository;

    /**
     * Return a string describing this mailet.
     * 
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "Local User Aliasing and Forwarding Mailet";
    }

    /**
     * Return null when the mail should be GHOSTed, the username string when it
     * should be changed due to the ignoreUser configuration.
     * 
     * @param sender
     * @param recipient
     * @param message
     * @throws MessagingException
     */
    public Collection processMail(MailAddress sender, MailAddress recipient,
            MimeMessage message) throws MessagingException {
        if (recipient == null) {
            throw new IllegalArgumentException(
                    "Recipient for mail to be spooled cannot be null.");
        }
        if (message == null) {
            throw new IllegalArgumentException(
                    "Mail message to be spooled cannot be null.");
        }

        if (usersRepository instanceof VirtualUserTable) {
            Collection<String> mappings;
            try {
                mappings = ((VirtualUserTable) usersRepository).getMappings(recipient.getUser(), recipient.getHost());
            } catch (ErrorMappingException e) {
                StringBuilder errorBuffer = new StringBuilder(128)
                    .append("A problem as occoured trying to alias and forward user ")
                    .append(recipient)
                    .append(": ")
                    .append(e.getMessage());
                    throw new MessagingException(errorBuffer.toString());
            }
            
            if (mappings != null) {
                return handleMappings(mappings, sender, recipient, message);
            }
        } else {
            StringBuilder errorBuffer = new StringBuilder(128)
                .append("Warning: the repository ")
                .append(usersRepository.getClass().getName())
                .append(" does not implement VirtualUserTable interface).");
            getMailetContext().log(errorBuffer.toString());
        }
        String realName = usersRepository.getRealName(recipient.getUser());
        if (realName != null) {
            ArrayList ret = new ArrayList();
            ret.add(new MailAddress(realName, recipient.getHost()));
            return ret;
        } else {
            ArrayList ret = new ArrayList();
            ret.add(recipient);
            return ret;
        }
    }

    /**
     * @see org.apache.mailet.GenericMailet#init()
     */
    public void init() throws MessagingException {
        super.init();
        ServiceManager compMgr = (ServiceManager) getMailetContext()
                .getAttribute(Constants.AVALON_COMPONENT_MANAGER);

        try {
            String userRep = getInitParameter("usersRepository");
            if (userRep == null || userRep.length() == 0) {
                try {
                    usersRepository = (UsersRepository) compMgr
                            .lookup(UsersRepository.ROLE);
                } catch (ServiceException e) {
                    log("Failed to retrieve UsersRepository component:"
                            + e.getMessage());
                }
            } else {
                UsersStore usersStore = (UsersStore) compMgr.lookup(UsersStore.ROLE);
                usersRepository = usersStore.getRepository(userRep);
            }

        } catch (ServiceException cnfe) {
            log("Failed to retrieve UsersStore component:" + cnfe.getMessage());
        }

    }

}
