/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.smtpserver;
import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.cornerstone.services.scheduler.PeriodicTimeTrigger;
import org.apache.avalon.cornerstone.services.scheduler.Target;
import org.apache.avalon.cornerstone.services.scheduler.TimeScheduler;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.james.BaseConnectionHandler;
import org.apache.james.Constants;
import org.apache.james.core.MailHeaders;
import org.apache.james.core.MailImpl;
import org.apache.james.services.MailServer;
import org.apache.james.services.UsersRepository;
import org.apache.james.services.UsersStore;
import org.apache.james.util.*;
import org.apache.mailet.MailAddress;
import javax.mail.MessagingException;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
/**
 * Provides SMTP functionality by carrying out the server side of the SMTP
 * interaction.
 *
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
 * @author Jason Borden <jborden@javasense.com>
 * @author Matthew Pangaro <mattp@lokitech.com>
 * @author Danny Angus <danny@thought.co.uk>
 * @author Peter M. Goldstein <farsight@alum.mit.edu>
 *
 * @version This is $Revision: 1.26 $
 */
public class SMTPHandler
    extends BaseConnectionHandler
    implements ConnectionHandler, Composable, Configurable, Target {

    /**
     * SMTP Server identification string used in SMTP headers
     */
    private final static String SOFTWARE_TYPE = "JAMES SMTP Server "
                                                 + Constants.SOFTWARE_VERSION;

    // Keys used to store/lookup data in the internal state hash map

    private final static String SERVER_NAME = "SERVER_NAME";   // Local server name
    private final static String SERVER_TYPE = "SERVER_TYPE";   // SMTP Software Type
    private final static String REMOTE_NAME = "REMOTE_NAME";   // Remote host name
    private final static String REMOTE_IP = "REMOTE_IP";       // Remote IP address
    private final static String NAME_GIVEN = "NAME_GIVEN";     // Remote host name provided by
                                                              // client
    private final static String CURRENT_HELO_MODE = "CURRENT_HELO_MODE"; // HELO or EHLO
    private final static String SENDER = "SENDER_ADDRESS";     // Sender's email address 
    private final static String MESG_FAILED = "MESG_FAILED";   // Message failed flag
    private final static String MESG_SIZE = "MESG_SIZE";       // The size of the message
    private final static String RCPT_VECTOR = "RCPT_VECTOR";   // The message recipients
    private final static String SMTP_ID = "SMTP_ID";           // The SMTP ID associated with
                                                              // the connection
    private final static String AUTH = "AUTHENTICATED";        // The authenticated user id

    /**
     * The character array that indicates termination of an SMTP connection
     */
    private final static char[] SMTPTerminator = { '\r', '\n', '.', '\r', '\n' };

    /**
     * Static Random instance used to generate SMTP ids
     */
    private final static Random random = new Random();

    /**
     * Static RFC822DateFormat used to generate date headers
     */
    private final static RFC822DateFormat rfc822DateFormat = new RFC822DateFormat();

    private Socket socket;    // The TCP/IP socket over which the SMTP 
                              // dialogue is occurring

    private InputStream in;     // The incoming stream of bytes coming from the socket.
    private PrintWriter out;    // The writer to which outgoing messages are written.

    /**
     * A Reader wrapper for the incoming stream of bytes coming from the socket.
     */
    private BufferedReader inReader;

    private String remoteHost;  // The remote host name obtained by lookup on the socket.
    private String remoteIP;    // The remote IP address of the socket.
    private String smtpID;      // The id associated with this particular SMTP interaction.

    private boolean authRequired = false;    // Whether authentication is required to use
                                             // this SMTP server.

    private boolean verifyIdentity = false;  // Whether the server verifies that the user
                                             // actually sending an email matches the
                                             // authentication credentials attached to the
                                             // SMTP interaction.

    private long maxmessagesize = 0;         // The maximum message size allowed by this
                                             // SMTP server.  The default value, 0, means
                                             // no limit.

    private int lengthReset = 20000;         // The number of bytes to read before resetting
                                             // the connection timeout timer.  Defaults to
                                             // 20 seconds.
                                    
    private TimeScheduler scheduler;    // The scheduler used to handle timeouts for the SMTP
                                        // interaction

    private UsersRepository users;      // The user repository for this server - used to authenticate
                                        // users.

    private MailServer mailServer;      // The internal mail server service

    private HashMap state = new HashMap();  // The hash map that holds variables for the SMTP
                                            // session in progress.

    /**
     * Pass the <code>ComponentManager</code> to the <code>composer</code>.
     * The instance uses the specified <code>ComponentManager</code> to 
     * acquire the components it needs for execution.
     *
     * @param componentManager The <code>ComponentManager</code> which this
     *                <code>Composable</code> uses.
     * @throws ComponentException if an error occurs
     */
    public void compose(final ComponentManager componentManager) throws ComponentException {
        mailServer = (MailServer) componentManager.lookup("org.apache.james.services.MailServer");
        scheduler =
            (TimeScheduler) componentManager.lookup(
                "org.apache.avalon.cornerstone.services.scheduler.TimeScheduler");
        UsersStore usersStore =
            (UsersStore) componentManager.lookup("org.apache.james.services.UsersStore");
        users = usersStore.getRepository("LocalUsers");
     }

    /**
     * Pass the <code>Configuration</code> to the instance.
     *
     * @param configuration the class configurations.
     * @throws ConfigurationException if an error occurs
     */
    public void configure(Configuration configuration) throws ConfigurationException {
        super.configure(configuration);
        authRequired = configuration.getChild("authRequired").getValueAsBoolean(false);
        verifyIdentity = configuration.getChild("verifyIdentity").getValueAsBoolean(false);
        // get the message size limit from the conf file and multiply
        // by 1024, to put it in bytes
        maxmessagesize = configuration.getChild( "maxmessagesize" ).getValueAsLong( 0 ) * 1024;
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Max message size is: " + maxmessagesize);
        }
        //how many bytes to read before updating the timer that data is being transfered
        lengthReset = configuration.getChild("lengthReset").getValueAsInteger(20000);
    }

    /**
     * Handle a connection.
     * This handler is responsible for processing connections as they occur.
     *
     * @param connection the connection
     * @throws IOException if an error reading from socket occurs
     * @throws ProtocolException if an error handling connection occurs
     */
    public void handleConnection(Socket connection) throws IOException {
        try {
            this.socket = connection;
            in = new BufferedInputStream(socket.getInputStream(), 1024);
            // An ASCII encoding can be used because all transmissions other
            // that those in the DATA command are guaranteed
            // to be ASCII
            inReader = new BufferedReader(new InputStreamReader(in, "ASCII"));
            out = new InternetPrintWriter(socket.getOutputStream(), true);
            remoteHost = socket.getInetAddress().getHostName();
            remoteIP = socket.getInetAddress().getHostAddress();
            smtpID = random.nextInt(1024) + "";
            resetState();
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
            throw new RuntimeException(exceptionString);
        }

        if (getLogger().isInfoEnabled()) {
            StringBuffer infoBuffer = 
                new StringBuffer(128)
                        .append("Connection from ")
                        .append(remoteHost)
                        .append(" (")
                        .append(remoteIP)
                        .append(")");
            getLogger().info(infoBuffer.toString());
        }

        try {
            // Initially greet the connector
            // Format is:  Sat,  24 Jan 1998 13:16:09 -0500

            final PeriodicTimeTrigger trigger = new PeriodicTimeTrigger( timeout, -1 );
            scheduler.addTrigger( this.toString(), trigger, this );
            StringBuffer responseBuffer =
                new StringBuffer(192)
                    .append("220 ")
                    .append(this.helloName)
                    .append(" SMTP Server (")
                    .append(SOFTWARE_TYPE)
                    .append(") ready ")
                    .append(rfc822DateFormat.format(new Date()));
            String responseString = responseBuffer.toString();
            out.println(responseString);
            out.flush();
            logResponseString(responseString);

            while (parseCommand(inReader.readLine())) {
                scheduler.resetTrigger(this.toString());
            }
            getLogger().debug("Closing socket.");
            scheduler.removeTrigger(this.toString());
        } catch (SocketException se) {
            if (getLogger().isDebugEnabled()) {
                StringBuffer errorBuffer = 
                    new StringBuffer(64)
                        .append("Socket to ")
                        .append(remoteHost)
                        .append(" closed remotely.");
                getLogger().debug(errorBuffer.toString(), se );
            }
        } catch ( InterruptedIOException iioe ) {
            if (getLogger().isDebugEnabled()) {
                StringBuffer errorBuffer = 
                    new StringBuffer(64)
                        .append("Socket to ")
                        .append(remoteHost)
                        .append(" timeout.");
                getLogger().debug( errorBuffer.toString(), iioe );
            }
        } catch ( IOException ioe ) {
            if (getLogger().isDebugEnabled()) {
                StringBuffer errorBuffer = 
                    new StringBuffer(256)
                            .append("Exception handling socket to ")
                            .append(remoteHost)
                            .append(":")
                            .append(ioe.getMessage());
                getLogger().debug( errorBuffer.toString(), ioe );
            }
        } catch (Exception e) {
            if (getLogger().isDebugEnabled()) {
                getLogger().debug( "Exception opening socket: "
                                   + e.getMessage(), e );
            }
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                if (getLogger().isErrorEnabled()) {
                    getLogger().error("Exception closing socket: "
                                      + e.getMessage());
                }
            }
        }
    }

    /**
     * Callback method called when the the PeriodicTimeTrigger in 
     * handleConnection is triggered.  In this case the trigger is
     * being used as a timeout, so the method simply closes the connection.
     *
     * @param triggerName the name of the trigger
     */
    public void targetTriggered(final String triggerName) {
        getLogger().error("Connection timeout on socket");
        try {
            out.println("Connection timeout. Closing connection");
            socket.close();
        } catch (IOException e) {
        }
    }

    /**
     * Resets message-specific, but not authenticated user, state.
     *
     */
    private void resetState() {
        String user = (String) state.get(AUTH);
        state.clear();
        state.put(SERVER_NAME, this.helloName);
        state.put(SERVER_TYPE, SOFTWARE_TYPE);
        state.put(REMOTE_NAME, remoteHost);
        state.put(REMOTE_IP, remoteIP);
        state.put(SMTP_ID, smtpID);
        // seems that after authenticating an smtp client sends
        // a RSET, so we need to remember that they are authenticated
        if (user != null) {
            state.put(AUTH, user);
        }
    }

    /**
     * This method parses SMTP commands read off the wire in handleConnection.
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
    private boolean parseCommand(String command) throws Exception {
        String argument = null;
        String argument1 = null;
        boolean returnValue = true;

        if (command == null) {
            return false;
        }
        if ((state.get(MESG_FAILED) == null) && (getLogger().isDebugEnabled())) {
            getLogger().debug("Command received: " + command);
        }
        command = command.trim();
        if (command.indexOf(" ") > 0) {
            argument = command.substring(command.indexOf(" ") + 1);
            command = command.substring(0, command.indexOf(" "));
            if (argument.indexOf(":") > 0) {
                argument1 = argument.substring(argument.indexOf(":") + 1);
                argument = argument.substring(0, argument.indexOf(":"));
            }
        }
        command = command.toUpperCase(Locale.US);
        if (command.equals("HELO")) {
            doHELO(command, argument, argument1);
        } else if (command.equals("EHLO")) {
            doEHLO(command, argument, argument1);
        } else if (command.equals("AUTH")) {
            doAUTH(command, argument, argument1);
        } else if (command.equals("MAIL")) {
            doMAIL(command, argument, argument1);
        } else if (command.equals("RCPT")) {
            doRCPT(command, argument, argument1);
        } else if (command.equals("NOOP")) {
            doNOOP(command, argument, argument1);
        } else if (command.equals("RSET")) {
            doRSET(command, argument, argument1);
        } else if (command.equals("DATA")) {
            doDATA(command, argument, argument1);
        } else if (command.equals("QUIT")) {
            doQUIT(command, argument, argument1);
            returnValue = false;
        } else {
            doUnknownCmd(command, argument, argument1);
        }
        return returnValue;
    }

    /**
     * Handler method called upon receipt of a HELO command.
     * Responds with a greeting and informs the client whether
     * client authentication is required.
     *
     * @param command the command parsed by the parseCommand method
     * @argument the first argument parsed by the parseCommand method
     * @argument1 the second argument parsed by the parseCommand method
     */
    private void doHELO(String command, String argument, String argument1) {
        String responseString = null;
        if (state.containsKey(CURRENT_HELO_MODE)) {
            StringBuffer responseBuffer =
                    new StringBuffer(96)
                            .append("250 ")
                            .append(state.get(SERVER_NAME))
                            .append(" Duplicate HELO");
            responseString = responseBuffer.toString();
            out.println(responseString);
        } else if (argument == null) {
            responseString = "501 domain address required: " + command;
            out.println(responseString);
        } else {
            state.put(CURRENT_HELO_MODE, command);
            state.put(NAME_GIVEN, argument);
            StringBuffer responseBuffer = new StringBuffer(256);
            if (authRequired) {
                //This is necessary because we're going to do a multiline response
                responseBuffer.append("250-");
            } else {
                responseBuffer.append("250 ");
            }
            responseBuffer.append(state.get(SERVER_NAME))
                          .append(" Hello ")
                          .append(argument)
                          .append(" (")
                          .append(state.get(REMOTE_NAME))
                          .append(" [")
                          .append(state.get(REMOTE_IP))
                          .append("])");
            responseString = responseBuffer.toString();
            out.println(responseString);
            if (authRequired) {
                logResponseString(responseString);
                responseString = "250 AUTH LOGIN PLAIN";
                out.println(responseString);
            }
        }
        out.flush();
        logResponseString(responseString);
    }

    /**
     * Handler method called upon receipt of a EHLO command.
     * Responds with a greeting and informs the client whether
     * client authentication is required.
     *
     * @param command the command parsed by the parseCommand method
     * @argument the first argument parsed by the parseCommand method
     * @argument1 the second argument parsed by the parseCommand method
     */
    private void doEHLO(String command, String argument, String argument1) {
        String responseString = null;
        if (state.containsKey(CURRENT_HELO_MODE)) {
            StringBuffer responseBuffer = 
                new StringBuffer(96)
                        .append("250 ")
                        .append(state.get(SERVER_NAME))
                        .append(" Duplicate EHLO");
            responseString = responseBuffer.toString();
            out.println(responseString);
        } else if (argument == null) {
            responseString = "501 domain address required: " + command;
            out.println(responseString);
        } else {
            state.put(CURRENT_HELO_MODE, command);
            state.put(NAME_GIVEN, argument);
            // Extension defined in RFC 1870
            if (maxmessagesize > 0) {
                responseString = "250-SIZE " + maxmessagesize;
                out.println(responseString);
                logResponseString(responseString);
            }
            StringBuffer responseBuffer = new StringBuffer(256);
            if (authRequired) {
                //This is necessary because we're going to do a multiline response
                responseBuffer.append("250-");
            } else {
                responseBuffer.append("250 ");
            }
            responseBuffer.append(state.get(SERVER_NAME))
                           .append(" Hello ")
                           .append(argument)
                           .append(" (")
                           .append(state.get(REMOTE_NAME))
                           .append(" [")
                           .append(state.get(REMOTE_IP))
                           .append("])");
            responseString = responseBuffer.toString();
            out.println(responseString);
            if (authRequired) {
                logResponseString(responseString);
                responseString = "250 AUTH LOGIN PLAIN";
                out.println(responseString);
            }
        }
        out.flush();
        logResponseString(responseString);
    }

    /**
     * Handler method called upon receipt of a AUTH command.
     * Handles client authentication to the SMTP server.
     *
     * @param command the command parsed by the parseCommand method
     * @argument the first argument parsed by the parseCommand method
     * @argument1 the second argument parsed by the parseCommand method
     */
    private void doAUTH(String command, String argument, String argument1)
            throws Exception {
        String responseString = null;
        if (state.containsKey(AUTH)) {
            responseString = "503 User has previously authenticated. "
                        + " Further authentication is not required!";
            out.println(responseString);
        } else if (argument == null) {
            responseString = "501 Usage: AUTH (authentication type) <challenge>";
            out.println(responseString);
        } else {
            if ((argument1 == null) && (argument.indexOf(" ") > 0)) {
                argument1 = argument.substring(argument.indexOf(" ")+1);
                argument = argument.substring(0,argument.indexOf(" "));
            }
            argument = argument.toUpperCase(Locale.US);
            if (argument.equals("PLAIN")) {
                String userpass = null, user = null, pass = null;
                StringTokenizer authTokenizer;
                if (argument1 == null) {
                    responseString = "334 OK. Continue authentication";
                    out.println(responseString);
                    out.flush();
                    logResponseString(responseString);
                    userpass = inReader.readLine().trim();
                } else {
                    userpass = argument1.trim();
                }
                try {
                    if (userpass != null) {
                        userpass = Base64.decodeAsString(userpass);
                    }
                    if (userpass != null) {
                        authTokenizer = new StringTokenizer(userpass, "\0");
                        user = authTokenizer.nextToken();
                        pass = authTokenizer.nextToken();
                    }
                }
                catch (Exception e) {
                    // Ignored - this exception in parsing will be dealt
                    // with in the if clause below
                }
                // Authenticate user
                if ((user == null) || (pass == null)) {
                    responseString = "501 Could not decode parameters for AUTH PLAIN";
                    out.println(responseString);
                    out.flush();
                } else if (users.test(user, pass)) {
                    state.put(AUTH, user);
                    responseString = "235 Authentication Successful";
                    out.println(responseString);
                    getLogger().info("AUTH method PLAIN succeeded");
                } else {
                    responseString = "535 Authentication Failed";
                    out.println(responseString);
                    getLogger().error("AUTH method PLAIN failed");
                }
                logResponseString(responseString);
                return;
            } else if (argument.equals("LOGIN")) {
                String user = null, pass = null;
                if (argument1 == null) {
                    responseString = "334 VXNlcm5hbWU6"; // base64 encoded "Username:"
                    out.println(responseString);
                    out.flush();
                    logResponseString(responseString);
                    user = inReader.readLine().trim();
                } else {
                    user = argument1.trim();
                }
                if (user != null) {
                    try {
                        user = Base64.decodeAsString(user);
                    } catch (Exception e) {
                        // Ignored - this parse error will be
                        // addressed in the if clause below
                        user = null;
                    }
                }
                responseString = "334 UGFzc3dvcmQ6"; // base64 encoded "Password:"
                out.println(responseString);
                out.flush();
                logResponseString(responseString);
                pass = inReader.readLine().trim();
                if (pass != null) {
                    try {
                        pass = Base64.decodeAsString(pass);
                    } catch (Exception e) {
                        // Ignored - this parse error will be
                        // addressed in the if clause below
                        pass = null;
                    }
                }
                // Authenticate user
                if ((user == null) || (pass == null)) {
                    responseString = "501 Could not decode parameters for AUTH LOGIN";
                    out.println(responseString);
                } else if (users.test(user, pass)) {
                    state.put(AUTH, user);
                    responseString = "235 Authentication Successful";
                    out.println(responseString);
                    getLogger().info("AUTH method LOGIN succeeded");
                } else {
                    responseString = "535 Authentication Failed";
                    out.println(responseString);
                    getLogger().error("AUTH method LOGIN failed");
                }
                out.flush();
                logResponseString(responseString);
                return;
            } else {
                responseString = "504 Unrecognized Authentication Type";
                out.println(responseString);
                logResponseString(responseString);
                if (getLogger().isErrorEnabled()) {
                    StringBuffer errorBuffer =
                        new StringBuffer(128)
                            .append("AUTH method ")
                            .append(argument)
                            .append(" is an unrecognized authentication type");
                    getLogger().error(errorBuffer.toString());
                }
                return;
            }
        }
        out.flush();
        logResponseString(responseString);
    }

    /**
     * Handler method called upon receipt of a MAIL command.
     * Sets up handler to deliver mail as the stated sender.
     *
     * @param command the command parsed by the parseCommand method
     * @argument the first argument parsed by the parseCommand method
     * @argument1 the second argument parsed by the parseCommand method
     */
    private void doMAIL(String command, String argument, String argument1) {
        String responseString = null;
        if (state.containsKey(SENDER)) {
            responseString = "503 Sender already specified";
            out.println(responseString);
        } else if (argument == null || !argument.toUpperCase(Locale.US).equals("FROM")
                   || argument1 == null) {
            responseString = "501 Usage: MAIL FROM:<sender>";
            out.println(responseString);
        } else {
            String sender = argument1.trim();
            int lastChar = sender.lastIndexOf('>');
            // Check to see if any options are present and, if so, whether they are correctly formatted
            // (separated from the closing angle bracket by a ' ').
            if ((lastChar > 0) && (sender.length() > lastChar + 2) && (sender.charAt(lastChar + 1) == ' ')) {
                String mailOptionString = sender.substring(lastChar + 2);

                // Remove the options from the sender
                sender = sender.substring(0, lastChar + 1);

                StringTokenizer optionTokenizer = new StringTokenizer(mailOptionString, " ");
                while (optionTokenizer.hasMoreElements()) {
                    String mailOption = optionTokenizer.nextToken();
                    int equalIndex = mailOptionString.indexOf('=');
                    String mailOptionName = mailOption;
                    String mailOptionValue = "";
                    if (equalIndex > 0) {
                        mailOptionName = mailOption.substring(0, (equalIndex - 1)).toUpperCase(Locale.US);
                        mailOptionValue = mailOption.substring(equalIndex + 1);
                    }

                    // Handle the SIZE extension keyword

                    // TODO: Encapsulate option logic in a method
                    if (mailOptionName.startsWith("SIZE")) {
                        int size = 0;
                        try {
                            size = Integer.parseInt(mailOptionValue);
                        } catch (NumberFormatException pe) {
                            // This is a malformed option value.  We ignore it
                            // and proceed to the next option.
                            continue;
                        }
                        if (getLogger().isDebugEnabled()) {
                            StringBuffer debugBuffer = 
                                new StringBuffer(128)
                                    .append("MAIL command option SIZE received with value ")
                                    .append(size)
                                    .append(".");
                            getLogger().debug(debugBuffer.toString());
                        }
                        if ((maxmessagesize > 0) && (size > maxmessagesize)) {
                            // Let the client know that the size limit has been hit.
                            responseString = "552 Message size exceeds fixed maximum message size";
                            out.println(responseString);
                            out.flush();
    
                            logResponseString(responseString);
                            getLogger().error(responseString);
                            return;
                        } else {
                            // put the message size in the message state so it can be used
                            // later to restrict messages for user quotas, etc.
                            state.put(MESG_SIZE, new Integer(size));
                        }
                    } else {
                        // Unexpected option attached to the Mail command
                        if (getLogger().isDebugEnabled()) {
                            StringBuffer debugBuffer = 
                                new StringBuffer(128)
                                    .append("MAIL command had unrecognized/unexpected option ")
                                    .append(mailOptionName)
                                    .append(" with value ")
                                    .append(mailOptionValue);
                            getLogger().debug(debugBuffer.toString());
                        }
                    }
                }
            }
            if (!sender.startsWith("<") || !sender.endsWith(">")) {
                responseString = "501 Syntax error in parameters or arguments";
                out.println(responseString);
                logResponseString(responseString);
                if (getLogger().isErrorEnabled()) {
                    StringBuffer errorBuffer =
                        new StringBuffer(128)
                            .append("Error parsing sender address: ")
                            .append(sender)
                            .append(": did not start and end with < >");
                    getLogger().error(errorBuffer.toString());
                }
                return;
            }
            MailAddress senderAddress = null;
            //Remove < and >
            sender = sender.substring(1, sender.length() - 1);
            if (sender.length() == 0) {
                //This is the <> case.  Let senderAddress == null
            } else {
                if (sender.indexOf("@") < 0) {
                    sender = sender + "@localhost";
                }
                try {
                    senderAddress = new MailAddress(sender);
                } catch (Exception pe) {
                    responseString = "501 Syntax error in parameters or arguments";
                    out.println(responseString);
                    logResponseString(responseString);
                    if (getLogger().isErrorEnabled()) {
                        StringBuffer errorBuffer = 
                            new StringBuffer(256)
                                    .append("Error parsing sender address: ")
                                    .append(sender)
                                    .append(": ")
                                    .append(pe.getMessage());
                        getLogger().error(errorBuffer.toString());
                    }
                    return;
                }
            }
            state.put(SENDER, senderAddress);
            StringBuffer responseBuffer = 
                new StringBuffer(128)
                        .append("250 Sender <")
                        .append(sender)
                        .append("> OK");
            responseString = responseBuffer.toString();
            out.println(responseString);
        }
        out.flush();
        logResponseString(responseString);
    }

    /**
     * Handler method called upon receipt of a RCPT command.
     * Reads recipient.  Does some connection validation.
     *
     * @param command the command parsed by the parseCommand method
     * @argument the first argument parsed by the parseCommand method
     * @argument1 the second argument parsed by the parseCommand method
     */
    private void doRCPT(String command, String argument, String argument1) {
        String responseString = null;
        if (!state.containsKey(SENDER)) {
            responseString = "503 Need MAIL before RCPT";
            out.println(responseString);
        } else if (argument == null || !argument.toUpperCase(Locale.US).equals("TO")
                   || argument1 == null) {
            responseString = "501 Usage: RCPT TO:<recipient>";
            out.println(responseString);
        } else {
            Collection rcptColl = (Collection) state.get(RCPT_VECTOR);
            if (rcptColl == null) {
                rcptColl = new Vector();
            }
            String recipient = argument1.trim();
            int lastChar = recipient.lastIndexOf('>');
            // Check to see if any options are present and, if so, whether they are correctly formatted
            // (separated from the closing angle bracket by a ' ').
            if ((lastChar > 0) && (recipient.length() > lastChar + 2) && (recipient.charAt(lastChar + 1) == ' ')) {
                String rcptOptionString = recipient.substring(lastChar + 2);

                // Remove the options from the recipient
                recipient = recipient.substring(0, lastChar + 1);

                StringTokenizer optionTokenizer = new StringTokenizer(rcptOptionString, " ");
                while (optionTokenizer.hasMoreElements()) {
                    String rcptOption = optionTokenizer.nextToken();
                    int equalIndex = rcptOptionString.indexOf('=');
                    String rcptOptionName = rcptOption;
                    String rcptOptionValue = "";
                    if (equalIndex > 0) {
                        rcptOptionName = rcptOption.substring(0, (equalIndex - 1)).toUpperCase(Locale.US);
                        rcptOptionValue = rcptOption.substring(equalIndex + 1);
                    }
                    // Unexpected option attached to the RCPT command
                    if (getLogger().isDebugEnabled()) {
                        StringBuffer debugBuffer = 
                            new StringBuffer(128)
                                .append("RCPT command had unrecognized/unexpected option ")
                                .append(rcptOptionName)
                                .append(" with value ")
                                .append(rcptOptionValue);
                        getLogger().debug(debugBuffer.toString());
                    }
                }
            }
            if (!recipient.startsWith("<") || !recipient.endsWith(">")) {
                responseString = "501 Syntax error in parameters or arguments";
                out.println(responseString);
                out.flush();
                logResponseString(responseString);
                if (getLogger().isErrorEnabled()) {
                    StringBuffer errorBuffer = 
                        new StringBuffer(192)
                                .append("Error parsing recipient address: ")
                                .append(recipient)
                                .append(": did not start and end with < >");
                    getLogger().error(errorBuffer.toString());
                }
                return;
            }
            MailAddress recipientAddress = null;
            //Remove < and >
            recipient = recipient.substring(1, recipient.length() - 1);
            if (recipient.indexOf("@") < 0) {
                recipient = recipient + "@localhost";
            }
            try {
                recipientAddress = new MailAddress(recipient);
            } catch (Exception pe) {
                responseString = "501 Syntax error in parameters or arguments";
                out.println(responseString);
                logResponseString(responseString);

                if (getLogger().isErrorEnabled()) {
                    StringBuffer errorBuffer = 
                        new StringBuffer(192)
                                .append("Error parsing recipient address: ")
                                .append(recipient)
                                .append(": ")
                                .append(pe.getMessage());
                    getLogger().error(errorBuffer.toString());
                }
                return;
            }
            if (authRequired) {
                // Make sure the mail is being sent locally if not
                // authenticated else reject.
                if (!state.containsKey(AUTH)) {
                    String toDomain = recipientAddress.getHost();
                    if (!mailServer.isLocalServer(toDomain)) {
                        responseString = "530 Authentication Required";
                        out.println(responseString);
                        logResponseString(responseString);
                        getLogger().error("Authentication is required for mail request");
                        return;
                    }
                } else {
                    // Identity verification checking
                    if (verifyIdentity) {
                        String authUser = ((String) state.get(AUTH)).toLowerCase(Locale.US);
                        MailAddress senderAddress = (MailAddress) state.get(SENDER);
                        boolean domainExists = false;

                        if (!authUser.equals(senderAddress.getUser())) {
                            responseString = "503 Incorrect Authentication for Specified Email Address";
                            out.println(responseString);
                            logResponseString(responseString);
                            if (getLogger().isErrorEnabled()) {
                                StringBuffer errorBuffer =
                                    new StringBuffer(128)
                                        .append("User ")
                                        .append(authUser)
                                        .append(" authenticated, however tried sending email as ")
                                        .append(senderAddress);
                                getLogger().error(errorBuffer.toString());
                            }
                            return;
                        }
                        if (!mailServer.isLocalServer(
                                           senderAddress.getHost())) {
                            responseString = "503 Incorrect Authentication for Specified Email Address";
                            out.println(responseString);
                            logResponseString(responseString);
                            if (getLogger().isErrorEnabled()) {
                                StringBuffer errorBuffer =
                                    new StringBuffer(128)
                                        .append("User ")
                                        .append(authUser)
                                        .append(" authenticated, however tried sending email as ")
                                        .append(senderAddress);
                                getLogger().error(errorBuffer.toString());
                            }
                            return;
                        }
                    }
                }
            }
            rcptColl.add(recipientAddress);
            state.put(RCPT_VECTOR, rcptColl);
            StringBuffer responseBuffer = 
                new StringBuffer(96)
                        .append("250 Recipient <")
                        .append(recipient)
                        .append("> OK");
            responseString = responseBuffer.toString();
            out.println(responseString);
        }
        out.flush();
        logResponseString(responseString);
    }

    /**
     * Handler method called upon receipt of a NOOP command.
     * Just sends back an OK and logs the command.
     *
     * @param command the command parsed by the parseCommand method
     * @argument the first argument parsed by the parseCommand method
     * @argument1 the second argument parsed by the parseCommand method
     */
    private void doNOOP(String command, String argument, String argument1) {
        String responseString = "250 OK";
        out.println(responseString);
        logResponseString(responseString);
    }

    /**
     * Handler method called upon receipt of a RSET command.
     * Resets message-specific, but not authenticated user, state.
     *
     * @param command the command parsed by the parseCommand method
     * @argument the first argument parsed by the parseCommand method
     * @argument1 the second argument parsed by the parseCommand method
     */
    private void doRSET(String command, String argument, String argument1) {
        resetState();
        String responseString = "250 OK";
        out.println(responseString);
        out.flush();
        logResponseString(responseString);
    }

    /**
     * Handler method called upon receipt of a DATA command.
     * Reads in message data, creates header, and delivers to
     * mail server service for delivery.
     *
     * @param command the command parsed by the parseCommand method
     * @argument the first argument parsed by the parseCommand method
     * @argument1 the second argument parsed by the parseCommand method
     */
    private void doDATA(String command, String argument, String argument1) {
        String responseString = null;
        if (!state.containsKey(SENDER)) {
            responseString = "503 No sender specified";
            out.println(responseString);
        } else if (!state.containsKey(RCPT_VECTOR)) {
            responseString = "503 No recipients specified";
            out.println(responseString);
        } else {
            responseString = "354 Ok Send data ending with <CRLF>.<CRLF>";
            out.println(responseString);
            out.flush();
            logResponseString(responseString);
            try {
                // Setup the input stream to notify the scheduler periodically
                InputStream msgIn =
                    new SchedulerNotifyInputStream(in, scheduler, this.toString(), 20000);
                // Look for data termination
                msgIn = new CharTerminatedInputStream(msgIn, SMTPTerminator);
                // if the message size limit has been set, we'll
                // wrap msgIn with a SizeLimitedInputStream
                if (maxmessagesize > 0) {
                    if (getLogger().isDebugEnabled()) {
                        StringBuffer logBuffer = 
                            new StringBuffer(128)
                                    .append("Using SizeLimitedInputStream ")
                                    .append(" with max message size: ")
                                    .append(maxmessagesize);
                        getLogger().debug(logBuffer.toString());
                    }
                    msgIn = new SizeLimitedInputStream(msgIn, maxmessagesize);
                }
                //Removes the dot stuffing
                msgIn = new SMTPInputStream(msgIn);
                //Parse out the message headers
                MailHeaders headers = new MailHeaders(msgIn);
                // if headers do not contains minimum REQUIRED headers fields,
                // add them
                if (!headers.isSet(RFC2822Headers.DATE)) {
                    headers.setHeader(RFC2822Headers.DATE, rfc822DateFormat.format(new Date()));
                }
                if (!headers.isSet(RFC2822Headers.FROM) && state.get(SENDER) != null) {
                    headers.setHeader(RFC2822Headers.FROM, state.get(SENDER).toString());
                }
                //Determine the Return-Path
                String returnPath = headers.getHeader(RFC2822Headers.RETURN_PATH, "\r\n");
                headers.removeHeader(RFC2822Headers.RETURN_PATH);
                if (returnPath == null) {
                    if (state.get(SENDER) == null) {
                        returnPath = "<>";
                    } else {
                        StringBuffer returnPathBuffer = 
                            new StringBuffer(64)
                                    .append("<")
                                    .append(state.get(SENDER))
                                    .append(">");
                        returnPath = returnPathBuffer.toString();
                    }
                }
                //We will rebuild the header object to put Return-Path and our
                //  Received message at the top
                Enumeration headerLines = headers.getAllHeaderLines();
                headers = new MailHeaders();
                //Put the Return-Path first
                headers.addHeaderLine(RFC2822Headers.RETURN_PATH + ": " + returnPath);
                //Put our Received header next
                StringBuffer headerLineBuffer = 
                    new StringBuffer(128)
                            .append(RFC2822Headers.RECEIVED + ": from ")
                            .append(state.get(REMOTE_NAME))
                            .append(" ([")
                            .append(state.get(REMOTE_IP))
                            .append("])");

                headers.addHeaderLine(headerLineBuffer.toString());

                headerLineBuffer = 
                    new StringBuffer(256)
                            .append("          by ")
                            .append(this.helloName)
                            .append(" (")
                            .append(SOFTWARE_TYPE)
                            .append(") with SMTP ID ")
                            .append(state.get(SMTP_ID));

                if (((Collection) state.get(RCPT_VECTOR)).size() == 1) {
                    //Only indicate a recipient if they're the only recipient
                    //(prevents email address harvesting and large headers in
                    // bulk email)
                    headers.addHeaderLine(headerLineBuffer.toString());
                    headerLineBuffer = 
                        new StringBuffer(256)
                                .append("          for <")
                                .append(((Vector) state.get(RCPT_VECTOR)).get(0).toString())
                                .append(">;");
                    headers.addHeaderLine(headerLineBuffer.toString());
                } else {
                    //Put the ; on the end of the 'by' line
                    headerLineBuffer.append(";");
                    headers.addHeaderLine(headerLineBuffer.toString());
                }
                headers.addHeaderLine("          " + rfc822DateFormat.format(new Date()));

                //Add all the original message headers back in next
                while (headerLines.hasMoreElements()) {
                    headers.addHeaderLine((String) headerLines.nextElement());
                }
                ByteArrayInputStream headersIn = new ByteArrayInputStream(headers.toByteArray());
                MailImpl mail =
                    new MailImpl(
                        mailServer.getId(),
                        (MailAddress) state.get(SENDER),
                        (Vector) state.get(RCPT_VECTOR),
                        new SequenceInputStream(headersIn, msgIn));
                // if the message size limit has been set, we'll
                // call mail.getSize() to force the message to be
                // loaded. Need to do this to enforce the size limit
                if (maxmessagesize > 0) {
                    mail.getMessageSize();
                }
                mail.setRemoteHost((String) state.get(REMOTE_NAME));
                mail.setRemoteAddr((String) state.get(REMOTE_IP));
                mailServer.sendMail(mail);
            } catch (MessagingException me) {
                //Grab any exception attached to this one.
                Exception e = me.getNextException();
                //If there was an attached exception, and it's a
                //MessageSizeException
                if (e != null && e instanceof MessageSizeException) {
                    // Add an item to the state to suppress
                    // logging of extra lines of data
                    // that are sent after the size limit has
                    // been hit.
                    state.put(MESG_FAILED, Boolean.TRUE);
                    //then let the client know that the size
                    //limit has been hit.
                    responseString = "552 Error processing message: "
                                + e.getMessage();
                } else {
                    responseString = "451 Error processing message: "
                                + me.getMessage();
                }
                out.println(responseString);
                out.flush();
                logResponseString(responseString);
                getLogger().error(responseString);
                return;
            }
            getLogger().info("Mail sent to Mail Server");
            resetState();
            responseString = "250 Message received";
            out.println(responseString);
        }
        out.flush();
        logResponseString(responseString);
    }

    /**
     * Handler method called upon receipt of a QUIT command.
     * This method informs the client that the connection is
     * closing.
     *
     * @param command the command parsed by the parseCommand method
     * @argument the first argument parsed by the parseCommand method
     * @argument1 the second argument parsed by the parseCommand method
     */
    private void doQUIT(String command, String argument, String argument1) {
        StringBuffer responseBuffer =
            new StringBuffer(128)
                    .append("221 ")
                    .append(state.get(SERVER_NAME))
                    .append(" Service closing transmission channel");
        String responseString = responseBuffer.toString();
        out.println(responseString);
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
    private void doUnknownCmd(String command, String argument, String argument1) {
        if (state.get(MESG_FAILED) == null) {
            StringBuffer responseBuffer =
                new StringBuffer(128)
                        .append("500 ")
                        .append(state.get(SERVER_NAME))
                        .append(" Syntax error, command unrecognized: ")
                        .append(command);
            String responseString = responseBuffer.toString();
            out.println(responseString);
            out.flush();
            logResponseString(responseString);
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
}
