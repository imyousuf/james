/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.pop3server;

import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.cornerstone.services.scheduler.PeriodicTimeTrigger;
import org.apache.avalon.cornerstone.services.scheduler.Target;
import org.apache.avalon.cornerstone.services.scheduler.TimeScheduler;
import org.apache.avalon.excalibur.collections.ListUtils;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.james.BaseConnectionHandler;
import org.apache.james.Constants;
import org.apache.james.core.MailImpl;
import org.apache.james.services.MailRepository;
import org.apache.james.services.MailServer;
import org.apache.james.services.UsersRepository;
import org.apache.james.services.UsersStore;
import org.apache.james.util.ExtraDotOutputStream;
import org.apache.james.util.InternetPrintWriter;
import org.apache.james.util.SchedulerNotifyOutputStream;
import org.apache.mailet.Mail;

import javax.mail.MessagingException;
import java.io.*;
import java.net.Socket;
import java.util.*;

/**
 * The handler class for POP3 connections.
 *
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */
public class POP3Handler
    extends BaseConnectionHandler
    implements ConnectionHandler, Composable, Configurable, Target {

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
     * The internal mail server service
     */
    private MailServer mailServer;

    /**
     * The user repository for this server - used to authenticate users.
     */
    private UsersRepository users;

    private TimeScheduler scheduler;    // The scheduler used to handle timeouts for the
                                        // POP3 interaction

    /**
     * The mail server's copy of the user's inbox
     */
    private MailRepository userInbox;

    /**
     * The TCP/IP socket over which the POP3 interaction
     * is occurring
     */
    private Socket socket;

    /**
     * The reader associated with incoming characters.
     */
    private BufferedReader in;

    /**
     * The writer to which outgoing messages are written.
     */
    private PrintWriter out;

    private OutputStream outs;     // The socket's output stream

    private int state;             // The current transaction state of the handler

    /**
     * The user id associated with the POP3 dialogue
     */
    private String user;

    private Vector userMailbox = new Vector();   // A dynamic list representing the set of
                                                 // emails in the user's inbox at any given time
                                                 // during the POP3 transaction

    private Vector backupUserMailbox;            // A snapshot list representing the set of 
                                                 // emails in the user's inbox at the beginning
                                                 // of the transaction

    private int lengthReset = 20000;         // The number of bytes to read before resetting
                                             // the connection timeout timer.  Defaults to
                                             // 20 seconds.

    /**
     * @see org.apache.avalon.framework.component.Composable#compose(ComponentManager)
     */
    public void compose( final ComponentManager componentManager )
        throws ComponentException {
        mailServer = (MailServer)componentManager.
            lookup( "org.apache.james.services.MailServer" );
        UsersStore usersStore = (UsersStore)componentManager.
            lookup( "org.apache.james.services.UsersStore" );
        users = usersStore.getRepository("LocalUsers");
        scheduler = (TimeScheduler)componentManager.
            lookup( "org.apache.avalon.cornerstone.services.scheduler.TimeScheduler" );
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration configuration)
            throws ConfigurationException {
        super.configure(configuration);

        lengthReset = configuration.getChild("lengthReset").getValueAsInteger(20000);
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
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outs = socket.getOutputStream();
            out = new InternetPrintWriter(outs, true);
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
            final PeriodicTimeTrigger trigger = new PeriodicTimeTrigger( timeout, -1 );
            scheduler.addTrigger( this.toString(), trigger, this );
            state = AUTHENTICATION_READY;
            user = "unknown";
            StringBuffer responseBuffer =
                new StringBuffer(256)
                        .append(OK_RESPONSE)
                        .append(" ")
                        .append(this.helloName)
                        .append(" POP3 server (")
                        .append(this.softwaretype)
                        .append(") ready ");
            out.println(responseBuffer.toString());
            while (parseCommand(in.readLine())) {
                scheduler.resetTrigger(this.toString());
            }
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
            try {
                socket.close();
            } catch (IOException ioe) {  }

            // release from scheduler.
            try {
                scheduler.removeTrigger(this.toString());
            } catch(Throwable t) { }
        }
    }

    /**
     * Callback method called when the the PeriodicTimeTrigger in 
     * handleConnection is triggered.  In this case the trigger is
     * being used as a timeout, so the method simply closes the connection.
     *
     * @param triggerName the name of the trigger
     */
    public void targetTriggered( final String triggerName ) {
        getLogger().error("Connection timeout on socket");
        try {
            out.println("Connection timeout. Closing connection");
            socket.close();
        } catch (IOException e) {
        }
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
        userMailbox = new Vector();
        userMailbox.addElement(DELETED);
        for (Iterator it = userInbox.list(); it.hasNext(); ) {
            String key = (String) it.next();
            MailImpl mc = userInbox.retrieve(key);
            // Retrieve can return null if the mail is no longer in the store.
            // In this case we simply continue to the next key
            if (mc == null) {
                continue;
            }
            userMailbox.addElement(mc);
        }
        backupUserMailbox = (Vector) userMailbox.clone();
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
        String command = rawCommand.trim();
        rawCommand = command;
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
        String argument = (String) null;
        if(arguments > 1) {
            argument = commandLine.nextToken();
        }
        String argument1 = (String) null;
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
     * @argument the first argument parsed by the parseCommand method
     * @argument1 the second argument parsed by the parseCommand method
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
        out.println(responseString);
        out.flush();
        logResponseString(responseString);
    }

    /**
     * Handler method called upon receipt of a PASS command.
     * Reads in and validates the password.
     *
     * @param command the command parsed by the parseCommand method
     * @argument the first argument parsed by the parseCommand method
     * @argument1 the second argument parsed by the parseCommand method
     */
    private void doPASS(String command,String argument,String argument1) {
        String responseString = null;
        if (state == AUTHENTICATION_USERSET && argument != null) {
            String passArg = argument;
            if (users.test(user, passArg)) {
                StringBuffer responseBuffer =
                    new StringBuffer(64)
                            .append(OK_RESPONSE)
                            .append(" Welcome ")
                            .append(user);
                responseString = responseBuffer.toString();
                state = TRANSACTION;
                out.println(responseString);
                userInbox = mailServer.getUserInbox(user);
                stat();
            } else {
                responseString = ERR_RESPONSE + " Authentication failed.";
                state = AUTHENTICATION_READY;
                out.println(responseString);
            }
        } else {
            responseString = ERR_RESPONSE;
            out.println(responseString);
        }
        out.flush();
        logResponseString(responseString);
    }

    /**
     * Handler method called upon receipt of a STAT command.
     * Returns the number of messages in the mailbox and its
     * aggregate size.
     *
     * @param command the command parsed by the parseCommand method
     * @argument the first argument parsed by the parseCommand method
     * @argument1 the second argument parsed by the parseCommand method
     */
    private void doSTAT(String command,String argument,String argument1) {
        String responseString = null;
        if (state == TRANSACTION) {
            long size = 0;
            int count = 0;
            try {
                for (Enumeration e = userMailbox.elements(); e.hasMoreElements(); ) {
                    MailImpl mc = (MailImpl) e.nextElement();
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
                out.println(responseString);
            } catch (MessagingException me) {
                responseString = ERR_RESPONSE;
                out.println(responseString);
            }
        } else {
            responseString = ERR_RESPONSE;
            out.println(responseString);
        }
        out.flush();
        logResponseString(responseString);
    }

    /**
     * Handler method called upon receipt of a LIST command.
     * Returns the number of messages in the mailbox and its
     * aggregate size, or optionally, the number and size of
     * a single message.
     *
     * @param command the command parsed by the parseCommand method
     * @argument the first argument parsed by the parseCommand method
     * @argument1 the second argument parsed by the parseCommand method
     */
    private void doLIST(String command,String argument,String argument1) {
        String responseString = null;
        if (state == TRANSACTION) {
            if (argument == null) {
                long size = 0;
                int count = 0;
                try {
                    for (Enumeration e = userMailbox.elements(); e.hasMoreElements(); ) {
                        MailImpl mc = (MailImpl) e.nextElement();
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
                    out.println(responseString);
                    count = 0;
                    for (Enumeration e = userMailbox.elements(); e.hasMoreElements(); count++) {
                        MailImpl mc = (MailImpl) e.nextElement();
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
                } catch (MessagingException me) {
                    responseString = ERR_RESPONSE;
                    out.println(responseString);
                }
            } else {
                int num = 0;
                try {
                    num = Integer.parseInt(argument);
                    MailImpl mc = (MailImpl) userMailbox.elementAt(num);
                    if (mc != DELETED) {
                        StringBuffer responseBuffer =
                            new StringBuffer(64)
                                    .append(OK_RESPONSE)
                                    .append(" ")
                                    .append(num)
                                    .append(" ")
                                    .append(mc.getMessageSize());
                        responseString = responseBuffer.toString();
                        out.println(responseString);
                    } else {
                        StringBuffer responseBuffer =
                            new StringBuffer(64)
                                    .append(ERR_RESPONSE)
                                    .append(" Message (")
                                    .append(num)
                                    .append(") does not exist.");
                        responseString = responseBuffer.toString();
                        out.println(responseString);
                    }
                } catch (ArrayIndexOutOfBoundsException npe) {
                    StringBuffer responseBuffer =
                        new StringBuffer(64)
                                .append(ERR_RESPONSE)
                                .append(" Message (")
                                .append(num)
                                .append(") does not exist.");
                    responseString = responseBuffer.toString();
                    out.println(responseString);
                } catch (NumberFormatException nfe) {
                    StringBuffer responseBuffer =
                        new StringBuffer(64)
                                .append(ERR_RESPONSE)
                                .append(" ")
                                .append(argument)
                                .append(" is not a valid number");
                    responseString = responseBuffer.toString();
                    out.println(responseString);
                } catch (MessagingException me) {
                    responseString = ERR_RESPONSE;
                    out.println(responseString);
                }
            }
        } else {
            responseString = ERR_RESPONSE;
            out.println(responseString);
        }
        out.flush();
        logResponseString(responseString);
    }

    /**
     * Handler method called upon receipt of a UIDL command.
     * Returns a listing of message ids to the client.
     *
     * @param command the command parsed by the parseCommand method
     * @argument the first argument parsed by the parseCommand method
     * @argument1 the second argument parsed by the parseCommand method
     */
    private void doUIDL(String command,String argument,String argument1) {
        String responseString = null;
        if (state == TRANSACTION) {
            if (argument == null) {
                responseString = OK_RESPONSE + " unique-id listing follows";
                out.println(responseString);
                int count = 0;
                for (Enumeration e = userMailbox.elements(); e.hasMoreElements(); count++) {
                    MailImpl mc = (MailImpl) e.nextElement();
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
            } else {
                int num = 0;
                try {
                    num = Integer.parseInt(argument);
                    MailImpl mc = (MailImpl) userMailbox.elementAt(num);
                    if (mc != DELETED) {
                        StringBuffer responseBuffer =
                            new StringBuffer(64)
                                    .append(OK_RESPONSE)
                                    .append(" ")
                                    .append(num)
                                    .append(" ")
                                    .append(mc.getName());
                        responseString = responseBuffer.toString();
                        out.println(responseString);
                    } else {
                        StringBuffer responseBuffer =
                            new StringBuffer(64)
                                    .append(ERR_RESPONSE)
                                    .append(" Message (")
                                    .append(num)
                                    .append(") does not exist.");
                        responseString = responseBuffer.toString();
                        out.println(responseString);
                    }
                } catch (ArrayIndexOutOfBoundsException npe) {
                    StringBuffer responseBuffer =
                        new StringBuffer(64)
                                .append(ERR_RESPONSE)
                                .append(" Message (")
                                .append(num)
                                .append(") does not exist.");
                    responseString = responseBuffer.toString();
                    out.println(responseString);
                } catch (NumberFormatException nfe) {
                    StringBuffer responseBuffer =
                        new StringBuffer(64)
                                .append(ERR_RESPONSE)
                                .append(" ")
                                .append(argument)
                                .append(" is not a valid number");
                    responseString = responseBuffer.toString();
                    out.println(responseString);
                }
            }
        } else {
            out.println(ERR_RESPONSE);
        }
        out.flush();
        logResponseString(responseString);
    }

    /**
     * Handler method called upon receipt of a RSET command.
     * Calls stat() to reset the mailbox.
     *
     * @param command the command parsed by the parseCommand method
     * @argument the first argument parsed by the parseCommand method
     * @argument1 the second argument parsed by the parseCommand method
     */
    private void doRSET(String command,String argument,String argument1) {
        String responseString = null;
        if (state == TRANSACTION) {
            stat();
            responseString = OK_RESPONSE;
        } else {
            responseString = ERR_RESPONSE;
        }
        out.println(responseString);
        out.flush();
        logResponseString(responseString);
    }

    /**
     * Handler method called upon receipt of a DELE command.
     * This command deletes a particular mail message from the
     * mailbox.
     *
     * @param command the command parsed by the parseCommand method
     * @argument the first argument parsed by the parseCommand method
     * @argument1 the second argument parsed by the parseCommand method
     */
    private void doDELE(String command,String argument,String argument1) {
        String responseString = null;
        if (state == TRANSACTION) {
            int num = 0;
            try {
                num = Integer.parseInt(argument);
            } catch (Exception e) {
                responseString = ERR_RESPONSE + " Usage: DELE [mail number]";
                out.println(responseString);
                out.flush();
                logResponseString(responseString);
                return;
            }
            try {
                MailImpl mc = (MailImpl) userMailbox.elementAt(num);
                if (mc == DELETED) {
                    StringBuffer responseBuffer =
                        new StringBuffer(64)
                                .append(ERR_RESPONSE)
                                .append(" Message (")
                                .append(num)
                                .append(") does not exist.");
                    responseString = responseBuffer.toString();
                    out.println(responseString);
                } else {
                    userMailbox.setElementAt(DELETED, num);
                    out.println(OK_RESPONSE + " Message removed");
                }
            } catch (ArrayIndexOutOfBoundsException iob) {
                StringBuffer responseBuffer =
                    new StringBuffer(64)
                            .append(ERR_RESPONSE)
                            .append(" Message (")
                            .append(num)
                            .append(") does not exist.");
                responseString = responseBuffer.toString();
                out.println(responseString);
            }
        } else {
            responseString = ERR_RESPONSE;
            out.println(responseString);
        }
        out.flush();
        logResponseString(responseString);
    }

    /**
     * Handler method called upon receipt of a NOOP command.
     * Like all good NOOPs, does nothing much.
     *
     * @param command the command parsed by the parseCommand method
     * @argument the first argument parsed by the parseCommand method
     * @argument1 the second argument parsed by the parseCommand method
     */
    private void doNOOP(String command,String argument,String argument1) {
        String responseString = null;
        if (state == TRANSACTION) {
            responseString = OK_RESPONSE;
            out.println(responseString);
        } else {
            responseString = ERR_RESPONSE;
            out.println(responseString);
        }
        out.flush();
        logResponseString(responseString);
    }

    /**
     * Handler method called upon receipt of a RETR command.
     * This command retrieves a particular mail message from the
     * mailbox.
     *
     * @param command the command parsed by the parseCommand method
     * @argument the first argument parsed by the parseCommand method
     * @argument1 the second argument parsed by the parseCommand method
     */
    private void doRETR(String command,String argument,String argument1) {
        String responseString = null;
        if (state == TRANSACTION) {
            int num = 0;
            try {
                num = Integer.parseInt(argument.trim());
            } catch (Exception e) {
                responseString = ERR_RESPONSE + " Usage: RETR [mail number]";
                out.println(responseString);
                logResponseString(responseString);
                out.flush();
                return;
            }
            //?May be written as
            //return parseCommand("TOP " + num + " " + Integer.MAX_VALUE);?
            try {
                MailImpl mc = (MailImpl) userMailbox.elementAt(num);
                if (mc != DELETED) {
                    responseString = OK_RESPONSE + " Message follows";
                    out.println(responseString);
                    OutputStream nouts =
                            new ExtraDotOutputStream(
                            new SchedulerNotifyOutputStream(outs, scheduler,
                            this.toString(), lengthReset));
                    mc.writeMessageTo(nouts);
                    out.println();
                    out.println(".");
                } else {
                    StringBuffer responseBuffer =
                        new StringBuffer(64)
                                .append(ERR_RESPONSE)
                                .append(" Message (")
                                .append(num)
                                .append(") deleted.");
                    responseString = responseBuffer.toString();
                    out.println(responseString);
                }
            } catch (IOException ioe) {
                responseString = ERR_RESPONSE + " Error while retrieving message.";
                out.println(responseString);
            } catch (MessagingException me) {
                responseString = ERR_RESPONSE + " Error while retrieving message.";
                out.println(responseString);
            } catch (ArrayIndexOutOfBoundsException iob) {
                StringBuffer responseBuffer =
                    new StringBuffer(64)
                            .append(ERR_RESPONSE)
                            .append(" Message (")
                            .append(num)
                            .append(") does not exist.");
                responseString = responseBuffer.toString();
                out.println(responseString);
            }
            // -------------------------------------------?
        } else {
            responseString = ERR_RESPONSE;
            out.println(responseString);
        }
        out.flush();
        logResponseString(responseString);
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
     * @argument the first argument parsed by the parseCommand method
     * @argument1 the second argument parsed by the parseCommand method
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
                out.println(responseString);
                out.flush();
                logResponseString(responseString);
                return;
            }
            try {
                MailImpl mc = (MailImpl) userMailbox.elementAt(num);
                if (mc != DELETED) {
                    responseString = OK_RESPONSE + " Message follows";
                    out.println(responseString);
                    for (Enumeration e = mc.getMessage().getAllHeaderLines(); e.hasMoreElements(); ) {
                        out.println(e.nextElement());
                    }
                    out.println("");
                    OutputStream nouts =
                            new ExtraDotOutputStream(
                            new SchedulerNotifyOutputStream(outs, scheduler,
                            this.toString(), lengthReset));
                    mc.writeContentTo(nouts, lines);
                    out.println(".");
                } else {
                    StringBuffer responseBuffer =
                        new StringBuffer(64)
                                .append(ERR_RESPONSE)
                                .append(" Message (")
                                .append(num)
                                .append(") already deleted.");
                    responseString = responseBuffer.toString();
                    out.println(responseString);
                }
            } catch (IOException ioe) {
                responseString = ERR_RESPONSE + " Error while retrieving message.";
                out.println(responseString);
            } catch (MessagingException me) {
                responseString = ERR_RESPONSE + " Error while retrieving message.";
                out.println(responseString);
            } catch (ArrayIndexOutOfBoundsException iob) {
                StringBuffer exceptionBuffer =
                    new StringBuffer(64)
                            .append(ERR_RESPONSE)
                            .append(" Message (")
                            .append(num)
                            .append(") does not exist.");
                responseString = exceptionBuffer.toString();
                out.println(responseString);
            }
        } else {
            responseString = ERR_RESPONSE;
            out.println(responseString);
        }
        out.flush();
        logResponseString(responseString);
    }

    /**
     * Handler method called upon receipt of a QUIT command.
     * This method handles cleanup of the POP3Handler state.
     *
     * @param command the command parsed by the parseCommand method
     * @argument the first argument parsed by the parseCommand method
     * @argument1 the second argument parsed by the parseCommand method
     */
    private void doQUIT(String command,String argument,String argument1) {
        String responseString = null;
        if (state == AUTHENTICATION_READY ||  state == AUTHENTICATION_USERSET) {
            return;
        }
        List toBeRemoved =  ListUtils.subtract(backupUserMailbox, userMailbox);
        try {
            for (Iterator it = toBeRemoved.iterator(); it.hasNext(); ) {
                MailImpl mc = (MailImpl) it.next();
                userInbox.remove(mc.getName());
            }
            responseString = OK_RESPONSE + " Apache James POP3 Server signing off.";
            out.println(responseString);
        } catch (Exception ex) {
            responseString = ERR_RESPONSE + " Some deleted messages were not removed";
            out.println(responseString);
            getLogger().error("Some deleted messages were not removed: " + ex.getMessage());
        }
        out.flush();
        logResponseString(responseString);
    }

    /**
     * Handler method called upon receipt of an unrecognized command.
     * Returns an error response and logs the command.
     *
     * @param command the command parsed by the parseCommand method
     * @argument the first argument parsed by the parseCommand method
     * @argument1 the second argument parsed by the parseCommand method
     */
    private void doUnknownCmd(String command,String argument,String argument1) {
        String responseString = ERR_RESPONSE;
        out.println(responseString);
        out.flush();
        logResponseString(responseString);
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
}

