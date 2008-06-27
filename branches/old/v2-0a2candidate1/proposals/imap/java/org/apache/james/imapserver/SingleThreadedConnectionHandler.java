/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import javax.mail.internet.*;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.cornerstone.services.scheduler.PeriodicTimeTrigger;
import org.apache.avalon.cornerstone.services.scheduler.Target;
import org.apache.avalon.cornerstone.services.scheduler.TimeScheduler;
import org.apache.james.AccessControlException;
import org.apache.james.AuthenticationException;
import org.apache.james.AuthorizationException;
import org.apache.james.Constants;
import org.apache.james.services.*;
import org.apache.james.util.InternetPrintWriter;
import org.apache.log.Logger;

/**
 * An IMAP Handler handles one IMAP connection. TBC - it may spawn worker
 * threads someday.
 *
 * <p> Based on SMTPHandler and POP3Handler by Federico Barbieri <scoobie@systemy.it>
 *
 * @author  <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1 on 14 Dec 2000
 */
public class SingleThreadedConnectionHandler
    extends BaseCommand
    implements ConnectionHandler, Composable, Configurable,
               Initializable, Disposable, Target, MailboxEventListener {

    //mainly to switch on stack traces and catch responses;
    private static final boolean DEEP_DEBUG = true;

    // Connection states
    private static final int NON_AUTHENTICATED = 0;
    private static final int AUTHENTICATED = 1;
    private static final int SELECTED = 2;
    private static final int LOGOUT = 3;

    // Connection termination options
    private static final int NORMAL_CLOSE = 0;
    private static final int OK_BYE = 1;
    private static final int UNTAGGED_BYE = 2;
    private static final int TAGGED_NO = 3;
    private static final int NO_BYE = 4;

    // Basic response types
    private static final String OK = "OK";
    private static final String NO = "NO";
    private static final String BAD = "BAD";
    private static final String UNTAGGED = "*";

    private static final String SP = " ";
    private static final String VERSION = "IMAP4rev1";
    private static final String CAPABILITY_RESPONSE = "CAPABILITY " + VERSION
        + " NAMESPACE" + " ACL"; //add as implemented

    private static final String LIST_WILD = "*";
    private static final String LIST_WILD_FLAT = "%";
    private static final char[] CTL = {};
    private static final String[] ATOM_SPECIALS
        = {"(", ")", "{", " ", LIST_WILD, LIST_WILD_FLAT,};

    private static final String AUTH_FAIL_MSG
        = "NO Command not authorized on this mailbox";
    private static final String BAD_LISTRIGHTS_MSG
        = "BAD Command should be <tag> <LISTRIGHTS> <mailbox> <identifier>";
    private static final String BAD_MYRIGHTS_MSG
        = "BAD Command should be <tag> <MYRIGHTS> <mailbox>";
    private static final String BAD_LIST_MSG
        = "BAD Command should be <tag> <LIST> <reference name> <mailbox>";
    private static final String BAD_LSUB_MSG
        = "BAD Command should be <tag> <LSUB> <reference name> <mailbox>";
    private static final String NO_NOTLOCAL_MSG
        = "NO Mailbox does not exist on this server";

    private Logger securityLogger;
    private MailServer mailServer;
    private UsersRepository users;
    private TimeScheduler scheduler;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private OutputStream outs;
    private String remoteHost;
    private String remoteIP;
    private String softwaretype  = "JAMES IMAP4rev1 Server " + Constants.SOFTWARE_VERSION;
    private int state;
    private String user;

    private IMAPSystem imapSystem;
    private Host imapHost;
    private String namespaceToken;
    private String currentNamespace = null;
    private String currentSeperator = null;
    private String commandRaw;

    //currentFolder holds the client-dependent absolute address of the current
    //folder, that is current Namespace and full mailbox hierarchy.
    private String currentFolder = null;
    private ACLMailbox currentMailbox = null;
    private boolean currentIsReadOnly = false;
    private boolean connectionClosed = false;
    private String tag;
    private boolean checkMailboxFlag = false;
    private int exists;
    private int recent;
    private List sequence;

    public void compose( final ComponentManager componentManager )
        throws ComponentException {

        mailServer = (MailServer)componentManager.
            lookup("org.apache.james.services.MailServer");
        users = (UsersRepository)componentManager.
            lookup("org.apache.james.services.UsersRepository");
        scheduler = (TimeScheduler)componentManager.
            lookup("org.apache.avalon.cornerstone.services.scheduler.TimeScheduler");
        imapSystem = (IMAPSystem)componentManager.
            lookup("org.apache.james.imapserver.IMAPSystem");
        imapHost = (Host)componentManager.
            lookup("org.apache.james.imapserver.Host");
    }

    public void initialize() throws Exception {
        getLogger().info("SingleThreadedConnectionHandler starting ...");
        namespaceToken = imapSystem.getNamespaceToken();
	securityLogger = getLogger().getChildLogger("security");
        getLogger().info("SingleThreadedConnectionHandler initialized");
    }

    /**
     * Handle a connection.
     * This handler is responsible for processing connections as they occur.
     *
     * @param connection the connection
     * @exception IOException if an error reading from socket occurs
     * @exception ProtocolException if an error handling connection occurs
     */
    public void handleConnection( final Socket connection )
        throws IOException {

        try {
            this.socket = connection;
            in = new BufferedReader(new
                InputStreamReader(socket.getInputStream()));
            outs = socket.getOutputStream();
            out = new InternetPrintWriter(outs, true);
            remoteHost = socket.getInetAddress ().getHostName ();
            remoteIP = socket.getInetAddress ().getHostAddress ();
        } catch (Exception e) {
            getLogger().error("Cannot open connection from " + remoteHost + " ("
                              + remoteIP + "): " + e.getMessage());
        }
        getLogger().info("Connection from " + remoteHost + " (" + remoteIP + ")");

        try {
            final PeriodicTimeTrigger trigger = new PeriodicTimeTrigger( timeout, -1 );
            scheduler.addTrigger( this.toString(), trigger, this );

            if (false) { // arbitrary rejection of connection
                // could screen connections by IP or host or implement
                // connection pool management
                connectionClosed = closeConnection(UNTAGGED_BYE,
                                                   " connection rejected.",
                                                   "");
            } else {
                if (false) { // connection is pre-authenticated
                    out.println(UNTAGGED + SP + "PREAUTH" + SP + VERSION + SP
                                + "server" + SP + this.helloName + SP
                                + "logged in as" + SP + user);
                    state = AUTHENTICATED;
                    user = "preauth user";
                    securityLogger.info("Pre-authenticated connection from  "
                                        + remoteHost + "(" + remoteIP
                                        + ") received by SingleThreadedConnectionHandler");
                } else {
                    out.println(UNTAGGED + SP + OK + SP + VERSION + SP
                                + "server " + this.helloName + SP + "ready.");
                    state = NON_AUTHENTICATED;
                    user = "unknown";
                    securityLogger.info("Non-authenticated connection from  "
                                        + remoteHost + "(" + remoteIP
                                        + ") received by SingleThreadedConnectionHandler");
                }
                while (parseCommand(in.readLine())) {
                    scheduler.resetTrigger(this.toString());
                }
            }

            if (!connectionClosed) {
                connectionClosed
                    = closeConnection(UNTAGGED_BYE,
                                      "Server error, closing connection", "");
            }

        } catch (Exception e) {
            // This should never happen once code is debugged
            getLogger().error("Exception during connection from " + remoteHost
                              + " (" + remoteIP + ") : " + e.getMessage());
            e.printStackTrace();
            connectionClosed = closeConnection(UNTAGGED_BYE,
                                               "Error processing command.", "");
        }

        scheduler.removeTrigger(this.toString());
    }

    public void targetTriggered( final String triggerName ) {
        getLogger().info("Connection timeout on socket");
        connectionClosed = closeConnection(UNTAGGED_BYE,
                                           "Autologout. Idle too long.", "");
    }

    private boolean closeConnection( int exitStatus,
                                     String message1,
                                     String message2 ) {
        scheduler.removeTrigger(this.toString());
        if (state == SELECTED) {
            currentMailbox.removeMailboxEventListener(this);
            imapHost.releaseMailbox(user, currentMailbox);
        }

        try {
            switch(exitStatus) {
            case 0 :
                out.println(UNTAGGED + SP + "BYE" + SP + "server logging out");
                out.println(tag + SP + OK + SP + "LOGOUT completed");
                break;
            case 1 :
                out.println(UNTAGGED + SP + "BYE" + SP + message1);
                out.println(tag + SP + OK + SP + message2);
                break;
            case 2:
                out.println(UNTAGGED + SP + "BYE" + SP + message1);
                break;
            case 3 :
                out.println(tag + SP + NO + SP + message1);
                break;
            case 4 :
                out.println(UNTAGGED + SP + "BYE" + SP + message1);
                out.println(tag + SP + NO + SP + message2);
                break;
            }
            out.flush();
            socket.close();
            getLogger().info("Connection closed" + SP + exitStatus + SP + message1
                             +  SP + message2);
        } catch (IOException ioe) {
            getLogger().error("Exception while closing connection from " + remoteHost
                              + " (" + remoteIP + ") : " + ioe.getMessage());
            try {
                socket.close();
            } catch (IOException ioe2) {
            }
        }
        return true;
    }

    private boolean parseCommand(String next) {
        commandRaw = next;
        String folder = null;
        String command = null;
        boolean subscribeOnly = false;

        if (commandRaw == null) return false;
        //        getLogger().debug("Command recieved: " + commandRaw + " from " + remoteHost
        //           + "(" + remoteIP  + ")");
        //String command = commandRaw.trim();
        StringTokenizer commandLine = new StringTokenizer(commandRaw.trim(), " ");
        int arguments = commandLine.countTokens();
        if (arguments == 0) {
            return true;
        } else {
            String test = commandLine.nextToken();
            if (test.length() < 10) {// this stops overlong junk.
                // we should validate the tag contents
                tag = test;
            } else {
                out.println(UNTAGGED + SP + BAD + SP + "tag too long");
                return true;
            }
        }
        if (arguments > 1) {
            String test = commandLine.nextToken();
            if (test.length() < 13) {// this stops overlong junk.
                // we could validate the command contents,
                // but may not be worth it
                command = test;
            }
            else {
                out.println(tag + SP + BAD + SP
                            + "overlong command attempted");
                return true;
            }
        } else {
            out.println(UNTAGGED + SP + BAD + SP + "no command sent");
            return true;
        }

        // At this stage we have a tag and a string which may be a command
        // Start with commands that are valid in any state
        // CAPABILITY, NOOP, LOGOUT

        if (command.equalsIgnoreCase("CAPABILITY")) {
            out.println(UNTAGGED + SP + CAPABILITY_RESPONSE);
            if (state == SELECTED ) {
                checkSize();
                checkExpunge();
            }
            out.println(tag + SP + OK + SP + "CAPABILITY completed");
            getLogger().debug("Capability command completed for " + remoteHost
                              + "(" + remoteIP  + ")");
            return true;

        } else if (command.equalsIgnoreCase("NOOP")) {
            if (state == SELECTED ) {
                checkSize();
                checkExpunge();
            }
            // we could send optional untagged status responses as well
            out.println(tag + SP + OK + SP + "NOOP completed");
            getLogger().debug("Noop command completed for " + remoteHost
                              + "(" + remoteIP  + ")");
            return true;

        } else if (command.equalsIgnoreCase("LOGOUT")) {
            connectionClosed = closeConnection(NORMAL_CLOSE, "", "");
            return false;

        }

        // Commands only valid in NON_AUTHENTICATED state
        // AUTHENTICATE, LOGIN

        if (state == NON_AUTHENTICATED) {
            if (command.equalsIgnoreCase("AUTHENTICATE")) {
                out.println(tag + SP + NO + SP + "Auth type not supported.");
                getLogger().info("Attempt to use Authenticate command by "
                                 + remoteHost  + "(" + remoteIP  + ")");
                securityLogger.info("Attempt to use Authenticate command by "
                                    + remoteHost  + "(" + remoteIP  + ")");
                return true;
            } else if (command.equalsIgnoreCase("LOGIN")) {
                if (arguments != 4) {
                    out.println(tag + SP + BAD + SP
                                + "Command should be <tag> <LOGIN> <username> <password>");
                    getLogger().info("Wrong number of arguments for LOGIN command from "
                                     + remoteHost  + "(" + remoteIP  + ")");
                    return true;
                }
                user = decodeAstring(commandLine.nextToken());
                String password = decodeAstring(commandLine.nextToken());
                if (users.test(user, password)) {
                    securityLogger.info("Login successful for " + user + " from  "
                                        + remoteHost + "(" + remoteIP  + ")");
                    // four possibilites handled:
                    // private mail: isLocal, is Remote
                    // other mail (shared, news, etc.) is Local, is Remote

                    if (imapHost.isHomeServer(user)) {
                        out.println(tag + SP + OK + SP + "LOGIN completed");
                        state = AUTHENTICATED;

                    } else  {
                        String remoteServer = null;
                        try {
                            remoteServer
                                = imapSystem.getHomeServer(user);
                        } catch (AuthenticationException ae) {
                            connectionClosed
                                = closeConnection(TAGGED_NO,
                                                  " cannot find your inbox, closing connection",
                                                  "");
                            return false;
                        }

                        if (imapHost.hasLocalAccess(user)) {
                            out.println(tag + SP + OK + SP + "[REFERRAL "
                                        + remoteServer +"]" + SP
                                        + "Your home server is remote, other mailboxes available here");
                            state = AUTHENTICATED;

                        } else {
                            closeConnection(TAGGED_NO, " [REFERRAL" + SP
                                            + remoteServer +"]" + SP
                                            + "No mailboxes available here, try remote server", "");
                            return false;
                        }
                    }
                    currentNamespace = imapHost.getDefaultNamespace(user);
                    currentSeperator
                        = imapSystem.getHierarchySeperator(currentNamespace);
                    // position at root of default Namespace,
                    // which is not actually a folder
                    currentFolder = currentNamespace + currentSeperator + "";
                    getLogger().debug("Current folder for user "  + user + " from "
                                      + remoteHost  + "(" + remoteIP  + ") is "
                                      + currentFolder);
                    return true;


                } // failed password test
                // We should add ability to monitor attempts to login
                out.println(tag + SP + NO + SP + "LOGIN failed");
                securityLogger.error("Failed attempt to use Login command for account "
                                     + user + " from "  + remoteHost  + "(" + remoteIP
                                     + ")");
                return true;
            }
            // bad client
            out.println(tag + SP + NO + SP + "Must authenticate first");
            return true;
        } // end of if (state == NON_AUTHENTICATED)

        // Commands not yet processed should be valid in either
        // Authenticated or Selected states.
        getLogger().debug("Command recieved: " + commandRaw + " from " + remoteHost
                          + "(" + remoteIP  + ")");

        // Create ImapRequest object here - is this the right stage?
        ImapRequest request = new ImapRequest(this);
        request.setCommandLine(commandLine);
        request.setUseUIDs(false);
        request.setCurrentMailbox(currentMailbox);
        request.setCommandRaw(commandRaw);
        request.setTag(tag);
        request.setCurrentFolder(currentFolder);

        // Commands valid in both Authenticated and Selected states
        // NAMESPACE, GETACL, SETACL, DELETEACL, LISTRIGHTS, MYRIGHTS, SELECT
        if (state == AUTHENTICATED || state == SELECTED) {

            // NAMESPACE capability ------------------------------
            if (command.equalsIgnoreCase("NAMESPACE")) {
                String namespaces = imapSystem.getNamespaces(user);
                out.println(UNTAGGED + SP + "NAMESPACE " + namespaces);
                getLogger().info("Provided NAMESPACE: " + namespaces );
                if (state == SELECTED ) {
                    checkSize();
                    checkExpunge();
                }
                out.println(tag + SP + OK + SP
                            + "NAMESPACE command completed");
                return true;

                // ACL Capability  ---------------------------------------
            } else if (command.equalsIgnoreCase("GETACL")) {
                ACLMailbox target = null;
                if (arguments != 3) {
                    out.println(tag + SP + BAD + SP +
                                "Command should be <tag> <GETACL> <mailbox>");
                    return true;
                }
                folder =  getFullName(commandLine.nextToken());
                if ( state == SELECTED && currentFolder.equals(folder) ) {
                    target = currentMailbox;
                } else {
                    target = getBox(user, folder);
                    if (target == null) return true;
                }
                try {
                    out.println(UNTAGGED + SP + "ACL " + target.getName() + SP
                                + target.getAllRights(user ));
                    getLogger().debug(UNTAGGED + SP + "ACL " + target.getName() + SP
                                      + target.getAllRights(user ));
                } catch (AccessControlException ace) {
                    out.println(tag + SP + NO + SP + "Unknown mailbox");
                    logACE(ace);
                    return true;
                } catch (AuthorizationException aze) {
                    out.println(tag + SP + AUTH_FAIL_MSG);
                    logAZE(aze);
                    return true;
                }
                if (state == SELECTED ) {
                    checkSize();
                    checkExpunge();
                }
                out.println(tag + SP + OK + SP
                            + "GetACL command completed");
                return true;

            } else if (command.equalsIgnoreCase("SETACL")) {
                ACLMailbox target = null;
                if (arguments != 5) {
                    out.println(tag + SP + BAD + SP +
                                "Command should be <tag> <SETACL> <mailbox> <identity> <rights modification>");
                    return true;
                }
                folder =  getFullName(commandLine.nextToken());
                String identity = commandLine.nextToken();
                String changes = commandLine.nextToken();

                if ( (state == SELECTED && currentFolder.equals(folder))) {
                    target = currentMailbox;
                } else {
                    target = getBox(user, folder);
                    if (target == null) return true;
                }

                try {
                    if (target.setRights(user, identity, changes)) {
                        out.println(tag + SP + OK + SP
                                    + "SetACL command completed");
                        securityLogger.info("ACL rights for "  + identity + " in "
                                            + folder  + " changed by " + user + " : "
                                            +  changes);
                    } else {
                        out.println(tag + SP + NO + SP
                                    + "SetACL command failed");
                        securityLogger.info("Failed attempt to change ACL rights for "
                                            + identity + " in " + folder  + " by "
                                            + user);
                    }
                } catch (AccessControlException ace) {
                    out.println(tag + SP + NO + SP + "Unknown mailbox");
                    logACE(ace);
                    return true;
                } catch (AuthorizationException aze) {
                    out.println(tag + SP + AUTH_FAIL_MSG);
                    logAZE(aze);
                    return true;
                }
                if (state == SELECTED ) {
                    checkSize();
                    checkExpunge();
                }
                return true;


            } else if (command.equalsIgnoreCase("DELETEACL")) {
                ACLMailbox target = null;
                if (arguments != 4) {
                    out.println(tag + SP + BAD + SP +
                                "Command should be <tag> <DELETEACL> <mailbox> <identity>");
                    return true;
                }
                folder =  getFullName(commandLine.nextToken());
                String identity = commandLine.nextToken();
                String changes = "";

                if ( (state == SELECTED && currentFolder.equals(folder))) {
                    target = currentMailbox;
                } else {
                    target = getBox(user, folder);
                    if (target == null) return true;
                }

                try {
                    if (target.setRights(user, identity, changes)) {
                        out.println(tag + SP + OK + SP
                                    + "DeleteACL command completed");
                        securityLogger.info("ACL rights for "  + identity + " in "
                                            + folder + " deleted by " + user);
                    } else {
                        out.println(tag + SP + NO + SP
                                    + "SetACL command failed");
                        securityLogger.info("Failed attempt to change ACL rights for "
                                            + identity + " in " + folder  + " by "
                                            + user);
                    }
                } catch (AccessControlException ace) {
                    out.println(tag + SP + NO + SP + "Unknown mailbox");
                    logACE(ace);
                    return true;
                } catch (AuthorizationException aze) {
                    out.println(tag + SP + AUTH_FAIL_MSG);
                    logAZE(aze);
                    return true;
                }
                if (state == SELECTED ) {
                    checkSize();
                    checkExpunge();
                }
                return true;

            } else if (command.equalsIgnoreCase("LISTRIGHTS")) {
                ACLMailbox target = null;
                if (arguments != 4) {
                    out.println(tag + SP + BAD_LISTRIGHTS_MSG);
                    return true;
                }
                folder =  getFullName(commandLine.nextToken());
                String identity = commandLine.nextToken();
                if ( state == SELECTED && currentFolder.equals(folder) ) {
                    target = currentMailbox;
                } else {
                    target = getBox(user, folder);
                    if (target == null) return true;
                }

                try {
                    out.println(UNTAGGED + SP + "LISTRIGHTS "
                                + target.getName() + SP + identity + SP
                                + target.getRequiredRights(user, identity)
                                + SP
                                + target.getOptionalRights(user, identity));
                    out.println(tag + SP + OK + SP
                                + "ListRights command completed");
                } catch (AccessControlException ace) {
                    out.println(tag + SP + NO + SP + "Unknown mailbox");
                    logACE(ace);
                    return true;
                } catch (AuthorizationException aze) {
                    out.println(tag + SP + AUTH_FAIL_MSG);
                    logAZE(aze);
                    return true;
                }
                if (state == SELECTED ) {
                    checkSize();
                    checkExpunge();
                }
                return true;

            } else if (command.equalsIgnoreCase("MYRIGHTS")) {
                ACLMailbox target = null;
                if (arguments != 3) {
                    out.println(tag + SP + BAD_MYRIGHTS_MSG);
                    return true;
                }
                folder =  getFullName(commandLine.nextToken());
                if ( state == SELECTED && currentFolder.equals(folder) ) {
                    target = currentMailbox;
                } else {
                    target = getBox(user, folder);
                    if (target == null) return true;
                }

                try {
                    out.println(UNTAGGED + SP + "MYRIGHTS "
                                + target.getName() + SP
                                + target.getRights(user, user));
                    out.println(tag + SP + OK + SP
                                + "MYRIGHTS command completed");
                } catch (AccessControlException ace) {
                    out.println(tag + SP + NO + SP + "Unknown mailbox");
                    logACE(ace);
                    return true;
                } catch (AuthorizationException aze) {
                    out.println(tag + SP + AUTH_FAIL_MSG);
                    logAZE(aze);
                    return true;
                }
                if (state == SELECTED ) {
                    checkSize();
                    checkExpunge();
                }
                return true;


                // Standard IMAP commands --------------------------

            } else if (command.equalsIgnoreCase("SELECT")
                       || command.equalsIgnoreCase("EXAMINE")) {
                // selecting a mailbox deselects current mailbox,
                // even if this select fails
                if (state == SELECTED) {
                    currentMailbox.removeMailboxEventListener(this);
                    imapHost.releaseMailbox(user, currentMailbox);
                    state = AUTHENTICATED;
                    currentMailbox = null;
                    currentIsReadOnly = false;
                }

                if (arguments != 3) {
                    if (command.equalsIgnoreCase("SELECT") ){
                        out.println(tag + SP + BAD + SP
                                    + "Command should be <tag> <SELECT> <mailbox>");
                    } else {
                        out.println(tag + SP + BAD + SP
                                    + "Command should be <tag> <EXAMINE> <mailbox>");
                    }
                    return true;
                }

                folder =  getFullName(commandLine.nextToken());
                currentMailbox = getBox(user,  folder);
                if (currentMailbox == null) {
                    return true;
                }
                try { // long tries clause against an AccessControlException
                    if (!currentMailbox.hasReadRights(user)) {
                        out.println(tag + SP + NO + SP
                                    + "Read access not granted." );
                        return true;
                    }
                    if (command.equalsIgnoreCase("SELECT") ){
                        if (!currentMailbox.isSelectable(user)) {
                            out.println(tag + SP + NO + SP
                                        + "Mailbox exists but is not selectable");
                            return true;
                        }
                    }

                    // Have mailbox with at least read rights. Server setup.
                    currentMailbox.addMailboxEventListener(this);
                    currentFolder = folder;
                    state = SELECTED;
                    exists = -1;
                    recent = -1;
                    getLogger().debug("Current folder for user "  + user + " from "
                                      + remoteHost  + "(" + remoteIP  + ") is "
                                      + currentFolder);

                    // Inform client
                    out.println(UNTAGGED + SP + "FLAGS ("
                                + currentMailbox.getSupportedFlags() + ")" );
                    if (!currentMailbox.allFlags(user)) {
                        out.println(UNTAGGED + SP + OK + " [PERMANENTFLAGS ("
                                    + currentMailbox.getPermanentFlags(user)
                                    + ") ]");
                    }
                    checkSize();
                    out.println(UNTAGGED + SP + OK + " [UIDVALIDITY "
                                + currentMailbox.getUIDValidity() + " ]");
                    int oldestUnseen = currentMailbox.getOldestUnseen(user);
                    if (oldestUnseen > 0 ) {
                        out.println(UNTAGGED + SP + OK + " [UNSEEN "
                                    + oldestUnseen + " ]");
                    } else {
                        out.println(UNTAGGED + SP + OK + " No unseen messages");
                    }
                    sequence = currentMailbox.listUIDs(user);

                    if (command.equalsIgnoreCase("EXAMINE")) {
                        currentIsReadOnly = true;

                        out.println(tag + SP + OK + SP
                                    + "[READ-ONLY] Examine completed");
                        return true;

                    } else if (currentMailbox.isReadOnly(user)) {
                        currentIsReadOnly = true;
                        out.println(tag + SP + OK + SP
                                    + "[READ-ONLY] Select completed");
                        return true;
                    }
                    out.println(tag + SP + OK + SP
                                + "[READ-WRITE] Select completed");
                    return true;
                } catch (AccessControlException ace) {
                    out.println(tag + SP + NO + SP + "No such mailbox.");
                    logACE(ace);
                    return true;
                }
                // End of SELECT || EXAMINE
            } else if (command.equalsIgnoreCase("CREATE")) {
                if (arguments != 3) {
                    out.println(tag + SP + BAD + SP +
                                "Command should be <tag> <CREATE> <mailbox>");
                    return true;
                }
                folder =  getFullName(commandLine.nextToken());
                if(currentFolder == folder) {
                    out.println(tag + SP + NO + SP
                                + "Folder exists and is selected." );
                    return true;
                }
                try {
                    ACLMailbox target = imapHost.createMailbox(user, folder);
                    out.println(tag + SP + OK + SP + "Create completed");
                    imapHost.releaseMailbox(user, target);
                }  catch (AccessControlException ace) {
                    out.println(tag + SP + NO + SP
                                + "No such mailbox. ");
                    logACE(ace);
                    return true;
                } catch (MailboxException mbe) {
                    if (mbe.isRemote()) {
                        out.println(tag + SP + NO + SP + "[REFERRAL "
                                    + mbe.getRemoteServer() +"]"
                                    + SP + "Wrong server. Try remote." );
                    } else  {
                        out.println(tag + SP + NO + SP + mbe.getStatus() );
                    }
                    return true;
                } catch (AuthorizationException aze) {
                    out.println(tag + SP + NO + SP
                                + "You do not have the rights to create mailbox: "
                                + folder);
                    return true;
                }
                if (state == SELECTED ) {
                    checkSize();
                    checkExpunge();
                }
                return true;


            } else if (command.equalsIgnoreCase("DELETE")) {
                if (arguments != 3) {
                    out.println(tag + SP + BAD + SP +
                                "Command should be <tag> <DELETE> <mailbox>");
                    return true;
                }
                folder =  getFullName(commandLine.nextToken());
                if(currentFolder == folder) {
                    out.println(tag + SP + NO + SP
                                + "You can't delete a folder while you have it selected." );
                    return true;
                }
                try {
                    if( imapHost.deleteMailbox(user, folder)) {
                        out.println(tag + SP + OK + SP + "Delete completed");
                    } else {
                        out.println(tag + SP + NO + SP
                                    + "Delete failed, unknown error");
                        getLogger().info("Attempt to delete mailbox " + folder
                                         + " by user " + user + " failed.");
                    }
                } catch (MailboxException mbe) {
                    if (mbe.getStatus().equals(MailboxException.NOT_LOCAL)) {
                        out.println(tag + SP + NO_NOTLOCAL_MSG);
                    } else  {
                        out.println(tag + SP + NO + SP + mbe.getMessage() );
                    }
                    return true;
                } catch (AuthorizationException aze) {
                    out.println(tag + SP + NO + SP
                                + "You do not have the rights to delete mailbox: " + folder);
                    logAZE(aze);
                    return true;
                }
                if (state == SELECTED ) {
                    checkSize();
                    checkExpunge();
                }
                return true;


            } else if (command.equalsIgnoreCase("RENAME")) {
                if (arguments != 4) {
                    out.println(tag + SP + BAD + SP +
                                "Command should be <tag> <RENAME> <oldname> <newname>");
                    return true;
                }
                folder =  getFullName(commandLine.nextToken());
                String newName = getFullName(commandLine.nextToken());
                if(currentFolder == folder) {
                    out.println(tag + SP + NO + SP
                                + "You can't rename a folder while you have it selected." );
                    return true;
                }
                try {
                    if(imapHost.renameMailbox(user, folder, newName)) {
                        out.println(tag + SP + OK + SP + "Rename completed");
                    } else {
                        out.println(tag + SP + NO + SP
                                    + "Rename failed, unknown error");
                        getLogger().info("Attempt to rename mailbox " + folder
                                         + " to " + newName
                                         + " by user " + user + " failed.");
                    }
                } catch (MailboxException mbe) {
                    if (mbe.getStatus().equals(MailboxException.NOT_LOCAL)) {
                        out.println(tag + SP + NO_NOTLOCAL_MSG);
                    } else  {
                        out.println(tag + SP + NO + SP + mbe.getMessage() );
                    }
                    return true;
                } catch (AuthorizationException aze) {
                    out.println(tag + SP + NO + SP
                                + "You do not have the rights to delete mailbox: " + folder);
                    return true;
                }
                if (state == SELECTED ) {
                    checkSize();
                    checkExpunge();
                }
                return true;


            } else if (command.equalsIgnoreCase("SUBSCRIBE")) {
                if (arguments != 3) {
                    out.println(tag + SP + BAD + SP +
                                "Command should be <tag> <SUBSCRIBE> <mailbox>");
                    return true;
                }
                folder =  getFullName(commandLine.nextToken());

                try {
                    if( imapHost.subscribe(user, folder) ) {
                        out.println(tag + SP + OK + SP
                                    + "Subscribe completed");
                    } else {
                        out.println(tag + SP + NO + SP + "Unknown error." );
                    }
                } catch (MailboxException mbe) {
                    if (mbe.isRemote()) {
                        out.println(tag + SP + NO + SP + "[REFERRAL "
                                    + mbe.getRemoteServer() +"]"
                                    + SP + "Wrong server. Try remote." );
                    } else  {
                        out.println(tag + SP + NO + SP + "No such mailbox" );
                    }
                    return true;
                } catch (AccessControlException ace) {
                    out.println(tag + SP + NO + SP + "No such mailbox");
                    logACE(ace);
                    return true;
                }
                if (state == SELECTED ) {
                    checkSize();
                    checkExpunge();
                }
                return true;

            } else if (command.equalsIgnoreCase("UNSUBSCRIBE")) {
                if (arguments != 3) {
                    out.println(tag + SP + BAD + SP +
                                "Command should be <tag> <UNSUBSCRIBE> <mailbox>");
                    return true;
                }
                folder =  getFullName(commandLine.nextToken());

                try {
                    if( imapHost.unsubscribe(user, folder) ) {
                        out.println(tag + SP + OK + SP
                                    + "Unsubscribe completed");
                    } else {
                        out.println(tag + SP + NO + SP + "Unknown error." );
                    }
                } catch (MailboxException mbe) {
                    if (mbe.isRemote()) {
                        out.println(tag + SP + NO + SP + "[REFERRAL "
                                    + mbe.getRemoteServer() +"]"
                                    + SP + "Wrong server. Try remote." );
                    } else  {
                        out.println(tag + SP + NO + SP + "No such mailbox" );
                    }
                    return true;
                } catch (AccessControlException ace) {
                    out.println(tag + SP + NO + SP + "No such mailbox");
                    logACE(ace);
                    return true;
                }
                if (state == SELECTED ) {
                    checkSize();
                    checkExpunge();
                }
                return true;

            } else if (command.equalsIgnoreCase("LIST")
                       || command.equalsIgnoreCase("LSUB")) {
                if (arguments != 4) {
                    if (command.equalsIgnoreCase("LIST")) {
                        out.println(tag + SP + BAD_LIST_MSG);
                    } else {
                        out.println(tag + SP + BAD_LSUB_MSG);
                    }
                    return true;
                }
                if (command.equalsIgnoreCase("LIST")) {
                    subscribeOnly =false;
                } else {
                    subscribeOnly = true;
                }
                String reference = decodeAstring(commandLine.nextToken());
                folder = decodeAstring(commandLine.nextToken());

                if (reference.equals("")) {
                    reference = currentFolder;
                } else {
                    reference = getFullName(reference);
                }
                Collection list = null;
                try {
                    list = imapHost.listMailboxes(user, reference, folder,
                                                  subscribeOnly);
                    if (list == null) {
                        getLogger().debug(tag + SP + NO + SP + command
                                          + " unable to interpret mailbox");
                        out.println(tag + SP + NO + SP + command
                                    + " unable to interpret mailbox");
                    } else if (list.size() == 0) {
                        getLogger().debug("List request matches zero mailboxes: " + commandRaw);
                        out.println(tag + SP + OK + SP + command
                                    + " completed");
                    } else {
                        Iterator it = list.iterator();
                        while (it.hasNext()) {
                            String listResponse = (String)it.next();
                            out.println(UNTAGGED + SP + command.toUpperCase()
                                        + SP + listResponse);
                            getLogger().debug(UNTAGGED + SP + command.toUpperCase()
                                              + SP + listResponse);
                        }
                        out.println(tag + SP + OK + SP + command
                                    + " completed");
                    }
                } catch (MailboxException mbe) {
                    if (mbe.isRemote()) {
                        out.println(tag + SP + NO + SP + "[REFERRAL "
                                    + mbe.getRemoteServer() +"]"
                                    + SP + "Wrong server. Try remote." );
                    } else  {
                        out.println(tag + SP + NO + SP
                                    + "No such mailbox" );
                    }
                    return true;
                } catch (AccessControlException ace) {
                    out.println(tag + SP + NO + SP + "No such mailbox");
                    logACE(ace);
                    return true;
                }

                if (state == SELECTED ) {
                    checkSize();
                    checkExpunge();
                }
                return true;

            } else if (command.equalsIgnoreCase("STATUS")) {
                if (arguments < 4) {
                    out.println(tag + SP + BAD + SP +
                                "Command should be <tag> <STATUS> <mailboxname> (status data items)");
                    return true;
                }
                folder =  getFullName(commandLine.nextToken());
                List dataNames = new ArrayList();
                String  attr = commandLine.nextToken();
                if (! attr.startsWith("(")) { //single attr
                    out.println(tag + SP + BAD + SP +
                                "Command should be <tag> <STATUS> <mailboxname> (status data items)");
                    return true;
                } else if (attr.endsWith(")")){ //single attr in paranthesis
                    dataNames.add(attr.substring(1, attr.length()-1 ));
                } else { // multiple attrs
                    dataNames.add(attr.substring(1).trim());
                    while(commandLine.hasMoreTokens()) {
                        attr = commandLine.nextToken();
                        if (attr.endsWith(")")) {
                            dataNames.add(attr.substring(0, attr.length()-1 ));
                        } else {
                            dataNames.add(attr);
                        }
                    }
                }
                try {
                    String response = imapHost.getMailboxStatus(user, folder,
                                                                dataNames);
                    out.println(UNTAGGED + " STATUS " + folder + " ("
                                + response + ")");
                    out.println(tag + SP + OK + SP + "Status completed");
                } catch (MailboxException mbe) {
                    if (mbe.isRemote()) {
                        out.println(tag + SP + NO + SP + "[REFERRAL "
                                    + mbe.getRemoteServer() +"]"
                                    + SP + "Wrong server. Try remote." );
                    } else  {
                        out.println(tag + SP + NO + SP
                                    + "No such mailbox" );
                    }
                    return true;
                } catch (AccessControlException ace) {
                    out.println(tag + SP + NO + SP + "No such mailbox");
                    logACE(ace);
                    return true;
                }
                if (state == SELECTED ) {
                    checkSize();
                    checkExpunge();
                }
                return true;

            } else if (command.equalsIgnoreCase("APPEND")) {
                if (true) {
                    out.println(tag + SP + BAD + SP +
                                "Append Command not yet implemented.");
                    return true;
                }

            }

        } // end of Auth & Selected

        // Commands valid only in Authenticated State
        // None
        if (state == AUTHENTICATED) {
            out.println(tag + SP + BAD + SP
                        + "Command not valid in this state");
            return true;
        }

        // Commands valid only in Selected state
        // CHECK
        if (state == SELECTED) {
            if (command.equalsIgnoreCase("CHECK")) {
                if (currentMailbox.checkpoint()) {
                    out.println(tag + SP + OK + SP
                                + "Check completed");
                    checkSize();
                    checkExpunge();
                    return true;
                } else {
                    out.println(tag + SP + NO + SP
                                + "Check failed");
                    return true;
                }
            } else if (command.equalsIgnoreCase("CLOSE")) {
                try {
                    currentMailbox.expunge(user);
                } catch (Exception e) {
                    getLogger().error("Exception while expunging mailbox on CLOSE : " + e);
                }
                currentMailbox.removeMailboxEventListener(this);
                imapHost.releaseMailbox(user, currentMailbox);
                state = AUTHENTICATED;
                currentMailbox = null;
                currentIsReadOnly = false;
                out.println(tag + SP + OK + SP
                            + "CLOSE completed");
                return true;
            } else if (command.equalsIgnoreCase("COPY")) {
                if (arguments < 4) {
                    out.println(tag + SP + BAD + SP +
                                "Command should be <tag> <COPY> <message set> <mailbox name>");
                    return true;
                }
                List set = decodeSet(commandLine.nextToken(),
                                     currentMailbox.getExists());
                getLogger().debug("Fetching message set of size: " + set.size());
                String  targetFolder = getFullName(commandLine.nextToken());

                ACLMailbox targetMailbox = getBox(user,  targetFolder);
                if (targetMailbox == null) {
                    return true;
                }
                try { // long tries clause against an AccessControlException
                    if (!currentMailbox.hasInsertRights(user)) {
                        out.println(tag + SP + NO + SP
                                    + "Insert access not granted." );
                        return true;
                    }
                    for (int i = 0; i < set.size(); i++) {
                        int msn = ((Integer)set.get(i)).intValue();
                        MessageAttributes attrs = currentMailbox.getMessageAttributes(msn, user);
                    }
                } catch (AccessControlException ace) {
                    out.println(tag + SP + NO + SP + "No such mailbox.");
                    logACE(ace);
                    return true;
                } catch (AuthorizationException aze) {
                    out.println(tag + SP + NO + SP
                                + "You do not have the rights to expunge mailbox: " + folder);
                    logAZE(aze);
                    return true;
                }

                out.println(tag + SP + OK + SP
                            + "CLOSE completed");
                return true;
            } else if (command.equalsIgnoreCase("EXPUNGE")) {
                try {
                    if(currentMailbox.expunge(user)) {
                        checkExpunge();
                        checkSize();
                        out.println(tag + SP + OK + SP
                                    + "EXPUNGE complete.");
                    } else {
                        out.println(tag + SP + NO + SP
                                    + "Unknown server error.");
                    }
                    return true;
                } catch (AccessControlException ace) {
                    out.println(tag + SP + NO + SP + "No such mailbox");
                    logACE(ace);
                    return true;
                } catch (AuthorizationException aze) {
                    out.println(tag + SP + NO + SP
                                + "You do not have the rights to expunge mailbox: " + folder);
                    logAZE(aze);
                    return true;
                } catch (Exception e) {
                    out.println(tag + SP + NO + SP
                                + "Unknown server error.");
                    getLogger().error("Exception expunging mailbox " + folder + " by user " + user + " was : " + e);
                    if (DEEP_DEBUG) {e.printStackTrace();}
                    return true;
                }
            } else if (command.equalsIgnoreCase("FETCH")) {
                if (arguments < 4) {
                    out.println(tag + SP + BAD + SP +
                                "Command should be <tag> <FETCH> <message set> <message data item names>");
                    return true;
                }
                CommandFetch fetcher = new CommandFetch();
                fetcher.setLogger( getLogger() );
                fetcher.setRequest(request);
                fetcher.service();
                return true;
                // end of FETCH
            } else if (command.equalsIgnoreCase("STORE")) {
                if (arguments < 5) {
                    out.println(tag + SP + BAD + SP +
                                "Command should be <tag> <STORE> <message set> <message data item names> <value for message data item>");
                    return true;
                }
                //storeCommand(commandLine, false);
                CommandStore storer = new CommandStore();
                storer.setLogger( getLogger() );
                storer.setRequest(request);
                storer.service();
                return true;
            } else if (command.equalsIgnoreCase("UID")) {
                if (arguments < 4) {
                    out.println(tag + SP + BAD + SP +
                                "Command should be <tag> <UID> <command> <command parameters>");
                    return true;
                }
                String uidCommand = commandLine.nextToken();
                if (uidCommand.equalsIgnoreCase("STORE")) {
                    //storeCommand(commandLine, true);
                    CommandStore storer = new CommandStore();
                    storer.setLogger( getLogger() );
                    storer.setRequest(request);
                    storer.service();
                    return true;
                } else if (uidCommand.equalsIgnoreCase("FETCH")) {
                    CommandFetch fetcher = new CommandFetch();
                    fetcher.setLogger( getLogger() );
                    fetcher.setRequest(request);
                    fetcher.service();
                    return true;
                }
            } else {
                // Other commands for selected state .....
                out.println(tag + SP + BAD + SP + "Protocol error");
                return true;

            } // end state SELECTED
        }
        // Shouldn't happen
        out.println(tag + SP + BAD + SP + "Protocol error");
        return true;

    } // end of parseCommand

    public void dispose() {
        // todo
        getLogger().error("Stop IMAPHandler");
    }

    public void receiveEvent(MailboxEvent me) {
        if (state == SELECTED) {
            checkMailboxFlag = true;
        }
    }

    private ACLMailbox getBox(String user, String mailboxName) {
        ACLMailbox tempMailbox = null;
        try {
            tempMailbox = imapHost.getMailbox(user, mailboxName);
        } catch (MailboxException me) {
            if (me.isRemote()) {
                out.println(tag + SP + NO + SP + "[REFERRAL " + me.getRemoteServer() +"]" + SP + "Remote mailbox" );
            } else {
                out.println(tag + SP + NO + SP + "Unknown mailbox" );
                getLogger().info("MailboxException in method getBox for user: "
                                 + user + " mailboxName: " + mailboxName + " was "
                                 + me.getMessage());
            }

        } catch (AccessControlException e) {
            out.println(tag + SP + NO + SP + "Unknown mailbox" );
        }
        return tempMailbox;
    }

    private String getFullName(String name) {
        getLogger().debug("Method getFullName called for " + name);
        name = decodeAstring(name);
        if (name == null) {
            getLogger().error("Received null name");
            return null;
        }
        int inbox = name.toUpperCase().indexOf("INBOX");
        if (inbox == -1) {
            if (name.startsWith(namespaceToken)) {          //absolute reference
                return name;
            } else if (name.startsWith(currentSeperator)) {//rooted relative ref
                return currentNamespace + name;
            }else {                                        //unrooted relative ref
                if (currentFolder.equals(currentNamespace + currentSeperator )) {
                    return currentFolder + name;
                } else {
                    return currentFolder + currentSeperator + name;
                }
            }
        } else {
            return ("#mail.INBOX");

        }
    }

    public void logACE(AccessControlException ace) {
        securityLogger.error("AccessControlException by user "  + user
                             + " from "  + remoteHost  + "(" + remoteIP
                             + ") with " + commandRaw + " was "
                             + ace.getMessage());
    }

    public void logAZE(AuthorizationException aze) {
        securityLogger.error("AuthorizationException by user "  + user
                             + " from "  + remoteHost  + "(" + remoteIP
                             + ") with " + commandRaw + " was "
                             + aze.getMessage());
    }

    public PrintWriter getPrintWriter() {
        return out;
    }

    public OutputStream getOutputStream() {
        return outs;
    }

    public String getUser() {
        return user;
    }

    private String decodeAstring(String rawAstring) {

        if (rawAstring.startsWith("\"")) {
            //quoted string
            if (rawAstring.endsWith("\"")) {
                if (rawAstring.length() == 2) {
                    return new String(); //ie blank
                } else {
                    return rawAstring.substring(1, rawAstring.length() - 1);
                }
            } else {
                getLogger().error("Quoted string with no closing quote.");
                return null;
            }
        } else {
            //atom
            return rawAstring;
        }
    }

    public void checkSize() {
        int newExists = currentMailbox.getExists();
        if (newExists != exists) {
            out.println(UNTAGGED + SP + newExists + " EXISTS");
            exists = newExists;
        }
        int newRecent = currentMailbox.getRecent();
        if (newRecent != recent) {
            out.println(UNTAGGED + SP + newRecent + " RECENT");
            recent = newRecent;
        }
        return;
    }

    private void checkExpunge() {
        List newList = currentMailbox.listUIDs(user);
        for (int k = 0; k < newList.size(); k++) {
            getLogger().debug("New List msn " + (k+1) + " is uid "  + newList.get(k));
        }
        for (int i = sequence.size() -1; i > -1 ; i--) {
            Integer j = (Integer)sequence.get(i);
            getLogger().debug("Looking for old msn " + (i+1) + " was uid " + j);
            if (! newList.contains((Integer)sequence.get(i))) {
                out.println(UNTAGGED + SP + (i+1) + " EXPUNGE");
            }
        }
        sequence = newList;
        //newList = null;
        return;
    }
}
