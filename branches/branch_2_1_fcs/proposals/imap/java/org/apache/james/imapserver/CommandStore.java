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

package org.apache.james.imapserver;

import org.apache.james.imapserver.AccessControlException;
import org.apache.james.imapserver.AuthorizationException;
import org.apache.james.imapserver.commands.ImapCommand;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.StringTokenizer;
//import org.apache.james.core.EnhancedMimeMessage;

/**
 * Implements the IMAP STORE command for a given ImapRequestImpl.
 *
 * References: rfc 2060, rfc 2193, rfc 2221
 * @version 0.2 on 04 Aug 2002
 */
public class CommandStore
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
    
    public boolean validForState( ImapSessionState state )
    {
        return ( state == ImapSessionState.SELECTED );
    }


    public boolean process( ImapRequest request, ImapSession session )
    {
        setRequest( request );
        if ( request.arguments() < 3 ) {
            session.badResponse( "Command '"+request.getCommandLine().nextToken()+"' should be <tag> <STORE> <message set> <message data item names> <value for message data item>" );
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
     * Implements IMAP store commands given an ImapRequestImpl.
     * <p>Warning - maybecome service(ImapRequestImpl request)
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
        while (commandLine.hasMoreTokens()) {
            buf.append(commandLine.nextToken());
        }
        String request = buf.toString();
        try {
            for (int i = 0; i < set.size(); i++) {
                if (useUIDs) {
                    Integer uidObject = (Integer)set.get(i);
                    int uid = uidObject.intValue();
                    if (currentMailbox.setFlagsUID(uid, user, request)) {
                        if (request.toUpperCase().indexOf("SILENT") == -1) {
                            String newflags
                                = currentMailbox.getFlagsUID(uid, user);
                            out.println(UNTAGGED + SP + SP
                                        + "FETCH (FLAGS " + newflags
                                        + " UID " + uid + ")");
                        } else {
                                //silent
                        }
                    } else {
                        //failed
                        out.println(tag + SP + NO + SP
                                    + "Unable to store flags for message: "
                                    + uid);
                    }
                } else {
                    int msn = ((Integer)set.get(i)).intValue();
                    if (currentMailbox.setFlags(msn, user, request)) {
                        if (request.toUpperCase().indexOf("SILENT") == -1) {
                            String newflags
                                = currentMailbox.getFlags(msn, user);
                            out.println(UNTAGGED + SP + msn + SP
                                        + "FETCH (FLAGS " + newflags + ")");
                        } else {
                                //silent
                        }
                    } else {
                        //failed
                        out.println(tag + SP + NO + SP
                                    + "Unable to store flags for message: "
                                    + msn);
                    }
                }
            }
            caller.checkSize();
            out.println(tag + SP + OK + SP + "STORE completed");
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
