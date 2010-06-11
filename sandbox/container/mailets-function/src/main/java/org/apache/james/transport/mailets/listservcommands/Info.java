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



package org.apache.james.transport.mailets.listservcommands;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.transport.mailets.ICommandListservManager;
import org.apache.james.util.XMLResources;
import org.apache.mailet.Mail;

import javax.mail.MessagingException;
import java.util.Iterator;
import java.util.Properties;

/**
 * Info handles the info command.
 * It is configured by:
 * <pre>&lt;command name="info" class="Info"/&gt;</pre>
 *
 * <br />
 * <br />
 *
 * It uses the formatted text-based resources for its return mail body:
 * <ul>
 *  <li>header
 *  <li>info
 *  <li>admincommands
 * </ul>
 *
 * <br />
 * <br />
 * After formatting the text, the message is delivered with {@link #sendStandardReply(Mail, String, String, String)}
 *
 * Todo: make displaying the current member list optional
 *
 * @version CVS $Revision$ $Date$
 * @since 2.2.0
 */
public class Info extends BaseCommand {

    //For resources
    protected XMLResources[] xmlResources;

    protected static final int HEADER = 0;
    protected static final int INFO = 1;
    protected static final int ADMIN_COMMANDS = 2;

    public void init(ICommandListservManager commandListservManager, Configuration configuration) throws ConfigurationException {
        super.init(commandListservManager, configuration);
        xmlResources = initXMLResources(new String[]{"header", "info", "admincommands"});
    }

    /**
     * Process the info command using the following text resources:
     * <ul>
     *  <li>{@link #HEADER}
     *  <li>{@link #INFO}
     *  <li>{@link #ADMIN_COMMANDS}
     * </ul>
     *
     * @param mail
     */
    public void onCommand(Mail mail) throws MessagingException {
        //send info mail
        Properties props = getStandardProperties();
        props.put("MEMBER_LIST", getMemberList());

        StringBuffer plainTextMessage = new StringBuffer();
        String header = xmlResources[HEADER].getString("text", props);
        plainTextMessage.append(header);

        String infoMail = xmlResources[INFO].getString("text", props);
        plainTextMessage.append(infoMail);

        String adminCommands = xmlResources[ADMIN_COMMANDS].getString("text", props);
        plainTextMessage.append(adminCommands);

        String subject = xmlResources[INFO].getString("info.subject", props);

        sendStandardReply(mail, subject, plainTextMessage.toString(), null);
    }

    /**
     * Retrieve the current member list
     * @return the formatted member list
     *
     * @see BaseCommand#getUsersRepository
     */
    protected String getMemberList() {

        StringBuffer buffer = new StringBuffer(0x1000);
        buffer.append("\r\n");
        UsersRepository usersRepository = getUsersRepository();
        for (Iterator it = usersRepository.list(); it.hasNext();) {
            String userName = (String) it.next();
            buffer.append("    ").append(userName);
            buffer.append("\r\n");
        }

        return buffer.toString();
    }
}
