/***********************************************************************
 * Copyright (c) 2000-2006 The Apache Software Foundation.             *
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
import org.apache.james.util.XMLResources;
import org.apache.james.services.UsersRepository;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import javax.mail.MessagingException;
import java.util.Properties;

/**
 * UnSubscribeConfirm handles the unsubscribe-confirm command.
 * It is configured by:
 * <pre>&lt;command name="unsubscribe-confirm" class="UnSubscribeConfirm"/&gt;</pre>
 *
 * <br />
 * <br />
 *
 * It uses the formatted text-based resources for its return mail body:
 * <ul>
 *  <li>unsubscribe-confirm
 * </ul>
 *
 * <br />
 * <br />
 * After formatting the text, the message is delivered with {@link #sendStandardReply}
 *
 * <br />
 * <br />
 * This command basically sends a goodbye message and removes the user from the mailing list.
 *
 * @version CVS $Revision$ $Date$
 * @since 2.2.0
 * @see UnSubscribe
 */
public class UnSubscribeConfirm extends BaseCommand {

    //For resources
    protected XMLResources xmlResources;

    public void init(ICommandListservManager commandListservManager, Configuration configuration) throws ConfigurationException {
        super.init(commandListservManager, configuration);
        xmlResources = initXMLResources(new String[]{"unsubscribeConfirm"})[0];
    }

    /**
     * After ensuring that the user is currently subscribed, remove the user to the
     * mailing list, and send a goodbye message.
     *
     * @param mail
     * @throws MessagingException
     */
    public void onCommand(Mail mail) throws MessagingException {
        if (checkSubscriptionStatus(mail)) {
            getUsersRepository().removeUser(mail.getSender().toString());

            //send mail
            Properties props = getStandardProperties();
            props.put("SENDER_ADDR", mail.getSender().toString());

            String confirmationMail = xmlResources.getString("text", props);
            String subject = xmlResources.getString("goodbye.subscribe.address", props);

            sendStandardReply(mail, subject, confirmationMail, null);
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
