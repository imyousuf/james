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

package org.apache.james.experimental.imapserver.decode;

import java.util.HashMap;
import java.util.Map;

import org.apache.avalon.framework.CascadingRuntimeException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.james.experimental.imapserver.ImapConstants;
import org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory;
import org.apache.james.experimental.imapserver.message.ImapMessageFactory;

/**
 * A factory for ImapCommand instances, provided based on the command name.
 * Command instances are created on demand, when first accessed.
 *
 * @version $Revision: 109034 $
 */
class ImapCommandParserFactory extends AbstractLogEnabled
{
    private Map _imapCommands;
    private final ImapMessageFactory messageFactory;
    private final Imap4Rev1CommandFactory commandFactory;
    
    public ImapCommandParserFactory(final ImapMessageFactory messageFactory, final Imap4Rev1CommandFactory commandFactory)
    {
        this.messageFactory = messageFactory;
        this.commandFactory = commandFactory;
        _imapCommands = new HashMap();

        // Commands valid in any state
        // CAPABILITY, NOOP, and LOGOUT
        _imapCommands.put( ImapConstants.CAPABILITY_COMMAND_NAME, CapabilityCommandParser.class );
        _imapCommands.put( ImapConstants.NOOP_COMMAND_NAME, NoopCommandParser.class );
        _imapCommands.put( ImapConstants.LOGOUT_COMMAND_NAME, LogoutCommandParser.class );

        // Commands valid in NON_AUTHENTICATED state.
        // AUTHENTICATE and LOGIN
        _imapCommands.put( ImapConstants.AUTHENTICATE_COMMAND_NAME, AuthenticateCommandParser.class );
        _imapCommands.put( ImapConstants.LOGIN_COMMAND_NAME, LoginCommandParser.class );

        // Commands valid in AUTHENTICATED or SELECTED state.
        // RFC2060: SELECT, EXAMINE, CREATE, DELETE, RENAME, SUBSCRIBE, UNSUBSCRIBE, LIST, LSUB, STATUS, and APPEND
        _imapCommands.put( ImapConstants.SELECT_COMMAND_NAME, SelectCommandParser.class );
        _imapCommands.put( ImapConstants.EXAMINE_COMMAND_NAME, ExamineCommandParser.class );
        _imapCommands.put( ImapConstants.CREATE_COMMAND_NAME, CreateCommandParser.class );
        _imapCommands.put( ImapConstants.DELETE_COMMAND_NAME, DeleteCommandParser.class );
        _imapCommands.put( ImapConstants.RENAME_COMMAND_NAME, RenameCommandParser.class );
        _imapCommands.put( ImapConstants.SUBSCRIBE_COMMAND_NAME, SubscribeCommandParser.class );
        _imapCommands.put( ImapConstants.UNSUBSCRIBE_COMMAND_NAME, UnsubscribeCommandParser.class );
        _imapCommands.put( ImapConstants.LIST_COMMAND_NAME, ListCommandParser.class );
        _imapCommands.put( ImapConstants.LSUB_COMMAND_NAME, LsubCommandParser.class );
        _imapCommands.put( ImapConstants.STATUS_COMMAND_NAME, StatusCommandParser.class );
        _imapCommands.put( ImapConstants.APPEND_COMMAND_NAME, AppendCommandParser.class );

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
        _imapCommands.put( ImapConstants.CHECK_COMMAND_NAME, CheckCommandParser.class );
        _imapCommands.put( ImapConstants.CLOSE_COMMAND_NAME, CloseCommandParser.class );
        _imapCommands.put( ImapConstants.EXPUNGE_COMMAND_NAME, ExpungeCommandParser.class );
        _imapCommands.put( ImapConstants.COPY_COMMAND_NAME, CopyCommandParser.class );
        _imapCommands.put( ImapConstants.SEARCH_COMMAND_NAME, SearchCommandParser.class );
        _imapCommands.put( ImapConstants.FETCH_COMMAND_NAME, FetchCommandParser.class );
        _imapCommands.put( ImapConstants.STORE_COMMAND_NAME, StoreCommandParser.class );
        _imapCommands.put( ImapConstants.UID_COMMAND_NAME, UidCommandParser.class );
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
            initialiseParser(commandClass, logger, cmd);
            return cmd;
        }
        catch ( Exception e ) {
            if (logger.isWarnEnabled()) {
                logger.warn("Create command instance failed: ", e);
            }
            // TODO: would probably be better to manage this in protocol
            // TODO: this runtime will produce a nasty disconnect for the client
            throw new CascadingRuntimeException( "Could not create command instance: " + commandClass.getName(), e );
        }
    }

    protected void initialiseParser(Class commandClass, final Logger logger, ImapCommandParser cmd) {
        setupLogger(cmd);
        if (logger.isDebugEnabled()) {
            logger.debug("Created command " + commandClass); 
        }
        
        if ( cmd instanceof DelegatingImapCommandParser ) {
            ( ( DelegatingImapCommandParser) cmd ).setParserFactory( this );
        }
        
        if (cmd instanceof MessagingImapCommandParser) {
            ((MessagingImapCommandParser) cmd).setMessageFactory(messageFactory);
        }
        
        if (cmd instanceof InitialisableCommandFactory) {
            ((InitialisableCommandFactory) cmd).init(commandFactory);
        }
    }

}
