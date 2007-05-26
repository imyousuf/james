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

package org.apache.james.experimental.imapserver.commands;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.experimental.imapserver.ImapConstants;
import org.apache.james.experimental.imapserver.ImapSessionState;

/**
 * Base class for all command implementations. This class provides common
 * core functionality useful for all {@link org.apache.james.experimental.imapserver.commands.ImapCommand} implementations.
 *
 * @version $Revision: 109034 $
 */
abstract public class CommandTemplate
        extends AbstractLogEnabled
        implements ImapCommand, ImapConstants
{
    /**
     * By default, valid in any state (unless overridden by subclass.
     * @see org.apache.james.experimental.imapserver.commands.ImapCommand#validForState
     */
    public boolean validForState( ImapSessionState state )
    {
        return true;
    }
    
    /**
     * Provides a message which describes the expected format and arguments
     * for this command. This is used to provide user feedback when a command
     * request is malformed.
     *
     * @return A message describing the command protocol format.
     */
    public String getExpectedMessage()
    {
        StringBuffer syntax = new StringBuffer( "<tag> " );
        syntax.append( getName() );

        String args = getArgSyntax();
        if ( args != null && args.length() > 0 ) {
            syntax.append( " " );
            syntax.append( args );
        }

        return syntax.toString();
    }

    /**
     * Provides the syntax for the command arguments if any. This value is used
     * to provide user feedback in the case of a malformed request.
     *
     * For commands which do not allow any arguments, <code>null</code> should
     * be returned.
     *
     * @return The syntax for the command arguments, or <code>null</code> for
     *         commands without arguments.
     */
    public abstract String getArgSyntax();
}
