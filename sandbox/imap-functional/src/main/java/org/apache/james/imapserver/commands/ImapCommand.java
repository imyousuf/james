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

import org.apache.james.imapserver.ImapRequestLineReader;
import org.apache.james.imapserver.ImapResponse;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ImapSessionState;

/**
 * Represents a processor for a particular Imap command. Implementations of this
 * interface should encpasulate all command specific processing.
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision$
 */
public interface ImapCommand
{
    /**
     * @return the name of the command, as specified in rfc2060.
     */
    String getName();

    /**
     * Specifies if this command is valid for the given session state.
     * @param state The current {@link org.apache.james.imapserver.ImapSessionState state} of the {@link org.apache.james.imapserver.ImapSession}
     * @return <code>true</code> if the command is valid in this state.
     */
    boolean validForState( ImapSessionState state );

    /**
     * Performs all processing of the current Imap request. Reads command
     * arguments from the request, performs processing, and writes responses
     * back to the request object, which are sent to the client.
     * @param request The current client request
     * @param response The current server response
     * @param session The current session
     */
    void process( ImapRequestLineReader request,
                  ImapResponse response,
                  ImapSession session );
}
