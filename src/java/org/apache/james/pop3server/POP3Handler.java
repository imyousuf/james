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

package org.apache.james.pop3server;

import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.excalibur.pool.Poolable;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.commons.collections.ListUtils;
import org.apache.james.Constants;
import org.apache.james.core.MailImpl;
import org.apache.james.services.MailRepository;
import org.apache.james.util.CRLFTerminatedReader;
import org.apache.james.util.ExtraDotOutputStream;
import org.apache.james.util.InternetPrintWriter;
import org.apache.james.util.watchdog.BytesWrittenResetOutputStream;
import org.apache.james.util.watchdog.Watchdog;
import org.apache.james.util.watchdog.WatchdogTarget;
import org.apache.mailet.Mail;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * The handler class for POP3 connections.
 *
 */
public class POP3Handler
    extends AbstractLogEnabled
    implements ConnectionHandler, Poolable {

    // POP3 Server identification string used in POP3 headers
    private static final String softwaretype        = "JAMES POP3 Server "
                                                        + Constants.SOFTWARE_VERSION;

    // POP3 response prefixes
    private final static String OK_RESPONSE = "+OK";    // OK response.  Requested content
                                                        // will follow

    private final static String ERR_RESPONSE = "-ERR";  // Error response.  Requested content
                                                        // will not be provided.  This prefix
                                                        // is followed by a more detailed
                                                        // error message

    // Authentication states for the POP3 interaction

    private final static int AUTHENTICATION_READY = 0;    // Waiting for user id

    private final static int AUTHENTICATION_USERSET = 1;  // User id provided, waiting for
                                                          // password

    private final static int TRANSACTION = 2;             // A valid user id/password combination
                                                          // has been provided.  In this state
                                                          // the client can access the mailbox
                                                          // of the specified user

    private static final Mail DELETED = new MailImpl();   // A placeholder for emails deleted
                                                          // during the course of the POP3
                                                          // transaction.  This Mail instance
                                                          // is used to enable fast checks as
                                                          // to whether an email has been
                                                          // deleted from the inbox.

    /**
     * The per-service configuration data that applies to all handlers
     */
    private POP3HandlerConfigurationData theConfigData;

    /**
     * The mail server's copy of the user's inbox
     */
    private MailRepository userInbox;

    /**
     * The thread executing this handler
     */
    private Thread handlerThread;

    /**
     * The TCP/IP socket over which the POP3 interaction
     * is occurring
     */
    private Socket socket;

    /**
     * The reader associated with incoming characters.
     */
    private CRLFTerminatedReader in;

    /**
     * The writer to which outgoing messages are written.
     */
    private PrintWriter out;

    /**
     * The socket's output stream
     */
    private OutputStream outs;

    /**
     * The current transaction state of the handler
     */
    private int state;

    /**
     * The user id associated with the POP3 dialogue
     */
    private String user;

    /**
     * A dynamic list representing the set of
     * emails in the user's inbox at any given time
     * during the POP3 transaction.
     */
    private ArrayList userMailbox = new ArrayList();

    private ArrayList backupUserMailbox;         // A snapshot list representing the set of
                                                 // emails in the user's inbox at the beginning
                                                 // of the transaction

    /**
     * The watchdog being used by this handler to deal with idle timeouts.
     */
    private Watchdog theWatchdog;

    /**
     * The watchdog target that idles out this handler.
     */
    private WatchdogTarget theWatchdogTarget = new POP3WatchdogTarget();

    /**
     * Set the configuration data for the handler.
     *
     * @param theData the configuration data
     */
    void setConfigurationData(POP3HandlerConfigurationData theData) {
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
            getLogger().error("POP3 Connection has idled out.");
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
    public void handleConnection( Socket connection )
            throws IOException {

        String remoteHost = "";
        String remoteIP = "";

        try {
            this.socket = connection;
            synchronized (this) {
                handlerThread = Thread.currentThread();
            }
            // in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "ASCII"), 512);
            in = new CRLFTerminatedReader(new BufferedInputStream(socket.getInputStream(), 512), "ASCII");
            remoteIP = socket.getInetAddress().getHostAddress ();
            remoteHost = socket.getInetAddress().getHostName ();
        } catch (Exception e) {
            if (getLogger().isErrorEnabled()) {
                StringBuffer exceptionBuffer =
                    new StringBuffer(256)
                            .append("Cannot open connection from ")
                            .append(remoteHost)
                            .append(" (")
                            .append(remoteIP)
                            .append("): ")
                            .append(e.getMessage());
                getLogger().error( exceptionBuffer.toString(), e );
            }
        }

        if (getLogger().isInfoEnabled()) {
            StringBuffer logBuffer =
                new StringBuffer(128)
                        .append("Connection from ")
                        .append(remoteHost)
                        .append(" (")
                        .append(remoteIP)
                        .append(") ");
            getLogger().info(logBuffer.toString());
        }

        try {
            outs = new BufferedOutputStream(socket.getOutputStream(), 1024);
            out = new InternetPrintWriter(outs, true);
            state = AUTHENTICATION_READY;
            user = "unknown";
            StringBuffer responseBuffer =
                new StringBuffer(256)
                        .append(OK_RESPONSE)
                        .append(" ")
                        .append(theConfigData.getHelloName())
                        .append(" POP3 server (")
                        .append(POP3Handler.softwaretype)
                        .append(") ready ");
            out.println(responseBuffer.toString());

            theWatchdog.start();
            while (parseCommand(readCommandLine())) {
                theWatchdog.reset();
            }
            theWatchdog.stop();
            if (getLogger().isInfoEnabled()) {
                StringBuffer logBuffer =
                    new StringBuffer(128)
                        .append("Connection for ")
                        .append(user)
                        .append(" from ")
                        .append(remoteHost)
                        .append(" (")
                        .append(remoteIP)
                        .append(") closed.");
                getLogger().info(logBuffer.toString());
            }
        } catch (Exception e) {
            out.println(ERR_RESPONSE + " Error closing connection.");
            out.flush();
            StringBuffer exceptionBuffer =
                new StringBuffer(128)
                        .append("Exception during connection from ")
                        .append(remoteHost)
                        .append(" (")
                        .append(remoteIP)
                        .append(") : ")
                        .append(e.getMessage());
            getLogger().error(exceptionBuffer.toString(), e );
        } finally {
            resetHandler();
        }
    }

    /**
     * Resets the handler data to a basic state.
     */
    private void resetHandler() {

        if (theWatchdog != null) {
            ContainerUtil.dispose(theWatchdog);
            theWatchdog = null;
        }

        // Close and clear streams, sockets

        try {
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException ioe) {
            // Ignoring exception on close
        } finally {
            socket = null;
        }

        try {
            if (in != null) {
                in.close();
            }
        } catch (Exception e) {
            // Ignored
        } finally {
            in = null;
        }

        try {
            if (out != null) {
                out.close();
            }
        } catch (Exception e) {
            // Ignored
        } finally {
            out = null;
        }

        try {
           if (outs != null) {
               outs.close();
            }
        } catch (Exception e) {
            // Ignored
        } finally {
            outs = null;
        }

        synchronized (this) {
            handlerThread = null;
        }

        // Clear user data
        user = null;
        userInbox = null;
        if (userMailbox != null) {
            userMailbox.clear();
            userMailbox = null;
        }

        if (backupUserMailbox != null) {
            backupUserMailbox.clear();
            backupUserMailbox = null;
        }

        // Clear config data
        theConfigData = null;
    }

    /**
     * Implements a "stat".  If the handler is currently in
     * a transaction state, this amounts to a rollback of the
     * mailbox contents to the beginning of the transaction.
     * This method is also called when first entering the
     * transaction state to initialize the handler copies of the
     * user inbox.
     *
     */
    private void stat() {
        userMailbox = new ArrayList();
        userMailbox.add(DELETED);
        try {
            for (Iterator it = userInbox.list(); it.hasNext(); ) {
                String key = (String) it.next();
                Mail mc = userInbox.retrieve(key);
                // Retrieve can return null if the mail is no longer in the store.
                // In this case we simply continue to the next key
                if (mc == null) {
                    continue;
                }
                userMailbox.add(mc);
            }
        } catch(MessagingException e) {
            // In the event of an exception being thrown there may or may not be anything in userMailbox
            getLogger().error("Unable to STAT mail box ", e);
        }
        finally {
            backupUserMailbox = (ArrayList) userMailbox.clone();
        }
    }

    /**
     * Reads a line of characters off the command line.
     *
     * @return the trimmed input line
     * @throws IOException if an exception is generated reading in the input characters
     */
    final String readCommandLine() throws IOException {
        for (;;) try {
            String commandLine = in.readLine();
            if (commandLine != null) {
                commandLine = commandLine.trim();
            }
            return commandLine;
        } catch (CRLFTerminatedReader.TerminationException te) {
            writeLoggedFlushedResponse("-ERR Syntax error at character position " + te.position() + ". CR and LF must be CRLF paired.  See RFC 1939 #3.");
        }
    }

    /**
     * This method parses POP3 commands read off the wire in handleConnection.
     * Actual processing of the command (possibly including additional back and
     * forth communication with the client) is delegated to one of a number of
     * command specific handler methods.  The primary purpose of this method is
     * to parse the raw command string to determine exactly which handler should
     * be called.  It returns true if expecting additional commands, false otherwise.
     *
     * @param rawCommand the raw command string passed in over the socket
     *
     * @return whether additional commands are expected.
     */
    private boolean parseCommand(String rawCommand) {
        if (rawCommand == null) {
            return false;
        }
        boolean returnValue = true;
        String command = rawCommand;
        StringTokenizer commandLine = new StringTokenizer(command, " ");
        int arguments = commandLine.countTokens();
        if (arguments == 0) {
            return true;
        } else if(arguments > 0) {
            command = commandLine.nextToken().toUpperCase(Locale.US);
        }
        if (getLogger().isDebugEnabled()) {
            // Don't display password in logger
            if (!command.equals("PASS")) {
                getLogger().debug("Command received: " + rawCommand);
            } else {
                getLogger().debug("Command received: PASS <password omitted>");
            }
        }
        String argument = null;
        if(arguments > 1) {
            argument = commandLine.nextToken();
        }
        String argument1 = null;
        if(arguments > 2) {
            argument1 = commandLine.nextToken();
        }

        if (command.equals("USER")) {
            doUSER(command,argument,argument1);
        } else if (command.equals("PASS")) {
            doPASS(command,argument,argument1);
        } else if (command.equals("STAT")) {
            doSTAT(command,argument,argument1);
        } else if (command.equals("LIST")) {
            doLIST(command,argument,argument1);
        } else if (command.equals("UIDL")) {
            doUIDL(command,argument,argument1);
        } else if (command.equals("RSET")) {
            doRSET(command,argument,argument1);
        } else if (command.equals("DELE")) {
            doDELE(command,argument,argument1);
        } else if (command.equals("NOOP")) {
            doNOOP(command,argument,argument1);
        } else if (command.equals("RETR")) {
            doRETR(command,argument,argument1);
        } else if (command.equals("TOP")) {
            doTOP(command,argument,argument1);
        } else if (command.equals("QUIT")) {
            returnValue = false;
            doQUIT(command,argument,argument1);
        } else {
            doUnknownCmd(command,argument,argument1);
        }
        return returnValue;
    }

    /**
     * Handler method called upon receipt of a USER command.
     * Reads in the user id.
     *
     * @param command the command parsed by the parseCommand method
     * @param argument the first argument parsed by the parseCommand method
     * @param argument1 the second argument parsed by the parseCommand method
     */
    private void doUSER(String command,String argument,String argument1) {
        String responseString = null;
        if (state == AUTHENTICATION_READY && argument != null) {
            user = argument;
            state = AUTHENTICATION_USERSET;
            responseString = OK_RESPONSE;
        } else {
            responseString = ERR_RESPONSE;
        }
        writeLoggedFlushedResponse(responseString);
    }

    /**
     * Handler method called upon receipt of a PASS command.
     * Reads in and validates the password.
     *
     * @param command the command parsed by the parseCommand method
     * @param argument the first argument parsed by the parseCommand method
     * @param argument1 the second argument parsed by the parseCommand method
     */
    private void doPASS(String command,String argument,String argument1) {
        String responseString = null;
        if (state == AUTHENTICATION_USERSET && argument != null) {
            String passArg = argument;
            if (theConfigData.getUsersRepository().test(user, passArg)) {
                StringBuffer responseBuffer =
                    new StringBuffer(64)
                            .append(OK_RESPONSE)
                            .append(" Welcome ")
                            .append(user);
                responseString = responseBuffer.toString();
                state = TRANSACTION;
                writeLoggedFlushedResponse(responseString);
                userInbox = theConfigData.getMailServer().getUserInbox(user);
                stat();
            } else {
                responseString = ERR_RESPONSE + " Authentication failed.";
                state = AUTHENTICATION_READY;
                writeLoggedFlushedResponse(responseString);
            }
        } else {
            responseString = ERR_RESPONSE;
            writeLoggedFlushedResponse(responseString);
        }
    }

    /**
     * Handler method called upon receipt of a STAT command.
     * Returns the number of messages in the mailbox and its
     * aggregate size.
     *
     * @param command the command parsed by the parseCommand method
     * @param argument the first argument parsed by the parseCommand method
     * @param argument1 the second argument parsed by the parseCommand method
     */
    private void doSTAT(String command,String argument,String argument1) {
        String responseString = null;
        if (state == TRANSACTION) {
            long size = 0;
            int count = 0;
            try {
                for (Iterator i = userMailbox.iterator(); i.hasNext(); ) {
                    Mail mc = (Mail) i.next();
                    if (mc != DELETED) {
                        size += mc.getMessageSize();
                        count++;
                    }
                }
                StringBuffer responseBuffer =
                    new StringBuffer(32)
                            .append(OK_RESPONSE)
                            .append(" ")
                            .append(count)
                            .append(" ")
                            .append(size);
                responseString = responseBuffer.toString();
                writeLoggedFlushedResponse(responseString);
            } catch (MessagingException me) {
                responseString = ERR_RESPONSE;
                writeLoggedFlushedResponse(responseString);
            }
        } else {
            responseString = ERR_RESPONSE;
            writeLoggedFlushedResponse(responseString);
        }
    }

    /**
     * Handler method called upon receipt of a LIST command.
     * Returns the number of messages in the mailbox and its
     * aggregate size, or optionally, the number and size of
     * a single message.
     *
     * @param command the command parsed by the parseCommand method
     * @param argument the first argument parsed by the parseCommand method
     * @param argument1 the second argument parsed by the parseCommand method
     */
    private void doLIST(String command,String argument,String argument1) {
        String responseString = null;
        if (state == TRANSACTION) {
            if (argument == null) {
                long size = 0;
                int count = 0;
                try {
                    for (Iterator i = userMailbox.iterator(); i.hasNext(); ) {
                        Mail mc = (Mail) i.next();
                        if (mc != DELETED) {
                            size += mc.getMessageSize();
                            count++;
                        }
                    }
                    StringBuffer responseBuffer =
                        new StringBuffer(32)
                                .append(OK_RESPONSE)
                                .append(" ")
                                .append(count)
                                .append(" ")
                                .append(size);
                    responseString = responseBuffer.toString();
                    writeLoggedFlushedResponse(responseString);
                    count = 0;
                    for (Iterator i = userMailbox.iterator(); i.hasNext(); count++) {
                        Mail mc = (Mail) i.next();

                        if (mc != DELETED) {
                            responseBuffer =
                                new StringBuffer(16)
                                        .append(count)
                                        .append(" ")
                                        .append(mc.getMessageSize());
                            out.println(responseBuffer.toString());
                        }
                    }
                    out.println(".");
                    out.flush();
                } catch (MessagingException me) {
                    responseString = ERR_RESPONSE;
                    writeLoggedFlushedResponse(responseString);
                }
            } else {
                int num = 0;
                try {
                    num = Integer.parseInt(argument);
                    Mail mc = (Mail) userMailbox.get(num);
                    if (mc != DELETED) {
                        StringBuffer responseBuffer =
                            new StringBuffer(64)
                                    .append(OK_RESPONSE)
                                    .append(" ")
                                    .append(num)
                                    .append(" ")
                                    .append(mc.getMessageSize());
                        responseString = responseBuffer.toString();
                        writeLoggedFlushedResponse(responseString);
                    } else {
                        StringBuffer responseBuffer =
                            new StringBuffer(64)
                                    .append(ERR_RESPONSE)
                                    .append(" Message (")
                                    .append(num)
                                    .append(") already deleted.");
                        responseString = responseBuffer.toString();
                        writeLoggedFlushedResponse(responseString);
                    }
                } catch (IndexOutOfBoundsException npe) {
                    StringBuffer responseBuffer =
                        new StringBuffer(64)
                                .append(ERR_RESPONSE)
                                .append(" Message (")
                                .append(num)
                                .append(") does not exist.");
                    responseString = responseBuffer.toString();
                    writeLoggedFlushedResponse(responseString);
                } catch (NumberFormatException nfe) {
                    StringBuffer responseBuffer =
                        new StringBuffer(64)
                                .append(ERR_RESPONSE)
                                .append(" ")
                                .append(argument)
                                .append(" is not a valid number");
                    responseString = responseBuffer.toString();
                    writeLoggedFlushedResponse(responseString);
                } catch (MessagingException me) {
                    responseString = ERR_RESPONSE;
                    writeLoggedFlushedResponse(responseString);
               }
            }
        } else {
            responseString = ERR_RESPONSE;
            writeLoggedFlushedResponse(responseString);
        }
    }

    /**
     * Handler method called upon receipt of a UIDL command.
     * Returns a listing of message ids to the client.
     *
     * @param command the command parsed by the parseCommand method
     * @param argument the first argument parsed by the parseCommand method
     * @param argument1 the second argument parsed by the parseCommand method
     */
    private void doUIDL(String command,String argument,String argument1) {
        String responseString = null;
        if (state == TRANSACTION) {
            if (argument == null) {
                responseString = OK_RESPONSE + " unique-id listing follows";
                writeLoggedFlushedResponse(responseString);
                int count = 0;
                for (Iterator i = userMailbox.iterator(); i.hasNext(); count++) {
                    Mail mc = (Mail) i.next();
                    if (mc != DELETED) {
                        StringBuffer responseBuffer =
                            new StringBuffer(64)
                                    .append(count)
                                    .append(" ")
                                    .append(mc.getName());
                        out.println(responseBuffer.toString());
                    }
                }
                out.println(".");
                out.flush();
            } else {
                int num = 0;
                try {
                    num = Integer.parseInt(argument);
                    Mail mc = (Mail) userMailbox.get(num);
                    if (mc != DELETED) {
                        StringBuffer responseBuffer =
                            new StringBuffer(64)
                                    .append(OK_RESPONSE)
                                    .append(" ")
                                    .append(num)
                                    .append(" ")
                                    .append(mc.getName());
                        responseString = responseBuffer.toString();
                        writeLoggedFlushedResponse(responseString);
                    } else {
                        StringBuffer responseBuffer =
                            new StringBuffer(64)
                                    .append(ERR_RESPONSE)
                                    .append(" Message (")
                                    .append(num)
                                    .append(") already deleted.");
                        responseString = responseBuffer.toString();
                        writeLoggedFlushedResponse(responseString);
                    }
                } catch (IndexOutOfBoundsException npe) {
                    StringBuffer responseBuffer =
                        new StringBuffer(64)
                                .append(ERR_RESPONSE)
                                .append(" Message (")
                                .append(num)
                                .append(") does not exist.");
                    responseString = responseBuffer.toString();
                    writeLoggedFlushedResponse(responseString);
                } catch (NumberFormatException nfe) {
                    StringBuffer responseBuffer =
                        new StringBuffer(64)
                                .append(ERR_RESPONSE)
                                .append(" ")
                                .append(argument)
                                .append(" is not a valid number");
                    responseString = responseBuffer.toString();
                    writeLoggedFlushedResponse(responseString);
                }
            }
        } else {
            writeLoggedFlushedResponse(ERR_RESPONSE);
        }
    }

    /**
     * Handler method called upon receipt of a RSET command.
     * Calls stat() to reset the mailbox.
     *
     * @param command the command parsed by the parseCommand method
     * @param argument the first argument parsed by the parseCommand method
     * @param argument1 the second argument parsed by the parseCommand method
     */
    private void doRSET(String command,String argument,String argument1) {
        String responseString = null;
        if (state == TRANSACTION) {
            stat();
            responseString = OK_RESPONSE;
        } else {
            responseString = ERR_RESPONSE;
        }
        writeLoggedFlushedResponse(responseString);
    }

    /**
     * Handler method called upon receipt of a DELE command.
     * This command deletes a particular mail message from the
     * mailbox.
     *
     * @param command the command parsed by the parseCommand method
     * @param argument the first argument parsed by the parseCommand method
     * @param argument1 the second argument parsed by the parseCommand method
     */
    private void doDELE(String command,String argument,String argument1) {
        String responseString = null;
        if (state == TRANSACTION) {
            int num = 0;
            try {
                num = Integer.parseInt(argument);
            } catch (Exception e) {
                responseString = ERR_RESPONSE + " Usage: DELE [mail number]";
                writeLoggedFlushedResponse(responseString);
                return;
            }
            try {
                Mail mc = (Mail) userMailbox.get(num);
                if (mc == DELETED) {
                    StringBuffer responseBuffer =
                        new StringBuffer(64)
                                .append(ERR_RESPONSE)
                                .append(" Message (")
                                .append(num)
                                .append(") already deleted.");
                    responseString = responseBuffer.toString();
                    writeLoggedFlushedResponse(responseString);
                } else {
                    userMailbox.set(num, DELETED);
                    writeLoggedFlushedResponse(OK_RESPONSE + " Message deleted");
                }
            } catch (IndexOutOfBoundsException iob) {
                StringBuffer responseBuffer =
                    new StringBuffer(64)
                            .append(ERR_RESPONSE)
                            .append(" Message (")
                            .append(num)
                            .append(") does not exist.");
                responseString = responseBuffer.toString();
                writeLoggedFlushedResponse(responseString);
            }
        } else {
            responseString = ERR_RESPONSE;
            writeLoggedFlushedResponse(responseString);
        }
    }

    /**
     * Handler method called upon receipt of a NOOP command.
     * Like all good NOOPs, does nothing much.
     *
     * @param command the command parsed by the parseCommand method
     * @param argument the first argument parsed by the parseCommand method
     * @param argument1 the second argument parsed by the parseCommand method
     */
    private void doNOOP(String command,String argument,String argument1) {
        String responseString = null;
        if (state == TRANSACTION) {
            responseString = OK_RESPONSE;
            writeLoggedFlushedResponse(responseString);
        } else {
            responseString = ERR_RESPONSE;
            writeLoggedFlushedResponse(responseString);
        }
    }

    /**
     * Handler method called upon receipt of a RETR command.
     * This command retrieves a particular mail message from the
     * mailbox.
     *
     * @param command the command parsed by the parseCommand method
     * @param argument the first argument parsed by the parseCommand method
     * @param argument1 the second argument parsed by the parseCommand method
     */
    private void doRETR(String command,String argument,String argument1) {
        String responseString = null;
        if (state == TRANSACTION) {
            int num = 0;
            try {
                num = Integer.parseInt(argument.trim());
            } catch (Exception e) {
                responseString = ERR_RESPONSE + " Usage: RETR [mail number]";
                writeLoggedFlushedResponse(responseString);
                return;
            }
            try {
                Mail mc = (Mail) userMailbox.get(num);
                if (mc != DELETED) {
                    responseString = OK_RESPONSE + " Message follows";
                    writeLoggedFlushedResponse(responseString);
                    try {
                        ExtraDotOutputStream edouts =
                                new ExtraDotOutputStream(outs);
                        OutputStream nouts = new BytesWrittenResetOutputStream(edouts,
                                                                  theWatchdog,
                                                                  theConfigData.getResetLength());
                        mc.getMessage().writeTo(nouts);
                        nouts.flush();
                        edouts.checkCRLFTerminator();
                        edouts.flush();
                    } finally {
                        out.println(".");
                        out.flush();
                    }
                } else {
                    StringBuffer responseBuffer =
                        new StringBuffer(64)
                                .append(ERR_RESPONSE)
                                .append(" Message (")
                                .append(num)
                                .append(") already deleted.");
                    responseString = responseBuffer.toString();
                    writeLoggedFlushedResponse(responseString);
                }
            } catch (IOException ioe) {
                responseString = ERR_RESPONSE + " Error while retrieving message.";
                writeLoggedFlushedResponse(responseString);
            } catch (MessagingException me) {
                responseString = ERR_RESPONSE + " Error while retrieving message.";
                writeLoggedFlushedResponse(responseString);
            } catch (IndexOutOfBoundsException iob) {
                StringBuffer responseBuffer =
                    new StringBuffer(64)
                            .append(ERR_RESPONSE)
                            .append(" Message (")
                            .append(num)
                            .append(") does not exist.");
                responseString = responseBuffer.toString();
                writeLoggedFlushedResponse(responseString);
            }
        } else {
            responseString = ERR_RESPONSE;
            writeLoggedFlushedResponse(responseString);
        }
    }

    /**
     * Handler method called upon receipt of a TOP command.
     * This command retrieves the top N lines of a specified
     * message in the mailbox.
     *
     * The expected command format is
     *  TOP [mail message number] [number of lines to return]
     *
     * @param command the command parsed by the parseCommand method
     * @param argument the first argument parsed by the parseCommand method
     * @param argument1 the second argument parsed by the parseCommand method
     */
    private void doTOP(String command,String argument,String argument1) {
        String responseString = null;
        if (state == TRANSACTION) {
            int num = 0;
            int lines = 0;
            try {
                num = Integer.parseInt(argument);
                lines = Integer.parseInt(argument1);
            } catch (NumberFormatException nfe) {
                responseString = ERR_RESPONSE + " Usage: TOP [mail number] [Line number]";
                writeLoggedFlushedResponse(responseString);
                return;
            }
            try {
                Mail mc = (Mail) userMailbox.get(num);
                if (mc != DELETED) {
                    responseString = OK_RESPONSE + " Message follows";
                    writeLoggedFlushedResponse(responseString);
                    try {
                        for (Enumeration e = mc.getMessage().getAllHeaderLines(); e.hasMoreElements(); ) {
                            out.println(e.nextElement());
                        }
                        out.println();
                        ExtraDotOutputStream edouts =
                                new ExtraDotOutputStream(outs);
                        OutputStream nouts = new BytesWrittenResetOutputStream(edouts,
                                                                  theWatchdog,
                                                                  theConfigData.getResetLength());
                        writeMessageContentTo(mc.getMessage(),nouts,lines);
                        nouts.flush();
                        edouts.checkCRLFTerminator();
                        edouts.flush();
                    } finally {
                        out.println(".");
                        out.flush();
                    }
                } else {
                    StringBuffer responseBuffer =
                        new StringBuffer(64)
                                .append(ERR_RESPONSE)
                                .append(" Message (")
                                .append(num)
                                .append(") already deleted.");
                    responseString = responseBuffer.toString();
                    writeLoggedFlushedResponse(responseString);
                }
            } catch (IOException ioe) {
                responseString = ERR_RESPONSE + " Error while retrieving message.";
                writeLoggedFlushedResponse(responseString);
            } catch (MessagingException me) {
                responseString = ERR_RESPONSE + " Error while retrieving message.";
                writeLoggedFlushedResponse(responseString);
            } catch (IndexOutOfBoundsException iob) {
                StringBuffer exceptionBuffer =
                    new StringBuffer(64)
                            .append(ERR_RESPONSE)
                            .append(" Message (")
                            .append(num)
                            .append(") does not exist.");
                responseString = exceptionBuffer.toString();
                writeLoggedFlushedResponse(responseString);
            }
        } else {
            responseString = ERR_RESPONSE;
            writeLoggedFlushedResponse(responseString);
        }
    }

    /**
     * Writes the content of the message, up to a total number of lines, out to 
     * an OutputStream.
     *
     * @param out the OutputStream to which to write the content
     * @param lines the number of lines to write to the stream
     *
     * @throws MessagingException if the MimeMessage is not set for this MailImpl
     * @throws IOException if an error occurs while reading or writing from the stream
     */
    public void writeMessageContentTo(MimeMessage message, OutputStream out, int lines)
        throws IOException, MessagingException {
        String line;
        BufferedReader br;
        if (message != null) {
            br = new BufferedReader(new InputStreamReader(message.getRawInputStream()));
            try {
                while (lines-- > 0) {
                    if ((line = br.readLine()) == null) {
                        break;
                    }
                    line += "\r\n";
                    out.write(line.getBytes());
                }
            } finally {
                br.close();
            }
        } else {
            throw new MessagingException("No message set for this MailImpl.");
        }
    }

    /**
     * Handler method called upon receipt of a QUIT command.
     * This method handles cleanup of the POP3Handler state.
     *
     * @param command the command parsed by the parseCommand method
     * @param argument the first argument parsed by the parseCommand method
     * @param argument1 the second argument parsed by the parseCommand method
     */
    private void doQUIT(String command,String argument,String argument1) {
        String responseString = null;
        if (state == AUTHENTICATION_READY ||  state == AUTHENTICATION_USERSET) {
            responseString = OK_RESPONSE + " Apache James POP3 Server signing off.";
            writeLoggedFlushedResponse(responseString);
            return;
        }
        List toBeRemoved =  ListUtils.subtract(backupUserMailbox, userMailbox);
        try {
            userInbox.remove(toBeRemoved);
            // for (Iterator it = toBeRemoved.iterator(); it.hasNext(); ) {
            //    Mail mc = (Mail) it.next();
            //    userInbox.remove(mc.getName());
            //}
            responseString = OK_RESPONSE + " Apache James POP3 Server signing off.";
            writeLoggedFlushedResponse(responseString);
        } catch (Exception ex) {
            responseString = ERR_RESPONSE + " Some deleted messages were not removed";
            writeLoggedFlushedResponse(responseString);
            getLogger().error("Some deleted messages were not removed: " + ex.getMessage());
        }
    }

    /**
     * Handler method called upon receipt of an unrecognized command.
     * Returns an error response and logs the command.
     *
     * @param command the command parsed by the parseCommand method
     * @param argument the first argument parsed by the parseCommand method
     * @param argument1 the second argument parsed by the parseCommand method
     */
    private void doUnknownCmd(String command,String argument,String argument1) {
        writeLoggedFlushedResponse(ERR_RESPONSE);
    }

    /**
     * This method logs at a "DEBUG" level the response string that
     * was sent to the POP3 client.  The method is provided largely
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
        out.println(responseString);
        out.flush();
        logResponseString(responseString);
    }

    /**
     * Write a response string.  The response is also logged.
     * Used for multi-line responses.
     *
     * @param responseString the response string sent to the client
     */
    final void writeLoggedResponse(String responseString) {
        out.println(responseString);
        logResponseString(responseString);
    }

    /**
     * A private inner class which serves as an adaptor
     * between the WatchdogTarget interface and this
     * handler class.
     */
    private class POP3WatchdogTarget
        implements WatchdogTarget {

        /**
         * @see org.apache.james.util.watchdog.WatchdogTarget#execute()
         */
        public void execute() {
            POP3Handler.this.idleClose();
        }

    }

}

