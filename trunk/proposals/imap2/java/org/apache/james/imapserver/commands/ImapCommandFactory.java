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

import java.util.HashMap;
import java.util.Map;

/**
 * A factory for ImapCommand instances, provided based on the command name.
 * Command instances are created on demand, when first accessed.
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.2 $
 */
public class ImapCommandFactory
        extends AbstractLogEnabled
{
    private Map _imapCommands;

    public ImapCommandFactory()
    {
        _imapCommands = new HashMap();

        // Commands valid in any state
        // CAPABILITY, NOOP, and LOGOUT
        _imapCommands.put( CapabilityCommand.NAME, CapabilityCommand.class );
        _imapCommands.put( NoopCommand.NAME, NoopCommand.class );
        _imapCommands.put( LogoutCommand.NAME, LogoutCommand.class );

        // Commands valid in NON_AUTHENTICATED state.
        // AUTHENTICATE and LOGIN
        _imapCommands.put( AuthenticateCommand.NAME, AuthenticateCommand.class );
        _imapCommands.put( LoginCommand.NAME, LoginCommand.class );

        // Commands valid in AUTHENTICATED or SELECTED state.
        // RFC2060: SELECT, EXAMINE, CREATE, DELETE, RENAME, SUBSCRIBE, UNSUBSCRIBE, LIST, LSUB, STATUS, and APPEND
        _imapCommands.put( SelectCommand.NAME, SelectCommand.class );
        _imapCommands.put( ExamineCommand.NAME, ExamineCommand.class );
        _imapCommands.put( CreateCommand.NAME, CreateCommand.class );
        _imapCommands.put( DeleteCommand.NAME, DeleteCommand.class );
        _imapCommands.put( RenameCommand.NAME, RenameCommand.class );
//        _imapCommands.put( "SUBSCRIBE", SubscribeCommand.class );
//        _imapCommands.put( "UNSUBSCRIBE", UnsubscribeCommand.class );
        _imapCommands.put( ListCommand.NAME, ListCommand.class );
        _imapCommands.put( LsubCommand.NAME, LsubCommand.class );
        _imapCommands.put( StatusCommand.NAME, StatusCommand.class );
//        _imapCommands.put( "APPEND", AppendCommand.class );
//        // RFC2342 NAMESPACE
//        _imapCommands.put( "NAMESPACE", NamespaceCommand.class );
//        // RFC2086 GETACL, SETACL, DELETEACL, LISTRIGHTS, MYRIGHTS
//        _imapCommands.put( "GETACL", GetAclCommand.class );
//        _imapCommands.put( "SETACL", SetAclCommand.class );
//        _imapCommands.put( "DELETEACL", DeleteAclCommand.class );
//        _imapCommands.put( "LISTRIGHTS", ListRightsCommand.class );
//        _imapCommands.put( "MYRIGHTS", MyRightsCommand.class );
//
//
//        // Commands only valid in SELECTED state.
//        // CHECK, CLOSE, EXPUNGE, SEARCH, FETCH, STORE, COPY, and UID
//        _imapCommands.put( "CHECK", CheckCommand.class );
//        _imapCommands.put( "CLOSE", CloseCommand.class );
//        _imapCommands.put( "COPY", CopyCommand.class );
//        _imapCommands.put( "EXPUNGE", ExpungeCommand.class );
//        _imapCommands.put( "SEARCH", SearchCommand.class );
//        _imapCommands.put( "FETCH", CommandFetch.class );
//        _imapCommands.put( "STORE", CommandStore.class );
//        _imapCommands.put( "UID", UidCommand.class );
    }

    public ImapCommand getCommand( String commandName )
    {
        Class cmdClass = ( Class ) _imapCommands.get( commandName.toUpperCase() );

        if ( cmdClass == null ) {
            return null;
        }
        else {
            return createCommand( cmdClass );
        }
    }

    private ImapCommand createCommand( Class commandClass )
    {
        try {
            ImapCommand cmd = ( ImapCommand ) commandClass.newInstance();
            if ( cmd instanceof LogEnabled ) {
                ( ( LogEnabled ) cmd ).enableLogging( getLogger() );
            }
            return cmd;
        }
        catch ( Exception e ) {
            throw new CascadingRuntimeException( "Could not create command instance: " + commandClass.getName(), e );
        }
    }

}
