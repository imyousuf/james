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
 * Subscribe handles the subscribe-confirm command.
 * It is configured by:
 * <pre>&lt;command name="subscribe-confirm" class="SubscribeConfirm"/&gt;</pre>
 *
 * <br />
 * <br />
 *
 * It uses the formatted text-based resources for its return mail body:
 * <ul>
 *  <li>subscribe-confirm
 *  <li>admincommands
 * </ul>
 *
 * <br />
 * <br />
 * After formatting the text, the message is delivered with {@link #sendStandardReply}
 *
 * <br />
 * <br />
 * This command basically sends the welcome message and adds the user to the mailing list.
 *
 * @version CVS $Revision: 1.1.2.2 $ $Date: 2003/07/06 11:53:56 $
 * @since 2.2.0
 * @see Subscribe
 */
public class SubscribeConfirm extends BaseCommand {

    //For resources
    protected XMLResources[] xmlResources;

    protected static final int SUBSCRIBE_CONFIRM = 0;
    protected static final int ADMIN_COMMANDS = 1;

    public void init(ICommandListservManager commandListservManager, Configuration configuration) throws ConfigurationException {
        super.init(commandListservManager, configuration);
        xmlResources = initXMLResources(new String[]{"subscribeConfirm", "admincommands"});
    }

    /**
     * After ensuring that the user isn't already subscribed, add the user to the
     * mailing list, and send a welcome message.
     *
     * <br />
     * <br />
     *
     * It uses the formatted text-based resources for its return mail body:
     * <ul>
     *  <li>{@link #SUBSCRIBE_CONFIRM}
     *  <li>{@link #ADMIN_COMMANDS}
     * </ul>
     *
     * @param mail
     * @throws MessagingException
     */
    public void onCommand(Mail mail) throws MessagingException {
        if (checkSubscriptionStatus(mail)) {
            getUsersRepository().addUser(mail.getSender().toString(), "");

            //send mail
            Properties props = getStandardProperties();
            props.put("SENDER_ADDR", mail.getSender().toString());

            String confirmationMail = xmlResources[SUBSCRIBE_CONFIRM].getString("text", props);
            String adminCommands = xmlResources[ADMIN_COMMANDS].getString("text", props);
            String subject = xmlResources[SUBSCRIBE_CONFIRM].getString("welcome.subscribe.address", props);

            sendStandardReply(mail, subject, confirmationMail + adminCommands, null);
        }
    }

    /**
     * Checks to see if this user is already subscribed, if so return false and send a message
     * @param mail
     * @return false if the user is subscribed, true otherwise
     * @throws MessagingException
     */
    protected boolean checkSubscriptionStatus(Mail mail) throws MessagingException {
        MailAddress mailAddress = mail.getSender();
        UsersRepository usersRepository = getUsersRepository();
        if (usersRepository.contains(mailAddress.toString())) {
            getCommandListservManager().onError(mail,
                    "Invalid request",
                    xmlResources[SUBSCRIBE_CONFIRM].getString("already.subscribed", getStandardProperties()));
            return false;
        }
        return true;
    }
}
