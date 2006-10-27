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

import org.apache.james.transport.mailets.listservcommands.IListServCommand;
import org.apache.james.util.XMLResources;
import org.apache.mailet.Mailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.UsersRepository;
import org.apache.avalon.framework.configuration.ConfigurationException;

import javax.mail.MessagingException;
import java.util.Map;
import java.util.Properties;

/**
 * ICommandListservManager is the interface that describes the functionality of any
 * command based list serv managers.
 *
 * In order to obtain a reference to one, you can call:
 * <pre>
 * ICommandListservManager mgr = (ICommandListservManager)mailetContext.getAttribute(ICommandListservManager.ID + listName);
 * </pre>
 *
 * @version CVS $Revision$ $Date$
 * @since 2.2.0
 */
public interface ICommandListservManager extends Mailet {

    public static final String ID = ICommandListservManager.class.getName();

    /**
     * Get the name of this list
     * @param displayFormat is whether you want a display version of this or not
     * @return the official display name of this list
     */
    public String getListName(boolean displayFormat);

    /**
     * Gets the owner of this list
     * @return this is an address like listOwner@localhost
     */
    public String getListOwner();

    /**
     * Get the domain of the list
     * @return a string like localhost
     */
    public String getListDomain();

    /**
     * Get a specific command
     * @param name case in-sensitive
     * @return a {@link IListServCommand} if found, null otherwise
     */
    public IListServCommand getCommand(String name);

    /**
     * Get all the available commands
     * @return a map of {@link IListServCommand}s
     */
    public Map getCommands();

    /**
     * Based on the to address get a valid or command or null
     * @param mailAddress
     * @return IListServCommand or null
     */
    public IListServCommand getCommandTarget(MailAddress mailAddress);

    /**
     * Get the current user repository for this list serv
     * @return an instance of {@link UsersRepository} that is used for the member list of the list serv
     */
    public UsersRepository getUsersRepository();

    /**
     * An error occurred, send some sort of message to the sender
     * @param subject the subject of the message to send
     * @param mail
     * @param errorMessage
     */
    public void onError(Mail mail, String subject, String errorMessage) throws MessagingException;

    /**
     * @return the configuration file for the xml resources
     */
    public String getResourcesFile();

    /**
     * Use this to get standard properties for future calls to {@link org.apache.james.util.XMLResources}
     * @return properties with the "LIST_NAME" and the "DOMAIN_NAME" properties
     */
    public Properties getStandardProperties();

    /**
     * Initializes an array of resources
     * @param names such as 'header, footer' etc...
     * @return an initialized array of XMLResources
     * @throws org.apache.avalon.framework.configuration.ConfigurationException
     */
    public XMLResources[] initXMLResources(String[] names) throws ConfigurationException;
}
