/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.*;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;
import org.apache.james.AccessControlException;
import org.apache.james.AuthorizationException;
import org.apache.james.core.EnhancedMimeMessage;

/**
 * Implements the IMAP FETCH command for a given ImapRequest.
 *
 * References: rfc 2060, rfc 2193, rfc 2221
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1 on 17 Jan 2001
 */
public class CommandStore 
    extends BaseCommand {
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
     * Implements IMAP store commands given an ImapRequest. 
     * <p>Warning - maybecome service(ImapRequest request)
     */
    public void service() {
        List set;
        List uidsList = null;
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
                            int msn = uidsList.indexOf(uidObject) + 1;
                            out.println(UNTAGGED + SP + msn + SP
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
