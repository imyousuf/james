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

package org.apache.james.transport.mailets.listservcommands;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.james.transport.mailets.ICommandListservManager;
import org.apache.mailet.Mail;

import javax.mail.MessagingException;

/**
 * IListServCommand is the interface that all pluggable list serv commands must implement.
 * The lifecycle of a IListServCommand will be controlled by the {@link ICommandListservManager}
 *
 * <br />
 * <br />
 * Requests sent to the CommandListservManager take the form of:
 * <pre>
 * &lt;listName&gt;-&lt;commandName&gt;@domain
 * </pre>
 * and if the commandName matches the command's name, then the {@link #onCommand} will be invoked.
 *
 * <br />
 * <br />
 * A typical command is configured:
 * <pre>
 * &lt;command name="subscribe" class="Subscribe"/&gt;
 * </pre>
 *
 * <br />
 * <br />
 * Typically, IListServCommands will format some text to reply with based off of resource files
 * and calls to {@link org.apache.james.util.XMLResources#getString}
 *
 * This allows you to customize the messages sent by these commands by editing text files and not editing the javacode.
 *
 * @version CVS $Revision$ $Date$
 * @since 2.2.0
 * @see ICommandListservManager
 */
public interface IListServCommand {

    /**
     * The name of this command
     * specified by the 'name' parameter.
     * eg:
     * <pre>
     * &lt;command name="subscribe" class="Subscribe"/&gt;
     * </pre>
     * @return the name of this command
     */
    public String getCommandName();

    /**
     * Perform any required initialization
     * @param configuration
     * @throws ConfigurationException
     */
    public void init(ICommandListservManager commandListservManager, Configuration configuration) throws ConfigurationException;

    /**
     * Process this command to your hearts content
     * @param mail
     * @throws MessagingException
     */
    public void onCommand(Mail mail) throws MessagingException;
}
