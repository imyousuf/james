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

import org.apache.james.imapserver.*;
import org.apache.james.util.Assert;

import java.util.StringTokenizer;
import java.util.List;

/**
 * Implements the UID Command for calling Commands with the fixed UID
 *
 * @author <a href="mailto:sascha@kulawik.de">Sascha Kulawik</a>
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.2 on 04 Aug 2002
 */
class UidCommand implements ImapCommand
{
    public boolean validForState( ImapSessionState state )
    {
        return ( state == ImapSessionState.SELECTED );
    }

    public boolean process( ImapRequest request, ImapSession session )
    {
       // StringTokenizer commandLine = new java.util.StringTokenizer(request.getCommandRaw());
        StringTokenizer commandLine = request.getCommandLine();
        int arguments = commandLine.countTokens();
       // StringTokenizer commandLine = request.getCommandLine();
        String command = request.getCommand();

        StringTokenizer txt = new java.util.StringTokenizer(request.getCommandRaw());
        System.out.println("UidCommand.process: #args="+txt.countTokens());
        while (txt.hasMoreTokens()) {
            System.out.println("UidCommand.process: arg='"+txt.nextToken()+"'");
        }
        if ( arguments < 3 ) {
            session.badResponse( "rawcommand='"+request.getCommandRaw()+"' #args="+request.arguments()+" Command should be <tag> <UID> <command> <command parameters>" );
            return true;
        }
        String uidCommand = commandLine.nextToken();
        System.out.println("UidCommand.uidCommand="+uidCommand);
        System.out.println("UidCommand.session="+session.getClass().getName());
        ImapCommand cmd = session.getImapCommand( uidCommand );
        System.out.println("UidCommand.cmd="+cmd);
        System.out.println("UidCommand.cmd="+cmd.getClass().getName());
        if ( cmd instanceof CommandFetch || cmd instanceof CommandStore  || cmd instanceof CopyCommand) {
            // As in RFC2060 also the COPY Command is valid for UID Command
            request.setCommand( uidCommand );
            ((ImapRequestImpl)request).setUseUIDs( true );
            cmd.process( request, session );
        } else {
            session.badResponse( "Invalid UID secondary command." );
        }
        return true;
    }
}
