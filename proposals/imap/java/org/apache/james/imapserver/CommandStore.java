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
 * @author <a href="mailto:sascha@kulawik.de">Sascha Kulawik</a>
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
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
