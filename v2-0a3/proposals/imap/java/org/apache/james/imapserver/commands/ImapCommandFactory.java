/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver.commands;

import org.apache.avalon.framework.CascadingRuntimeException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.james.imapserver.CommandFetch;
import org.apache.james.imapserver.CommandStore;

import java.util.HashMap;
import java.util.Map;

/**
 * A factory for ImapCommand instances, provided based on the command name.
 */
public final class ImapCommandFactory
        extends AbstractLogEnabled
{
    private Map _imapCommands;
    private ImapCommand _invalidCommand;

    public ImapCommandFactory()
    {
        _invalidCommand = new InvalidCommand();

        _imapCommands = new HashMap();
        // Commands valid in any state
        // CAPABILITY, NOOP, and LOGOUT
        _imapCommands.put( "CAPABILITY", CapabilityCommand.class );
        _imapCommands.put( "NOOP", NoopCommand.class );
        _imapCommands.put( "LOGOUT", LogoutCommand.class );

        // Commands valid in NON_AUTHENTICATED state.
        // AUTHENTICATE and LOGIN
        _imapCommands.put( "AUTHENTICATE", AuthenticateCommand.class );
        _imapCommands.put( "LOGIN", LoginCommand.class );

        // Commands valid in AUTHENTICATED or SELECTED state.
        // RFC2060: SELECT, EXAMINE, CREATE, DELETE, RENAME, SUBSCRIBE, UNSUBSCRIBE, LIST, LSUB, STATUS, and APPEND
        _imapCommands.put( "SELECT", SelectCommand.class );
        _imapCommands.put( "EXAMINE", ExamineCommand.class );
        _imapCommands.put( "CREATE", CreateCommand.class );
        _imapCommands.put( "DELETE", DeleteCommand.class );
        _imapCommands.put( "RENAME", RenameCommand.class );
        _imapCommands.put( "SUBSCRIBE", SubscribeCommand.class );
        _imapCommands.put( "UNSUBSCRIBE", UnsubscribeCommand.class );
        _imapCommands.put( "LIST", ListCommand.class );
        _imapCommands.put( "LSUB", LsubCommand.class );
        _imapCommands.put( "STATUS", StatusCommand.class );
        _imapCommands.put( "APPEND", NotImplementedCommand.class );
        // RFC2342 NAMESPACE
        _imapCommands.put( "NAMESPACE", NamespaceCommand.class );
        // RFC2086 GETACL, SETACL, DELETEACL, LISTRIGHTS, MYRIGHTS
        _imapCommands.put( "GETACL", GetAclCommand.class );
        _imapCommands.put( "SETACL", SetAclCommand.class );
        _imapCommands.put( "DELETEACL", DeleteAclCommand.class );
        _imapCommands.put( "LISTRIGHTS", ListRightsCommand.class );
        _imapCommands.put( "MYRIGHTS", MyRightsCommand.class );


        // Commands only valid in SELECTED state.
        // CHECK, CLOSE, EXPUNGE, SEARCH, FETCH, STORE, COPY, and UID
        _imapCommands.put( "CHECK", CheckCommand.class );
        _imapCommands.put( "CLOSE", CloseCommand.class );
        _imapCommands.put( "COPY", CopyCommand.class );
        _imapCommands.put( "EXPUNGE", ExpungeCommand.class );
        _imapCommands.put( "FETCH", CommandFetch.class );
        _imapCommands.put( "STORE", CommandStore.class );
        _imapCommands.put( "UID", UidCommand.class );
    }

    public ImapCommand getCommand( String commandName )
    {
        Class cmdClass = (Class)_imapCommands.get( commandName.toUpperCase() );

        if ( cmdClass == null ) {
            return _invalidCommand;
        }
        else {
            return createCommand( cmdClass );
        }
    }

    private ImapCommand createCommand( Class commandClass )
    {
        try {
            ImapCommand cmd = (ImapCommand) commandClass.newInstance();
            if ( cmd instanceof LogEnabled ) {
                ((LogEnabled) cmd).enableLogging( getLogger() );
            }
            return cmd;
        }
        catch ( Exception e ) {
            throw new CascadingRuntimeException( "Could not create command instance: " + commandClass.getName(), e );
        }
    }


}
