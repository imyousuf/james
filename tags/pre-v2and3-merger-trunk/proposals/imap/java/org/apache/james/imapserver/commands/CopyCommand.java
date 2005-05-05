/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.imapserver.commands;

import org.apache.james.imapserver.AccessControlException;
import org.apache.james.imapserver.AuthorizationException;
import org.apache.james.imapserver.commands.ImapCommand;
import org.apache.james.imapserver.BaseCommand;
import org.apache.james.imapserver.ACLMailbox;
import org.apache.james.imapserver.ImapRequest;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.MessageAttributes;
import org.apache.james.imapserver.ImapSessionState;
import org.apache.james.imapserver.SingleThreadedConnectionHandler;
import javax.mail.internet.MimeMessage;
import org.apache.james.imapserver.Flags;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.StringTokenizer;
//import org.apache.james.core.EnhancedMimeMessage;

/**
 * Implements the IMAP UID COPY command for a given ImapRequestImpl.
 *
 * References: rfc 2060
 * @author <a href="mailto:sascha@kulawik.de">Sascha Kulawik</a>
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.2 on 04 Aug 2002
 */
public class CopyCommand
    extends BaseCommand implements ImapCommand
{
    //mainly to switch on stack traces and catch responses;
    private static final boolean DEEP_DEBUG = true;

    private static final String OK = "OK";
    private static final String NO = "NO";
    private static final String BAD = "BAD";
    private static final String UNTAGGED = "*";
    private static final String SP = " ";

    private StringTokenizer commandLine;
    private boolean useUIDs;
    private ACLMailbox currentMailbox;
    private String commandRaw;
    private PrintWriter out;
    private OutputStream outs;
    private String tag;
    private String user;
    private SingleThreadedConnectionHandler caller;
    private String currentFolder;
    private ImapSession session;
    public final String commandName = "COPY";
    
    public boolean validForState( ImapSessionState state ) {
        return ( state == ImapSessionState.SELECTED );
    }


    public boolean process( ImapRequest request, ImapSession session ) {
        setRequest( request );
        this.session = session;
        if ( request.arguments() < 2 ) {
            session.badResponse( "Command '"+request.getCommandLine().nextToken()+"' should be <tag> <COPY> <message set> <destination mailbox>" );
            return true;
        }
        service();
        return true;
    }

    /**
     * Debugging method - will probably disappear
     */
    public void setRequest(ImapRequest request) {
        commandLine = request.getCommandLine();
        useUIDs = request.useUIDs();
        currentMailbox = request.getCurrentMailbox();
        commandRaw = request.getCommandRaw();
        tag = request.getTag();
        currentFolder = request.getCurrentFolder();

        caller = request.getCaller();
        out = caller.getPrintWriter();
        outs = caller.getOutputStream();
        user = caller.getUser();
    }

    /**
     * Implements IMAP UID COPY commands given an ImapRequestImpl.
     */
    public void service() {
        List set;
        if (useUIDs) {
            set = decodeUIDSet(commandLine.nextToken(),
                               currentMailbox.listUIDs(user));
        } else {
            set = decodeSet(commandLine.nextToken(),
                            currentMailbox.getExists());
        }
        StringBuffer buf = new StringBuffer();
        String foldername = commandLine.nextToken();
        
        while (commandLine.hasMoreTokens()) {
            buf.append(" "+commandLine.nextToken());
        }
        foldername += buf.toString();
        
        foldername = foldername.replace('"',' ').trim();
        System.out.println("FOLDERNAME FOR COPIING: " + foldername);
        try {
            ACLMailbox targetMailbox = getMailbox( session, foldername, this.commandName );
            if ( targetMailbox == null ) {
                return;
            }
            if ( !targetMailbox.hasInsertRights( session.getCurrentUser() ) ) {
                session.noResponse( this.commandName, "Insert access not granted." );
                return;
            }
            for (int i = 0; i < set.size(); i++) {
                if (useUIDs) {
                    Integer uidObject = (Integer)set.get(i);
                    int uid = uidObject.intValue();
                    MimeMessage message = (MimeMessage)
                        session.getCurrentMailbox().retrieveUID(uid, session.getCurrentUser() );
                    /*MessageAttributes mattr = 
                        session.getCurrentMailbox().getMessageAttributesUID(uid, session.getCurrentUser() );
                    Flags flags = new Flags();
                    flags.setFlags(session.getCurrentMailbox().getFlagsUID(uid, session.getCurrentUser()),
                                    session.getCurrentUser());
                     */
                     targetMailbox.store(message, session.getCurrentUser());
                } else {
                    int msn = ((Integer)set.get( 0 ) ).intValue();
                    MimeMessage message = (MimeMessage)
                        session.getCurrentMailbox().retrieve(msn, session.getCurrentUser() );
                    /*MessageAttributes mattr = 
                        session.getCurrentMailbox().getMessageAttributes(msn, session.getCurrentUser() );
                    Flags flags = new Flags();
                    flags.setFlags(session.getCurrentMailbox().getFlags(msn, session.getCurrentUser()),
                                    session.getCurrentUser());
                     */
                     targetMailbox.store(message, session.getCurrentUser());
                }
            }

            caller.checkSize();
            out.println(tag + SP + OK + SP + "COPY completed");
        } catch (AccessControlException ace) {
            out.println(tag + SP + NO + SP + "No such mailbox");
            caller.logACE(ace);
            return;
        } catch (AuthorizationException aze) {
            out.println(tag + SP + NO + SP
                        + "You do not have the rights to store those flags");
            caller.logAZE(aze);
            return;
        } catch (IllegalArgumentException iae) {
            out.println(tag + SP + BAD + SP
                        + "Arguments to store not recognised.");
            getLogger().error("Unrecognised arguments for STORE by user "  + user
                         + " with " + commandRaw);
            return;
        }
        return;
    }
}
