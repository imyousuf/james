/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
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
 * @version $Revision: 1.5.2.2 $
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
        _imapCommands.put( SubscribeCommand.NAME, SubscribeCommand.class );
        _imapCommands.put( UnsubscribeCommand.NAME, UnsubscribeCommand.class );
        _imapCommands.put( ListCommand.NAME, ListCommand.class );
        _imapCommands.put( LsubCommand.NAME, LsubCommand.class );
        _imapCommands.put( StatusCommand.NAME, StatusCommand.class );
        _imapCommands.put( AppendCommand.NAME, AppendCommand.class );

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
        _imapCommands.put( CheckCommand.NAME, CheckCommand.class );
        _imapCommands.put( CloseCommand.NAME, CloseCommand.class );
        _imapCommands.put( ExpungeCommand.NAME, ExpungeCommand.class );
        _imapCommands.put( CopyCommand.NAME, CopyCommand.class );
        _imapCommands.put( SearchCommand.NAME, SearchCommand.class );
        _imapCommands.put( FetchCommand.NAME, FetchCommand.class );
        _imapCommands.put( StoreCommand.NAME, StoreCommand.class );
        _imapCommands.put( UidCommand.NAME, UidCommand.class );
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
            if ( cmd instanceof UidCommand ) {
                ( ( UidCommand) cmd ).setCommandFactory( this );
            }
            return cmd;
        }
        catch ( Exception e ) {
            throw new CascadingRuntimeException( "Could not create command instance: " + commandClass.getName(), e );
        }
    }

}
