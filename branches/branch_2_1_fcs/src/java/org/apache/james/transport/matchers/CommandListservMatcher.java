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

package org.apache.james.transport.matchers;

import org.apache.mailet.GenericRecipientMatcher;
import org.apache.mailet.MailAddress;

import javax.mail.MessagingException;

/**
 * CommandListservMatcher is the matcher that pairs with the {@link org.apache.james.transport.mailets.CommandListservManager}
 * It checks to see if the request is intended for the ListservManager, but doesn't guarantee that it is a valid command.
 * <br />
 * To configure, insert this into the config.xml inside of the root processor block.
 * <pre>
 * &lt;mailet match="CommandListservMatcher=announce@localhost" class="CommandListservManager"&gt;
 * ...
 * &lt;/mailet&gt;
 * </pre>
 *
 * @version CVS $Revision: 1.1.2.3 $ $Date: 2004/03/15 03:54:21 $
 * @since 2.2.0
 * @see org.apache.james.transport.mailets.CommandListservManager
 */
public class CommandListservMatcher extends GenericRecipientMatcher {

    private MailAddress listservAddress;

    public void init() throws MessagingException {
        listservAddress = new MailAddress(getCondition());
    }

    /**
     * This doesn't perform an exact match, but checks to see if the request is at lesast
     * intended to go to the list serv manager.
     * @param recipient
     * @return true if matches, false otherwise
     */
    public boolean matchRecipient(MailAddress recipient) {
        if (recipient.getHost().equals(listservAddress.getHost())) {
            if (recipient.getUser().startsWith(listservAddress.getUser() + "-")) {
                return true;
            }
        }
        return false;
    }
}
