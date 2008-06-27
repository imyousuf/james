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

package org.apache.james.imapserver.commands;

import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.store.MailboxException;

import java.util.Collection;

/**
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.5 $
 */
class LsubCommand extends ListCommand
{
    public static final String NAME = "LSUB";

    protected Collection doList( ImapSession session, String searchPattern )
            throws MailboxException
    {
        return session.getHost().listSubscribedMailboxes( session.getUser(), searchPattern );
    }

    /** @see ImapCommand#getName */
    public String getName()
    {
        return NAME;
    }
}
