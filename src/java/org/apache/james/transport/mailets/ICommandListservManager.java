/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */
package org.apache.james.transport.mailets;

import org.apache.james.transport.mailets.listservcommands.IListServCommand;
import org.apache.james.services.UsersRepository;
import org.apache.james.util.XMLResources;
import org.apache.mailet.Mailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
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
 * @version CVS $Revision: 1.1.2.2 $ $Date: 2003/07/06 11:53:55 $
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
