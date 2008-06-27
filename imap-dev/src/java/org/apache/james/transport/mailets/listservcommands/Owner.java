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

import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.james.transport.mailets.ICommandListservManager;

import javax.mail.MessagingException;
import javax.mail.internet.ParseException;
import java.util.List;
import java.util.ArrayList;

/**
 * This command will send email to the current owner(s) of this mailing list
 *
 * @version CVS $Revision$ $Date$
 * @since 2.2.0
 */
public class Owner extends BaseCommand {

    protected List m_listOwner = new ArrayList();

    /**
     * Perform any required initialization
     * @param configuration
     * @throws ConfigurationException
     */
    public void init(ICommandListservManager commandListservManager, Configuration configuration) throws ConfigurationException {
        super.init(commandListservManager, configuration);
        try {
            m_listOwner.add(new MailAddress(getCommandListservManager().getListOwner()));
        } catch (ParseException e) {
            throw new ConfigurationException(e.getMessage(), e);
        }
    }

    /**
     * Process this command to your hearts content
     * @param mail
     * @throws MessagingException
     */
    public void onCommand(Mail mail) throws MessagingException {
        getMailetContext().sendMail(mail.getSender(), m_listOwner, mail.getMessage());
    }
}
