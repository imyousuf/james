/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver.commands;

import org.apache.james.imapserver.ImapRequestParser;
import org.apache.james.imapserver.ImapResponse;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ImapSessionState;

/**
 * Represents a processor for a particular Imap command. Implementations of this
 * interface should encpasulate all command specific processing.
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.1 $
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
    void process( ImapRequestParser request,
                  ImapResponse response,
                  ImapSession session );
}
