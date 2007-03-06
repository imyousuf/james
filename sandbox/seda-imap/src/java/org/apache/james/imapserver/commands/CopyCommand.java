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

package org.apache.james.imapserver.commands;

import org.apache.james.imapserver.ImapConstants;


/**
 * Handles processeing for the COPY imap command.
 *
 * @version $Revision: 109034 $
 */
class CopyCommand extends SelectedStateCommand
{
    public static final String ARGS = "<message-set> <mailbox>";

    /** @see ImapCommand#getName */
    public String getName()
    {
        return ImapConstants.COPY_COMMAND_NAME;
    }

    /** @see CommandTemplate#getArgSyntax */
    public String getArgSyntax()
    {
        return ARGS;
    }
}
/*
6.4.7.  COPY Command

   Arguments:  message set
               mailbox name

   Responses:  no specific responses for this command

   Result:     OK - copy completed
               NO - copy error: can't copy those messages or to that
                    name
               BAD - command unknown or arguments invalid

      The COPY command copies the specified message(s) to the end of the
      specified destination mailbox.  The flags and internal date of the
      message(s) SHOULD be preserved in the copy.

      If the destination mailbox does not exist, a server SHOULD return
      an error.  It SHOULD NOT automatically create the mailbox.  Unless
      it is certain that the destination mailbox can not be created, the
      server MUST send the response code "[TRYCREATE]" as the prefix of
      the text of the tagged NO response.  This gives a hint to the
      client that it can attempt a CREATE command and retry the COPY if
      the CREATE is successful.

      If the COPY command is unsuccessful for any reason, server
      implementations MUST restore the destination mailbox to its state
      before the COPY attempt.

   Example:    C: A003 COPY 2:4 MEETING
               S: A003 OK COPY completed
*/
