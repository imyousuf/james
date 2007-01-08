/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.nntpserver;

import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.excalibur.pool.Poolable;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.core.MailHeaders;
import org.apache.james.nntpserver.repository.NNTPArticle;
import org.apache.james.nntpserver.repository.NNTPGroup;
import org.apache.james.util.CharTerminatedInputStream;
import org.apache.james.util.DotStuffingInputStream;
import org.apache.james.util.ExtraDotOutputStream;
import org.apache.james.util.InternetPrintWriter;
import org.apache.mailet.dates.RFC977DateFormat;
import org.apache.mailet.dates.RFC2980DateFormat;
import org.apache.mailet.dates.SimplifiedDateFormat;
import org.apache.james.util.watchdog.Watchdog;
import org.apache.james.util.watchdog.WatchdogTarget;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.SequenceInputStream;
import java.net.Socket;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import javax.mail.MessagingException;

/**
 * The NNTP protocol is defined by RFC 977.
 * This implementation is based on IETF draft 15, posted on 15th July '2002.
 * URL: http://www.ietf.org/internet-drafts/draft-ietf-nntpext-base-15.txt
 *
 * Common NNTP extensions are in RFC 2980.
 */
public class NNTPHandler
    extends AbstractLogEnabled
    implements ConnectionHandler, Poolable {

    /**
     * used to calculate DATE from - see 11.3
     */
    private static final SimplifiedDateFormat DF_RFC977 = new RFC977DateFormat();

    /**
     * Date format for the DATE keyword - see 11.1.1
     */
    private static final SimplifiedDateFormat DF_RFC2980 = new RFC2980DateFormat();

    /**
     * The UTC offset for this time zone.
     */
    public static final long UTC_OFFSET = Calendar.getInstance().get(Calendar.ZONE_OFFSET);

    /**
     * The text string for the NNTP MODE command.
     */
    private final static String COMMAND_MODE = "MODE";

    /**
     * The text string for the NNTP LIST command.
     */
    private final static String COMMAND_LIST = "LIST";

    /**
     * The text string for the NNTP GROUP command.
     */
    private final static String COMMAND_GROUP = "GROUP";

    /**
     * The text string for the NNTP NEXT command.
     */
    private final static String COMMAND_NEXT = "NEXT";

    /**
     * The text string for the NNTP LAST command.
     */
    private final static String COMMAND_LAST = "LAST";

    /**
     * The text string for the NNTP ARTICLE command.
     */
    private final static String COMMAND_ARTICLE = "ARTICLE";

    /**
     * The text string for the NNTP HEAD command.
     */
    private final static String COMMAND_HEAD = "HEAD";

    /**
     * The text string for the NNTP BODY command.
     */
    private final static String COMMAND_BODY = "BODY";

    /**
     * The text string for the NNTP STAT command.
     */
    private final static String COMMAND_STAT = "STAT";

    /**
     * The text string for the NNTP POST command.
     */
    private final static String COMMAND_POST = "POST";

    /**
     * The text string for the NNTP IHAVE command.
     */
    private final static String COMMAND_IHAVE = "IHAVE";

    /**
     * The text string for the NNTP QUIT command.
     */
    private final static String COMMAND_QUIT = "QUIT";

    /**
     * The text string for the NNTP SLAVE command.
     */
    private final static String COMMAND_SLAVE = "SLAVE";

    /**
     * The text string for the NNTP DATE command.
     */
    private final static String COMMAND_DATE = "DATE";

    /**
     * The text string for the NNTP HELP command.
     */
    private final static String COMMAND_HELP = "HELP";

    /**
     * The text string for the NNTP NEWGROUPS command.
     */
    private final static String COMMAND_NEWGROUPS = "NEWGROUPS";

    /**
     * The text string for the NNTP NEWNEWS command.
     */
    private final static String COMMAND_NEWNEWS = "NEWNEWS";

    /**
     * The text string for the NNTP LISTGROUP command.
     */
    private final static String COMMAND_LISTGROUP = "LISTGROUP";

    /**
     * The text string for the NNTP OVER command.
     */
    private final static String COMMAND_OVER = "OVER";

    /**
     * The text string for the NNTP XOVER command.
     */
    private final static String COMMAND_XOVER = "XOVER";

    /**
     * The text string for the NNTP HDR command.
     */
    private final static String COMMAND_HDR = "HDR";

    /**
     * The text string for the NNTP XHDR command.
     */
    private final static String COMMAND_XHDR = "XHDR";

    /**
     * The text string for the NNTP AUTHINFO command.
     */
    private final static String COMMAND_AUTHINFO = "AUTHINFO";

    /**
     * The text string for the NNTP PAT command.
     */
    private final static String COMMAND_PAT = "PAT";

    /**
     * The text string for the NNTP MODE READER parameter.
     */
    private final static String MODE_TYPE_READER = "READER";

    /**
     * The text string for the NNTP MODE STREAM parameter.
     */
    private final static String MODE_TYPE_STREAM = "STREAM";

    /**
     * The text string for the NNTP AUTHINFO USER parameter.
     */
    private final static String AUTHINFO_PARAM_USER = "USER";

    /**
     * The text string for the NNTP AUTHINFO PASS parameter.
     */
    private final static String AUTHINFO_PARAM_PASS = "PASS";

    /**
     * The character array that indicates termination of an NNTP message
     */
    private final static char[] NNTPTerminator = { '\r', '\n', '.', '\r', '\n' };

    /**
     * The thread executing this handler 
     */
    private Thread handlerThread;

    /**
     * The remote host name obtained by lookup on the socket.
     */
    private String remoteHost;

    /**
     * The remote IP address of the socket.
     */
    private String remoteIP;

    /**
     * The TCP/IP socket over which the POP3 interaction
     * is occurring
     */
    private Socket socket;

    /**
     * The incoming stream of bytes coming from the socket.
     */
    private InputStream in;

    /**
     * The reader associated with incoming characters.
     */
    private BufferedReader reader;

    /**
     * The socket's output stream
     */
    private OutputStream outs;

    /**
     * The writer to which outgoing messages are written.
     */
    private PrintWriter writer;

    /**
     * The current newsgroup.
     */
    private NNTPGroup group;

    /**
     * The current newsgroup.
     */
    private int currentArticleNumber = -1;

    /**
     * Per-service configuration data that applies to all handlers
     * associated with the service.
     */
    private NNTPHandlerConfigurationData theConfigData;

    /**
     * The user id associated with the NNTP dialogue
     */
    private String user = null;

    /**
     * The password associated with the NNTP dialogue
     */
    private String password = null;

    /**
     * Whether the user for this session has already authenticated.
     * Used to optimize authentication checks
     */
    boolean isAlreadyAuthenticated = false;

    /**
     * The watchdog being used by this handler to deal with idle timeouts.
     */
    private Watchdog theWatchdog;

    /**
     * The watchdog target that idles out this handler.
     */
    private WatchdogTarget theWatchdogTarget = new NNTPWatchdogTarget();

    /**
     * Set the configuration data for the handler
     *
     * @param theData configuration data for the handler
     */
    void setConfigurationData(NNTPHandlerConfigurationData theData) {
        theConfigData = theData;
    }

    /**
     * Set the Watchdog for use by this handler.
     *
     * @param theWatchdog the watchdog
     */
    void setWatchdog(Watchdog theWatchdog) {
        this.theWatchdog = theWatchdog;
    }

    /**
     * Gets the Watchdog Target that should be used by Watchdogs managing
     * this connection.
     *
     * @return the WatchdogTarget
     */
    WatchdogTarget getWatchdogTarget() {
        return theWatchdogTarget;
    }

    /**
     * Idle out this connection
     */
    void idleClose() {
        if (getLogger() != null) {
            getLogger().error("NNTP Connection has idled out.");
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            // ignored
        } finally {
            socket = null;
        }

        synchronized (this) {
            // Interrupt the thread to recover from internal hangs
            if (handlerThread != null) {
                handlerThread.interrupt();
                handlerThread = null;
            }
        }
    }

    /**
     * @see org.apache.avalon.cornerstone.services.connection.ConnectionHandler#handleConnection(Socket)
     */
    public void handleConnection( Socket connection ) throws IOException {
        try {
            this.socket = connection;
            synchronized (this) {
                handlerThread = Thread.currentThread();
            }
            remoteIP = socket.getInetAddress().getHostAddress();
            remoteHost = socket.getInetAddress().getHostName();
            in = new BufferedInputStream(socket.getInputStream(), 1024);
            // An ASCII encoding can be used because all transmissions other
            // that those in the message body command are guaranteed
            // to be ASCII
            reader = new BufferedReader(new InputStreamReader(in, "ASCII"), 512);
            outs = new BufferedOutputStream(socket.getOutputStream(), 1024);
            writer = new InternetPrintWriter(outs, true);
        } catch (Exception e) {
            StringBuffer exceptionBuffer = 
                new StringBuffer(256)
                    .append("Cannot open connection from ")
                    .append(remoteHost)
                    .append(" (")
                    .append(remoteIP)
                    .append("): ")
                    .append(e.getMessage());
            String exceptionString = exceptionBuffer.toString();
            getLogger().error(exceptionString, e );
        }

        try {
            // section 7.1
            if ( theConfigData.getNNTPRepository().isReadOnly() ) {
                StringBuffer respBuffer =
                    new StringBuffer(128)
                        .append("201 ")
                        .append(theConfigData.getHelloName())
                        .append(" NNTP Service Ready, posting prohibited");
                writeLoggedFlushedResponse(respBuffer.toString());
            } else {
                StringBuffer respBuffer =
                    new StringBuffer(128)
                            .append("200 ")
                            .append(theConfigData.getHelloName())
                            .append(" NNTP Service Ready, posting permitted");
                writeLoggedFlushedResponse(respBuffer.toString());
            }

            theWatchdog.start();
            while (parseCommand(reader.readLine())) {
                theWatchdog.reset();
            }
            theWatchdog.stop();

            getLogger().info("Connection closed");
        } catch (Exception e) {
            // If the connection has been idled out, the
            // socket will be closed and null.  Do NOT
            // log the exception or attempt to send the
            // closing connection message
            if (socket != null) {
                try {
                    doQUIT(null);
                } catch (Throwable t) {}
                getLogger().error( "Exception during connection:" + e.getMessage(), e );
            }
        } finally {
            resetHandler();
        }
    }

    /**
     * Resets the handler data to a basic state.
     */
    private void resetHandler() {

        // Clear the Watchdog
        if (theWatchdog != null) {
            ContainerUtil.dispose(theWatchdog);
            theWatchdog = null;
        }

        // Clear the streams
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException ioe) {
            getLogger().warn("NNTPHandler: Unexpected exception occurred while closing reader: " + ioe);
        } finally {
            reader = null;
        }

        in = null;

        if (writer != null) {
            writer.close();
            writer = null;
        }
        outs = null;

        remoteHost = null;
        remoteIP = null;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ioe) {
            getLogger().warn("NNTPHandler: Unexpected exception occurred while closing socket: " + ioe);
        } finally {
            socket = null;
        }

        synchronized (this) {
            handlerThread = null;
        }

        // Clear the selected group, article info
        group = null;
        currentArticleNumber = -1;

        // Clear the authentication info
        user = null;
        password = null;
        isAlreadyAuthenticated = false;

        // Clear the config data
        theConfigData = null;
    }

    /**
     * This method parses NNTP commands read off the wire in handleConnection.
     * Actual processing of the command (possibly including additional back and
     * forth communication with the client) is delegated to one of a number of
     * command specific handler methods.  The primary purpose of this method is
     * to parse the raw command string to determine exactly which handler should
     * be called.  It returns true if expecting additional commands, false otherwise.
     *
     * @param commandRaw the raw command string passed in over the socket
     *
     * @return whether additional commands are expected.
     */
    private boolean parseCommand(String commandRaw) {
        if (commandRaw == null) {
            return false;
        }
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Command received: " + commandRaw);
        }

        String command = commandRaw.trim();
        String argument = null;
        int spaceIndex = command.indexOf(" ");
        if (spaceIndex >= 0) {
            argument = command.substring(spaceIndex + 1);
            command = command.substring(0, spaceIndex);
        }
        command = command.toUpperCase(Locale.US);

        boolean returnValue = true;
        if (!isAuthorized(command) ) {
            writeLoggedFlushedResponse("480 User is not authenticated");
            getLogger().debug("Command not allowed.");
            return returnValue;
        }
        if ((command.equals(COMMAND_MODE)) && (argument != null)) {
            if (argument.toUpperCase(Locale.US).equals(MODE_TYPE_READER)) {
                doMODEREADER(argument);
            } else if (argument.toUpperCase(Locale.US).equals(MODE_TYPE_STREAM)) {
                doMODESTREAM(argument);
            } else {
                writeLoggedFlushedResponse("500 Command not understood");
            }
        } else if ( command.equals(COMMAND_LIST)) {
            doLIST(argument);
        } else if ( command.equals(COMMAND_GROUP) ) {
            doGROUP(argument);
        } else if ( command.equals(COMMAND_NEXT) ) {
            doNEXT(argument);
        } else if ( command.equals(COMMAND_LAST) ) {
            doLAST(argument);
        } else if ( command.equals(COMMAND_ARTICLE) ) {
            doARTICLE(argument);
        } else if ( command.equals(COMMAND_HEAD) ) {
            doHEAD(argument);
        } else if ( command.equals(COMMAND_BODY) ) {
            doBODY(argument);
        } else if ( command.equals(COMMAND_STAT) ) {
            doSTAT(argument);
        } else if ( command.equals(COMMAND_POST) ) {
            doPOST(argument);
        } else if ( command.equals(COMMAND_IHAVE) ) {
            doIHAVE(argument);
        } else if ( command.equals(COMMAND_QUIT) ) {
            doQUIT(argument);
            returnValue = false;
        } else if ( command.equals(COMMAND_DATE) ) {
            doDATE(argument);
        } else if ( command.equals(COMMAND_HELP) ) {
            doHELP(argument);
        } else if ( command.equals(COMMAND_NEWGROUPS) ) {
            doNEWGROUPS(argument);
        } else if ( command.equals(COMMAND_NEWNEWS) ) {
            doNEWNEWS(argument);
        } else if ( command.equals(COMMAND_LISTGROUP) ) {
            doLISTGROUP(argument);
        } else if ( command.equals(COMMAND_OVER) ) {
            doOVER(argument);
        } else if ( command.equals(COMMAND_XOVER) ) {
            doXOVER(argument);
        } else if ( command.equals(COMMAND_HDR) ) {
            doHDR(argument);
        } else if ( command.equals(COMMAND_XHDR) ) {
            doXHDR(argument);
        } else if ( command.equals(COMMAND_AUTHINFO) ) {
            doAUTHINFO(argument);
        } else if ( command.equals(COMMAND_SLAVE) ) {
            doSLAVE(argument);
        } else if ( command.equals(COMMAND_PAT) ) {
            doPAT(argument);
        } else {
            doUnknownCommand(command, argument);
        }
        return returnValue;
    }

    /**
     * Handles an unrecognized command, logging that.
     *
     * @param command the command received from the client
     * @param argument the argument passed in with the command
     */
    private void doUnknownCommand(String command, String argument) {
        if (getLogger().isDebugEnabled()) {
            StringBuffer logBuffer =
                new StringBuffer(128)
                    .append("Received unknown command ")
                    .append(command)
                    .append(" with argument ")
                    .append(argument);
            getLogger().debug(logBuffer.toString());
        }
        writeLoggedFlushedResponse("500 Unknown command");
    }

    /**
     * Implements only the originnal AUTHINFO.
     * for simple and generic AUTHINFO, 501 is sent back. This is as
     * per article 3.1.3 of RFC 2980
     *
     * @param argument the argument passed in with the AUTHINFO command
     */
    private void doAUTHINFO(String argument) {
        String command = null;
        String value = null;
        if (argument != null) {
            int spaceIndex = argument.indexOf(" ");
            if (spaceIndex >= 0) {
                command = argument.substring(0, spaceIndex);
                value = argument.substring(spaceIndex + 1);
            }
        }
        if (command == null) {
            writeLoggedFlushedResponse("501 Syntax error");
            return;
        }
        command = command.toUpperCase(Locale.US);
        if ( command.equals(AUTHINFO_PARAM_USER) ) {
            // Reject re-authentication
            if ( isAlreadyAuthenticated ) {
                writeLoggedFlushedResponse("482 Already authenticated - rejecting new credentials");
            }
            // Reject doubly sent user
            if (user != null) {
                user = null;
                password = null;
                isAlreadyAuthenticated = false;
                writeLoggedFlushedResponse("482 User already specified - rejecting new user");
                return;
            }
            user = value;
            writeLoggedFlushedResponse("381 More authentication information required");
        } else if ( command.equals(AUTHINFO_PARAM_PASS) ) {
            // Reject password sent before user
            if (user == null) {
                writeLoggedFlushedResponse("482 User not yet specified.  Rejecting user.");
                return;
            }
            // Reject doubly sent password
            if (password != null) {
                user = null;
                password = null;
                isAlreadyAuthenticated = false;
                writeLoggedFlushedResponse("482 Password already specified - rejecting new password");
                return;
            }
            password = value;
            isAlreadyAuthenticated = isAuthenticated();
            if ( isAlreadyAuthenticated ) {
                writeLoggedFlushedResponse("281 Authentication accepted");
            } else {
                writeLoggedFlushedResponse("482 Authentication rejected");
                // Clear bad authentication
                user = null;
                password = null;
            }
        } else {
            writeLoggedFlushedResponse("501 Syntax error");
            return;
        }
    }

    /**
     * Lists the articles posted since the date passed in as
     * an argument.
     *
     * @param argument the argument passed in with the NEWNEWS command.
     *                 Should be a wildmat followed by a date.
     */
    private void doNEWNEWS(String argument) {
        // see section 11.4

        String wildmat = "*";

        if (argument != null) {
            int spaceIndex = argument.indexOf(" ");
            if (spaceIndex >= 0) {
                wildmat = argument.substring(0, spaceIndex);
                argument = argument.substring(spaceIndex + 1);
            } else {
                getLogger().error("NEWNEWS had an invalid argument");
                writeLoggedFlushedResponse("501 Syntax error");
                return;
            }
        } else {
            getLogger().error("NEWNEWS had a null argument");
            writeLoggedFlushedResponse("501 Syntax error");
            return;
        }

        Date theDate = null;
        try {
            theDate = getDateFrom(argument);
        } catch (NNTPException nntpe) {
            getLogger().error("NEWNEWS had an invalid argument", nntpe);
            writeLoggedFlushedResponse("501 Syntax error");
            return;
        }

        writeLoggedFlushedResponse("230 list of new articles by message-id follows");
        Iterator groupIter = theConfigData.getNNTPRepository().getMatchedGroups(wildmat);
        while ( groupIter.hasNext() ) {
            Iterator articleIter = ((NNTPGroup)(groupIter.next())).getArticlesSince(theDate);
            while (articleIter.hasNext()) {
                StringBuffer iterBuffer =
                    new StringBuffer(64)
                        .append(((NNTPArticle)articleIter.next()).getUniqueID());
                writeLoggedResponse(iterBuffer.toString());
            }
        }
        writeLoggedFlushedResponse(".");
    }

    /**
     * Lists the groups added since the date passed in as
     * an argument.
     *
     * @param argument the argument passed in with the NEWGROUPS command.
     *                 Should be a date.
     */
    private void doNEWGROUPS(String argument) {
        // see section 11.3
        // both draft-ietf-nntpext-base-15.txt and rfc977 have only group names 
        // in response lines, but INN sends 
        // '<group name> <last article> <first article> <posting allowed>'
        // NOTE: following INN over either document.
        //
        // TODO: Check this.  Audit at http://www.academ.com/pipermail/ietf-nntp/2001-July/002185.html
        // doesn't mention the supposed discrepancy.  Consider changing code to 
        // be in line with spec.
        Date theDate = null;
        try {
            theDate = getDateFrom(argument);
        } catch (NNTPException nntpe) {
            getLogger().error("NEWGROUPS had an invalid argument", nntpe);
            writeLoggedFlushedResponse("501 Syntax error");
            return;
        }
        Iterator iter = theConfigData.getNNTPRepository().getGroupsSince(theDate);
        writeLoggedFlushedResponse("231 list of new newsgroups follows");
        while ( iter.hasNext() ) {
            NNTPGroup currentGroup = (NNTPGroup)iter.next();
            StringBuffer iterBuffer =
                new StringBuffer(128)
                    .append(currentGroup.getName())
                    .append(" ")
                    .append(currentGroup.getLastArticleNumber())
                    .append(" ")
                    .append(currentGroup.getFirstArticleNumber())
                    .append(" ")
                    .append((currentGroup.isPostAllowed()?"y":"n"));
            writeLoggedResponse(iterBuffer.toString());
        }
        writeLoggedFlushedResponse(".");
    }

    /**
     * Lists the help text for the service.
     *
     * @param argument the argument passed in with the HELP command.
     */
    private void doHELP(String argument) {
        writeLoggedResponse("100 Help text follows");
        writeLoggedFlushedResponse(".");
    }

    /**
     * Acknowledges a SLAVE command.  No special preference is given
     * to slave connections.
     *
     * @param argument the argument passed in with the SLAVE command.
     */
    private void doSLAVE(String argument) {
        writeLoggedFlushedResponse("202 slave status noted");
    }

    /**
     * Returns the current date according to the news server.
     *
     * @param argument the argument passed in with the DATE command
     */
    private void doDATE(String argument) {
        Date dt = new Date(System.currentTimeMillis()-UTC_OFFSET);
        String dtStr = DF_RFC2980.format(new Date(dt.getTime() - UTC_OFFSET));
        writeLoggedFlushedResponse("111 " + dtStr);
    }

    /**
     * Quits the transaction.
     *
     * @param argument the argument passed in with the QUIT command
     */
    private void doQUIT(String argument) {
        writeLoggedFlushedResponse("205 closing connection");
    }

    /**
     * Handles the LIST command and its assorted extensions.
     *
     * @param argument the argument passed in with the LIST command.
     */
    private void doLIST(String argument) {
        // see section 9.4.1
        String wildmat = "*";
        boolean isListNewsgroups = false;

        String extension = argument;
        if (argument != null) {
            int spaceIndex = argument.indexOf(" ");
            if (spaceIndex >= 0) {
                wildmat = argument.substring(spaceIndex + 1);
                extension = argument.substring(0, spaceIndex);
            }
            extension = extension.toUpperCase(Locale.US);
        }

        if (extension != null) {
            if (extension.equals("ACTIVE")) {
                isListNewsgroups = false;
            } else if (extension.equals("NEWSGROUPS") ) {
                isListNewsgroups = true;
            } else if (extension.equals("EXTENSIONS") ) {
                doLISTEXTENSIONS();
                return;
            } else if (extension.equals("OVERVIEW.FMT") ) {
                doLISTOVERVIEWFMT();
                return;
            } else if (extension.equals("ACTIVE.TIMES") ) {
                // not supported - 9.4.2.1, 9.4.3.1, 9.4.4.1
                writeLoggedFlushedResponse("503 program error, function not performed");
                return;
            } else if (extension.equals("DISTRIBUTIONS") ) {
                // not supported - 9.4.2.1, 9.4.3.1, 9.4.4.1
                writeLoggedFlushedResponse("503 program error, function not performed");
                return;
            } else if (extension.equals("DISTRIB.PATS") ) {
                // not supported - 9.4.2.1, 9.4.3.1, 9.4.4.1
                writeLoggedFlushedResponse("503 program error, function not performed");
                return;
            } else {
                writeLoggedFlushedResponse("501 Syntax error");
                return;
            }
        }

        Iterator iter = theConfigData.getNNTPRepository().getMatchedGroups(wildmat);
        writeLoggedFlushedResponse("215 list of newsgroups follows");
        while ( iter.hasNext() ) {
            NNTPGroup theGroup = (NNTPGroup)iter.next();
            if (isListNewsgroups) {
                writeLoggedResponse(theGroup.getListNewsgroupsFormat());
            } else {
                writeLoggedResponse(theGroup.getListFormat());
            }
        }
        writeLoggedFlushedResponse(".");
    }

    /**
     * Informs the server that the client has an article with the specified
     * message-ID.
     *
     * @param id the message id
     */
    private void doIHAVE(String id) {
        // see section 9.3.2.1
        if (!isMessageId(id)) {
            writeLoggedFlushedResponse("501 command syntax error");
            return;
        }
        NNTPArticle article = theConfigData.getNNTPRepository().getArticleFromID(id);
        if ( article != null ) {
            writeLoggedFlushedResponse("435 article not wanted - do not send it");
        } else {
            writeLoggedFlushedResponse("335 send article to be transferred. End with <CR-LF>.<CR-LF>");
            try {
                createArticle();
            } catch (RuntimeException e) {
                writeLoggedFlushedResponse("436 transfer failed - try again later");
                throw e;
            }
            writeLoggedFlushedResponse("235 article received ok");
        }
    }

    /**
     * Posts an article to the news server.
     *
     * @param argument the argument passed in with the POST command
     */
    private void doPOST(String argument) {
        // see section 9.3.1.1
        if ( argument != null ) {
            writeLoggedFlushedResponse("501 Syntax error - unexpected parameter");
        }
        writeLoggedFlushedResponse("340 send article to be posted. End with <CR-LF>.<CR-LF>");
        createArticle();
        writeLoggedFlushedResponse("240 article received ok");
    }

    /**
     * Executes the STAT command.  Sets the current article pointer,
     * returns article information.
     *
     * @param the argument passed in to the STAT command,
     *        which should be an article number or message id.
     *        If no parameter is provided, the current selected
     *        article is used.
     */
    private void doSTAT(String param) {
        // section 9.2.4
        NNTPArticle article = null;
        if (isMessageId(param)) {
            article = theConfigData.getNNTPRepository().getArticleFromID(param);
            if ( article == null ) {
                writeLoggedFlushedResponse("430 no such article");
                return;
            } else {
                StringBuffer respBuffer =
                    new StringBuffer(64)
                            .append("223 0 ")
                            .append(param);
                writeLoggedFlushedResponse(respBuffer.toString());
            }
        } else {
            int newArticleNumber = currentArticleNumber;
            if ( group == null ) {
                writeLoggedFlushedResponse("412 no newsgroup selected");
                return;
            } else {
                if ( param == null ) {
                    if ( currentArticleNumber < 0 ) {
                        writeLoggedFlushedResponse("420 no current article selected");
                        return;
                    } else {
                        article = group.getArticle(currentArticleNumber);
                    }
                }
                else {
                    newArticleNumber = Integer.parseInt(param);
                    article = group.getArticle(newArticleNumber);
                }
                if ( article == null ) {
                    writeLoggedFlushedResponse("423 no such article number in this group");
                    return;
                } else {
                    currentArticleNumber = newArticleNumber;
                    String articleID = article.getUniqueID();
                    if (articleID == null) {
                        articleID = "<0>";
                    }
                    StringBuffer respBuffer =
                        new StringBuffer(128)
                                .append("223 ")
                                .append(article.getArticleNumber())
                                .append(" ")
                                .append(articleID);
                    writeLoggedFlushedResponse(respBuffer.toString());
                }
            }
        }
    }

    /**
     * Executes the BODY command.  Sets the current article pointer,
     * returns article information and body.
     *
     * @param the argument passed in to the BODY command,
     *        which should be an article number or message id.
     *        If no parameter is provided, the current selected
     *        article is used.
     */
    private void doBODY(String param) {
        // section 9.2.3
        NNTPArticle article = null;
        if (isMessageId(param)) {
            article = theConfigData.getNNTPRepository().getArticleFromID(param);
            if ( article == null ) {
                writeLoggedFlushedResponse("430 no such article");
                return;
            } else {
                StringBuffer respBuffer =
                    new StringBuffer(64)
                            .append("222 0 ")
                            .append(param);
                writeLoggedFlushedResponse(respBuffer.toString());
            }
        } else {
            int newArticleNumber = currentArticleNumber;
            if ( group == null ) {
                writeLoggedFlushedResponse("412 no newsgroup selected");
                return;
            } else {
                if ( param == null ) {
                    if ( currentArticleNumber < 0 ) {
                        writeLoggedFlushedResponse("420 no current article selected");
                        return;
                    } else {
                        article = group.getArticle(currentArticleNumber);
                    }
                }
                else {
                    newArticleNumber = Integer.parseInt(param);
                    article = group.getArticle(newArticleNumber);
                }
                if ( article == null ) {
                    writeLoggedFlushedResponse("423 no such article number in this group");
                    return;
                } else {
                    currentArticleNumber = newArticleNumber;
                    String articleID = article.getUniqueID();
                    if (articleID == null) {
                        articleID = "<0>";
                    }
                    StringBuffer respBuffer =
                        new StringBuffer(128)
                                .append("222 ")
                                .append(article.getArticleNumber())
                                .append(" ")
                                .append(articleID);
                    writeLoggedFlushedResponse(respBuffer.toString());
                }
            }
        }
        if (article != null) {
            writer.flush();
            article.writeBody(new ExtraDotOutputStream(outs));
            writeLoggedFlushedResponse("\r\n.");
        }
    }

    /**
     * Executes the HEAD command.  Sets the current article pointer,
     * returns article information and headers.
     *
     * @param the argument passed in to the HEAD command,
     *        which should be an article number or message id.
     *        If no parameter is provided, the current selected
     *        article is used.
     */
    private void doHEAD(String param) {
        // section 9.2.2
        NNTPArticle article = null;
        if (isMessageId(param)) {
            article = theConfigData.getNNTPRepository().getArticleFromID(param);
            if ( article == null ) {
                writeLoggedFlushedResponse("430 no such article");
                return;
            } else {
                StringBuffer respBuffer =
                    new StringBuffer(64)
                            .append("221 0 ")
                            .append(param);
                writeLoggedFlushedResponse(respBuffer.toString());
            }
        } else {
            int newArticleNumber = currentArticleNumber;
            if ( group == null ) {
                writeLoggedFlushedResponse("412 no newsgroup selected");
                return;
            } else {
                if ( param == null ) {
                    if ( currentArticleNumber < 0 ) {
                        writeLoggedFlushedResponse("420 no current article selected");
                        return;
                    } else {
                        article = group.getArticle(currentArticleNumber);
                    }
                }
                else {
                    newArticleNumber = Integer.parseInt(param);
                    article = group.getArticle(newArticleNumber);
                }
                if ( article == null ) {
                    writeLoggedFlushedResponse("423 no such article number in this group");
                    return;
                } else {
                    currentArticleNumber = newArticleNumber;
                    String articleID = article.getUniqueID();
                    if (articleID == null) {
                        articleID = "<0>";
                    }
                    StringBuffer respBuffer =
                        new StringBuffer(128)
                                .append("221 ")
                                .append(article.getArticleNumber())
                                .append(" ")
                                .append(articleID);
                    writeLoggedFlushedResponse(respBuffer.toString());
                }
            }
        }
        if (article != null) {
            writer.flush();
            article.writeHead(new ExtraDotOutputStream(outs));
            writeLoggedFlushedResponse(".");
        }
    }

    /**
     * Executes the ARTICLE command.  Sets the current article pointer,
     * returns article information and contents.
     *
     * @param the argument passed in to the ARTICLE command,
     *        which should be an article number or message id.
     *        If no parameter is provided, the current selected
     *        article is used.
     */
    private void doARTICLE(String param) {
        // section 9.2.1
        NNTPArticle article = null;
        if (isMessageId(param)) {
            article = theConfigData.getNNTPRepository().getArticleFromID(param);
            if ( article == null ) {
                writeLoggedFlushedResponse("430 no such article");
                return;
            } else {
                StringBuffer respBuffer =
                    new StringBuffer(64)
                            .append("220 0 ")
                            .append(param);
                writeLoggedResponse(respBuffer.toString());
            }
        } else {
            int newArticleNumber = currentArticleNumber;
            if ( group == null ) {
                writeLoggedFlushedResponse("412 no newsgroup selected");
                return;
            } else {
                if ( param == null ) {
                    if ( currentArticleNumber < 0 ) {
                        writeLoggedFlushedResponse("420 no current article selected");
                        return;
                    } else {
                        article = group.getArticle(currentArticleNumber);
                    }
                }
                else {
                    newArticleNumber = Integer.parseInt(param);
                    article = group.getArticle(newArticleNumber);
                }
                if ( article == null ) {
                    writeLoggedFlushedResponse("423 no such article number in this group");
                    return;
                } else {
                    currentArticleNumber = newArticleNumber;
                    String articleID = article.getUniqueID();
                    if (articleID == null) {
                        articleID = "<0>";
                    }
                    StringBuffer respBuffer =
                        new StringBuffer(128)
                                .append("220 ")
                                .append(article.getArticleNumber())
                                .append(" ")
                                .append(articleID);
                    writeLoggedFlushedResponse(respBuffer.toString());
                }
            }
        }
        if (article != null) {
            writer.flush();
            article.writeArticle(new ExtraDotOutputStream(outs));
            // see jira JAMES-311 for an explanation of the "\r\n"
            writeLoggedFlushedResponse("\r\n.");
        }
    }

    /**
     * Advances the current article pointer to the next article in the group.
     *
     * @param argument the argument passed in with the NEXT command
     */
    private void doNEXT(String argument) {
        // section 9.1.1.3.1
        if ( argument != null ) {
            writeLoggedFlushedResponse("501 Syntax error - unexpected parameter");
        } else if ( group == null ) {
            writeLoggedFlushedResponse("412 no newsgroup selected");
        } else if ( currentArticleNumber < 0 ) {
            writeLoggedFlushedResponse("420 no current article has been selected");
        } else if ( currentArticleNumber >= group.getLastArticleNumber() ) {
            writeLoggedFlushedResponse("421 no next article in this group");
        } else {
            currentArticleNumber++;
            NNTPArticle article = group.getArticle(currentArticleNumber);
            StringBuffer respBuffer =
                new StringBuffer(64)
                        .append("223 ")
                        .append(article.getArticleNumber())
                        .append(" ")
                        .append(article.getUniqueID());
            writeLoggedFlushedResponse(respBuffer.toString());
        }
    }

    /**
     * Moves the currently selected article pointer to the article
     * previous to the currently selected article in the selected group.
     *
     * @param argument the argument passed in with the LAST command
     */
    private void doLAST(String argument) {
        // section 9.1.1.2.1
        if ( argument != null ) {
            writeLoggedFlushedResponse("501 Syntax error - unexpected parameter");
        } else if ( group == null ) {
            writeLoggedFlushedResponse("412 no newsgroup selected");
        } else if ( currentArticleNumber < 0 ) {
            writeLoggedFlushedResponse("420 no current article has been selected");
        } else if ( currentArticleNumber <= group.getFirstArticleNumber() ) {
            writeLoggedFlushedResponse("422 no previous article in this group");
        } else {
            currentArticleNumber--;
            NNTPArticle article = group.getArticle(currentArticleNumber);
            StringBuffer respBuffer =
                new StringBuffer(64)
                        .append("223 ")
                        .append(article.getArticleNumber())
                        .append(" ")
                        .append(article.getUniqueID());
            writeLoggedFlushedResponse(respBuffer.toString());
        }
    }

    /**
     * Selects a group to be the current newsgroup.
     *
     * @param group the name of the group being selected.
     */
    private void doGROUP(String groupName) {
        if (groupName == null) {
            writeLoggedFlushedResponse("501 Syntax error - missing required parameter");
            return;
        }
        NNTPGroup newGroup = theConfigData.getNNTPRepository().getGroup(groupName);
        // section 9.1.1.1
        if ( newGroup == null ) {
            writeLoggedFlushedResponse("411 no such newsgroup");
        } else {
            group = newGroup;
            // if the number of articles in group == 0
            // then the server may return this information in 3 ways, 
            // The clients must honor all those 3 ways.
            // our response is: 
            // highWaterMark == lowWaterMark and number of articles == 0
            int articleCount = group.getNumberOfArticles();
            int lowWaterMark = group.getFirstArticleNumber();
            int highWaterMark = group.getLastArticleNumber();

            // Set the current article pointer.  If no
            // articles are in the group, the current article
            // pointer should be explicitly unset.
            if (articleCount != 0) {
                currentArticleNumber = lowWaterMark;
            } else {
                currentArticleNumber = -1;
            }
            StringBuffer respBuffer =
                new StringBuffer(128)
                        .append("211 ")
                        .append(articleCount)
                        .append(" ")
                        .append(lowWaterMark)
                        .append(" ")
                        .append(highWaterMark)
                        .append(" ")
                        .append(group.getName())
                        .append(" group selected");
            writeLoggedFlushedResponse(respBuffer.toString());
        }
    }

    /**
     * Lists the extensions supported by this news server.
     */
    private void doLISTEXTENSIONS() {
        // 8.1.1
        writeLoggedResponse("202 Extensions supported:");
        writeLoggedResponse("LISTGROUP");
        writeLoggedResponse("AUTHINFO");
        writeLoggedResponse("OVER");
        writeLoggedResponse("XOVER");
        writeLoggedResponse("HDR");
        writeLoggedResponse("XHDR");
        writeLoggedFlushedResponse(".");
    }

    /**
     * Informs the server that the client is a newsreader.
     *
     * @param argument the argument passed in with the MODE READER command
     */
    private void doMODEREADER(String argument) {
        // 7.2
        writeLoggedFlushedResponse(theConfigData.getNNTPRepository().isReadOnly()
                       ? "201 Posting Not Permitted" : "200 Posting Permitted");
    }

    /**
     * Informs the server that the client is a news server.
     *
     * @param argument the argument passed in with the MODE STREAM command
     */
    private void doMODESTREAM(String argument) {
        // 7.2
        writeLoggedFlushedResponse("500 Command not understood");
    }

    /**
     * Gets a listing of article numbers in specified group name
     * or in the already selected group if the groupName is null.
     *
     * @param groupName the name of the group to list
     */
    private void doLISTGROUP(String groupName) {
        // 9.5.1.1.1
        if (groupName==null) {
            if ( group == null) {
                writeLoggedFlushedResponse("412 no news group currently selected");
                return;
            }
        }
        else {
            group = theConfigData.getNNTPRepository().getGroup(groupName);
            if ( group == null ) {
                writeLoggedFlushedResponse("411 no such newsgroup");
                return;
            }
        }
        if ( group != null ) {
            // this.group = group;

            // Set the current article pointer.  If no
            // articles are in the group, the current article
            // pointer should be explicitly unset.
            if (group.getNumberOfArticles() > 0) {
                currentArticleNumber = group.getFirstArticleNumber();
            } else {
                currentArticleNumber = -1;
            }

            writeLoggedFlushedResponse("211 list of article numbers follow");

            Iterator iter = group.getArticles();
            while (iter.hasNext()) {
                NNTPArticle article = (NNTPArticle)iter.next();
                writeLoggedResponse(article.getArticleNumber() + "");
            }
            writeLoggedFlushedResponse(".");
        }
    }

    /**
     * Handles the LIST OVERVIEW.FMT command.  Not supported.
     */
    private void doLISTOVERVIEWFMT() {
        // 9.5.3.1.1
        writeLoggedFlushedResponse("215 Information follows");
        String[] overviewHeaders = theConfigData.getNNTPRepository().getOverviewFormat();
        for (int i = 0;  i < overviewHeaders.length; i++) {
            writeLoggedResponse(overviewHeaders[i]);
        }
        writeLoggedFlushedResponse(".");
    }

    /**
     * Handles the PAT command.  Not supported.
     *
     * @param argument the argument passed in with the PAT command
     */
    private void doPAT(String argument) {
        // 9.5.3.1.1 in draft-12
        writeLoggedFlushedResponse("500 Command not recognized");
    }

    /**
     * Get the values of the headers for the selected newsgroup, 
     * with an optional range modifier.
     *
     * @param argument the argument passed in with the XHDR command.
     */
    private void doXHDR(String argument) {
        doHDR(argument);
    }

    /**
     * Get the values of the headers for the selected newsgroup, 
     * with an optional range modifier.
     *
     * @param argument the argument passed in with the HDR command.
     */
    private void doHDR(String argument) {
        // 9.5.3
        if (argument == null) {
            writeLoggedFlushedResponse("501 Syntax error - missing required parameter");
            return;
        }
        String hdr = argument;
        String range = null;
        int spaceIndex = hdr.indexOf(" ");
        if (spaceIndex >= 0 ) {
            range = hdr.substring(spaceIndex + 1);
            hdr = hdr.substring(0, spaceIndex);
        }
        if (group == null ) {
            writeLoggedFlushedResponse("412 No news group currently selected.");
            return;
        }
        if ((range == null) && (currentArticleNumber < 0)) {
            writeLoggedFlushedResponse("420 No current article selected");
            return;
        }
        NNTPArticle[] article = getRange(range);
        if ( article == null ) {
            writeLoggedFlushedResponse("412 no newsgroup selected");
        } else if ( article.length == 0 ) {
            writeLoggedFlushedResponse("430 no such article");
        } else {
            writeLoggedFlushedResponse("221 Header follows");
            for ( int i = 0 ; i < article.length ; i++ ) {
                String val = article[i].getHeader(hdr);
                if ( val == null ) {
                    val = "";
                }
                StringBuffer hdrBuffer =
                    new StringBuffer(128)
                            .append(article[i].getArticleNumber())
                            .append(" ")
                            .append(val);
                writeLoggedResponse(hdrBuffer.toString());
            }
            writeLoggedFlushedResponse(".");
        }
    }

    /**
     * Returns information from the overview database regarding the
     * current article, or a range of articles.
     *
     * @param range the optional article range.
     */
    private void doXOVER(String range) {
        doOVER(range);
    }

    /**
     * Returns information from the overview database regarding the
     * current article, or a range of articles.
     *
     * @param range the optional article range.
     */
    private void doOVER(String range) {
        // 9.5.2.2.1
        if ( group == null ) {
            writeLoggedFlushedResponse("412 No newsgroup selected");
            return;
        }
        if ((range == null) && (currentArticleNumber < 0)) {
            writeLoggedFlushedResponse("420 No current article selected");
            return;
        }
        NNTPArticle[] article = getRange(range);
        if ( article.length == 0 ) {
            writeLoggedFlushedResponse("420 No article(s) selected");
        } else {
            writeLoggedResponse("224 Overview information follows");
            for ( int i = 0 ; i < article.length ; i++ ) {
                article[i].writeOverview(outs);
                if (i % 100 == 0) {
                    // Reset the watchdog every hundred headers or so
                    // to ensure the connection doesn't timeout for slow
                    // clients
                    theWatchdog.reset();
                }
            }
            writeLoggedFlushedResponse(".");
        }
    }

    /**
     * Handles the transaction for getting the article data.
     */
    private void createArticle() {
        try {
            InputStream msgIn = new CharTerminatedInputStream(in, NNTPTerminator);
            // Removes the dot stuffing
            msgIn = new DotStuffingInputStream(msgIn);
            MailHeaders headers = new MailHeaders(msgIn);
            processMessageHeaders(headers);
            processMessage(headers, msgIn);
        } catch (MessagingException me) {
            throw new NNTPException("MessagingException encountered when loading article.");
        }
    }

    /**
     * Processes the NNTP message headers coming in off the wire.
     *
     * @param headers the headers of the message being read
     */
    private MailHeaders processMessageHeaders(MailHeaders headers)
        throws MessagingException {
        return headers;
    }

    /**
     * Processes the NNTP message coming in off the wire.  Reads the
     * content and delivers to the spool.
     *
     * @param headers the headers of the message being read
     * @param msgIn the stream containing the message content
     */
    private void processMessage(MailHeaders headers, InputStream bodyIn)
        throws MessagingException {
        InputStream messageIn = null;
        try {
            messageIn = new SequenceInputStream(new ByteArrayInputStream(headers.toByteArray()), bodyIn);
            theConfigData.getNNTPRepository().createArticle(messageIn);
        } finally {
            if (messageIn != null) {
                try {
                    messageIn.close();
                } catch (IOException ioe) {
                    // Ignore exception on close.
                }
                messageIn = null;
            }
        }
    }

    /**
     * Returns the date from @param input.
     * The input tokens are assumed to be in format date time [GMT|UTC] .
     * 'date' is in format [XX]YYMMDD. 'time' is in format 'HHMMSS'
     * NOTE: This routine could do with some format checks.
     *
     * @param argument the date string
     */
    private Date getDateFrom(String argument) {
        if (argument == null) {
            throw new NNTPException("Date argument was absent.");
        }
        StringTokenizer tok = new StringTokenizer(argument, " ");
        if (tok.countTokens() < 2) {
            throw new NNTPException("Date argument was ill-formed.");
        }
        String date = tok.nextToken();
        String time = tok.nextToken();
        boolean utc = ( tok.hasMoreTokens() );
        Date d = new Date();
        try {
            StringBuffer dateStringBuffer =
                new StringBuffer(64)
                    .append(date)
                    .append(" ")
                    .append(time);
            Date dt = DF_RFC977.parse(dateStringBuffer.toString());
            if ( utc ) {
                dt = new Date(dt.getTime()+UTC_OFFSET);
            }
            return dt;
        } catch ( ParseException pe ) {
            StringBuffer exceptionBuffer =
                new StringBuffer(128)
                    .append("Date extraction failed: ")
                    .append(date)
                    .append(",")
                    .append(time)
                    .append(",")
                    .append(utc);
            throw new NNTPException(exceptionBuffer.toString());
        }
    }

    /**
     * Returns the list of articles that match the range.
     *
     * A precondition of this method is that the selected
     * group be non-null.  The current article pointer must
     * be valid if no range is explicitly provided.
     *
     * @return null indicates insufficient information to
     * fetch the list of articles
     */
    private NNTPArticle[] getRange(String range) {
        // check for msg id
        if ( isMessageId(range)) {
            NNTPArticle article = theConfigData.getNNTPRepository().getArticleFromID(range);
            return ( article == null )
                ? new NNTPArticle[0] : new NNTPArticle[] { article };
        }

        if ( range == null ) {
            range = "" + currentArticleNumber;
        }

        int start = -1;
        int end = -1;
        int idx = range.indexOf('-');
        if ( idx == -1 ) {
            start = Integer.parseInt(range);
            end = start;
        } else {
            start = Integer.parseInt(range.substring(0,idx));
            if ( (idx + 1) == range.length() ) {
                end = group.getLastArticleNumber();
            } else {
                end = Integer.parseInt(range.substring(idx + 1));
            }
        }
        List list = new ArrayList();
        for ( int i = start ; i <= end ; i++ ) {
            NNTPArticle article = group.getArticle(i);
            if ( article != null ) {
                list.add(article);
            }
        }
        return (NNTPArticle[])list.toArray(new NNTPArticle[0]);
    }

    /**
     * Return whether the user associated with the connection (possibly no
     * user) is authorized to execute the command.
     *
     * @param the command being tested
     * @return whether the command is authorized
     */
    private boolean isAuthorized(String command) {
        isAlreadyAuthenticated = isAlreadyAuthenticated || isAuthenticated();
        if (isAlreadyAuthenticated) {
            return true;
        }
        // some commands are authorized, even if the user is not authenticated
        boolean allowed = command.equals("AUTHINFO");
        allowed = allowed || command.equals("MODE");
        allowed = allowed || command.equals("QUIT");
        return allowed;
    }

    /**
     * Return whether the connection has been authenticated.
     *
     * @return whether the connection has been authenticated.
     */
    private boolean isAuthenticated() {
        if ( theConfigData.isAuthRequired() ) {
            if  ((user != null) && (password != null) && (theConfigData.getUsersRepository() != null)) {
                return theConfigData.getUsersRepository().test(user,password);
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    /**
     * Tests a string to see whether it is formatted as a message
     * ID.
     *
     * @param testString the string to test
     *
     * @return whether the string is a candidate message ID
     */
    private static boolean isMessageId(String testString) {
        if ((testString != null) &&
            (testString.startsWith("<")) &&
            (testString.endsWith(">"))) {
            return true;
        } else {
            return false;
        }
   }

    /**
     * This method logs at a "DEBUG" level the response string that 
     * was sent to the SMTP client.  The method is provided largely
     * as syntactic sugar to neaten up the code base.  It is declared
     * private and final to encourage compiler inlining.
     *
     * @param responseString the response string sent to the client
     */
    private final void logResponseString(String responseString) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Sent: " + responseString);
        }
    }

    /**
     * Write and flush a response string.  The response is also logged.
     * Should be used for the last line of a multi-line response or
     * for a single line response.
     *
     * @param responseString the response string sent to the client
     */
    final void writeLoggedFlushedResponse(String responseString) {
        writer.println(responseString);
        writer.flush();
        logResponseString(responseString);
    }

    /**
     * Write a response string.  The response is also logged. 
     * Used for multi-line responses.
     *
     * @param responseString the response string sent to the client
     */
    final void writeLoggedResponse(String responseString) {
        writer.println(responseString);
        logResponseString(responseString);
    }

    /**
     * A private inner class which serves as an adaptor
     * between the WatchdogTarget interface and this
     * handler class.
     */
    private class NNTPWatchdogTarget
        implements WatchdogTarget {

        /**
         * @see org.apache.james.util.watchdog.WatchdogTarget#execute()
         */
        public void execute() {
            NNTPHandler.this.idleClose();
        }

    }
}
