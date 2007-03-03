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

import java.util.HashMap;
import java.util.Map;

import org.apache.avalon.framework.CascadingRuntimeException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;

/**
 * A factory for ImapCommand instances, provided based on the command name.
 * Command instances are created on demand, when first accessed.
 *
 * @version $Revision: 109034 $
 */
public class ImapCommandParserFactory
        extends AbstractLogEnabled
{
    private Map _imapCommands;

    public ImapCommandParserFactory()
    {
        _imapCommands = new HashMap();

        // Commands valid in any state
        // CAPABILITY, NOOP, and LOGOUT
        _imapCommands.put( CapabilityCommand.NAME, CapabilityCommandParser.class );
        _imapCommands.put( NoopCommand.NAME, NoopCommandParser.class );
        _imapCommands.put( LogoutCommand.NAME, LogoutCommandParser.class );

        // Commands valid in NON_AUTHENTICATED state.
        // AUTHENTICATE and LOGIN
        _imapCommands.put( AuthenticateCommand.NAME, AuthenticateCommandParser.class );
        _imapCommands.put( LoginCommand.NAME, LoginCommandParser.class );

        // Commands valid in AUTHENTICATED or SELECTED state.
        // RFC2060: SELECT, EXAMINE, CREATE, DELETE, RENAME, SUBSCRIBE, UNSUBSCRIBE, LIST, LSUB, STATUS, and APPEND
        _imapCommands.put( SelectCommand.NAME, SelectCommandParser.class );
        _imapCommands.put( ExamineCommand.NAME, ExamineCommandParser.class );
        _imapCommands.put( CreateCommand.NAME, CreateCommandParser.class );
        _imapCommands.put( DeleteCommand.NAME, DeleteCommandParser.class );
        _imapCommands.put( RenameCommand.NAME, RenameCommandParser.class );
        _imapCommands.put( SubscribeCommand.NAME, SubscribeCommandParser.class );
        _imapCommands.put( UnsubscribeCommand.NAME, UnsubscribeCommandParser.class );
        _imapCommands.put( ListCommand.NAME, ListCommandParser.class );
        _imapCommands.put( LsubCommand.NAME, LsubCommandParser.class );
        _imapCommands.put( StatusCommand.NAME, StatusCommandParser.class );
        _imapCommands.put( AppendCommand.NAME, AppendCommandParser.class );

//        // RFC2342 NAMESPACE
//        _imapCommands.put( "NAMESPACE", NamespaceCommand.class );

        // RFC2086 GETACL, SETACL, DELETEACL, LISTRIGHTS, MYRIGHTS
//        _imapCommands.put( "GETACL", GetAclCommand.class );
//        _imapCommands.put( "SETACL", SetAclCommand.class );
//        _imapCommands.put( "DELETEACL", DeleteAclCommand.class );
//        _imapCommands.put( "LISTRIGHTS", ListRightsCommand.class );
//        _imapCommands.put( "MYRIGHTS", MyRightsCommand.class );


        // Commands only valid in SELECTED state.
        // CHECK, CLOSE, EXPUNGE, SEARCH, FETCH, STORE, COPY, and UID
        _imapCommands.put( CheckCommand.NAME, CheckCommandParser.class );
        _imapCommands.put( CloseCommand.NAME, CloseCommandParser.class );
        _imapCommands.put( ExpungeCommand.NAME, ExpungeCommandParser.class );
        _imapCommands.put( CopyCommand.NAME, CopyCommandParser.class );
        _imapCommands.put( SearchCommand.NAME, SearchCommandParser.class );
        _imapCommands.put( FetchCommand.NAME, FetchCommandParser.class );
        _imapCommands.put( StoreCommand.NAME, StoreCommandParser.class );
        _imapCommands.put( UidCommand.NAME, UidCommandParser.class );
    }

    public ImapCommandParser getParser( String commandName )
    {
        Class cmdClass = ( Class ) _imapCommands.get( commandName.toUpperCase() );

        if ( cmdClass == null ) {
            return null;
        }
        else {
            return createCommand( cmdClass );
        }
    }

    private ImapCommandParser createCommand( Class commandClass )
    {
        final Logger logger = getLogger(); 
        try {
            ImapCommandParser cmd = ( ImapCommandParser ) commandClass.newInstance();
            setupLogger(cmd);
            if (logger.isDebugEnabled()) {
                logger.debug("Created command " + commandClass); 
            }
            // TODO: introduce interface
            if ( cmd instanceof UidCommandParser ) {
                ( ( UidCommandParser) cmd ).setCommandFactory( this );
            }
            return cmd;
        }
        catch ( Exception e ) {
            if (logger.isWarnEnabled()) {
                logger.warn("Create command instance failed: ", e);
            }
            throw new CascadingRuntimeException( "Could not create command instance: " + commandClass.getName(), e );
        }
    }

}
