/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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
import org.apache.james.imapserver.CommandFetch;
import org.apache.james.imapserver.CommandStore;

import java.util.HashMap;
import java.util.Map;

/**
 * A factory for ImapCommand instances, provided based on the command name.
 *
 * @author <a href="mailto:sascha@kulawik.de">Sascha Kulawik</a>
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.2 on 04 Aug 2002
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
        _imapCommands.put( "APPEND", AppendCommand.class );
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
        _imapCommands.put( "SEARCH", SearchCommand.class );
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
