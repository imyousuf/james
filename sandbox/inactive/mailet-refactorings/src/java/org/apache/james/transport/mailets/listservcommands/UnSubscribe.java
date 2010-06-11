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
import org.apache.james.transport.mailets.ICommandListservManager;
import org.apache.james.util.XMLResources;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.UsersRepository;

import javax.mail.MessagingException;
import java.util.Properties;

/**
 * UnSubscribe handles the unsubscribe command.
 * It is configured by:
 * <pre>&lt;command name="unsubscribe" class="UnSubscribe"/&gt;</pre>
 *
 * <br />
 * <br />
 *
 * It uses the formatted text-based resources for its return mail body:
 * <ul>
 *  <li>unsubscribe
 * </ul>
 *
 * <br />
 * <br />
 * After formatting the text, the message is delivered with {@link #sendStandardReply}
 *
 * Note, prior to formatting and sending any text, the user is checked to see that they
 * are currently subscribed to this list.  If so, they will be sent a confirmation mail to
 * be processed by {@link UnSubscribeConfirm}
 *
 * @version CVS $Revision$ $Date$
 * @since 2.2.0
 * @see UnSubscribeConfirm
 */
public class UnSubscribe extends BaseCommand {

    //For resources
    protected XMLResources xmlResources;

    public void init(ICommandListservManager commandListservManager, Configuration configuration) throws ConfigurationException {
        super.init(commandListservManager, configuration);
        xmlResources = initXMLResources(new String[]{"unsubscribe"})[0];
    }

    /**
     * After ensuring that the user is currently subscribed, confirmation mail
     * will be sent to be processed by {@link UnSubscribeConfirm}.
     * @param mail
     * @throws MessagingException
     */
    public void onCommand(Mail mail) throws MessagingException {
        if (checkSubscriptionStatus(mail)) {
            //send confirmation mail
            Properties props = getStandardProperties();
            props.put("SENDER_ADDR", mail.getSender().toString());

            String confirmationMail = xmlResources.getString("text", props);
            String subject = xmlResources.getString("confirm.unsubscribe.subject", props);
            String replyAddress = xmlResources.getString("confirm.unsubscribe.address", props);

            sendStandardReply(mail, subject, confirmationMail, replyAddress);
        }
    }

    /**
     * Checks to see that this user is already subscribed, if not return false and send a message
     * @param mail
     * @return false if the user isn't subscribed, true otherwise
     * @throws MessagingException
     */
    protected boolean checkSubscriptionStatus(Mail mail) throws MessagingException {
        MailAddress mailAddress = mail.getSender();
        UsersRepository usersRepository = getUsersRepository();
        if (!usersRepository.contains(mailAddress.toString())) {
            getCommandListservManager().onError(mail,
                    "Invalid request",
                    xmlResources.getString("not.subscribed", getStandardProperties()));
            return false;
        }
        return true;
    }
}
