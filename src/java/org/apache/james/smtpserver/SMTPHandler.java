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
 * @version This is $Revision: 1.30 $
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

    private final static String CURRENT_HELO_MODE = "CURRENT_HELO_MODE"; // HELO or EHLO
    private final static String SENDER = "SENDER_ADDRESS";     // Sender's email address 
    private final static String MESG_FAILED = "MESG_FAILED";   // Message failed flag
    private final static String MESG_SIZE = "MESG_SIZE";       // The size of the message
    private final static String RCPT_VECTOR = "RCPT_VECTOR";   // The message recipients

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

    /**
     * The text string for the SMTP HELO command.
     */
    private final static String COMMAND_HELO = "HELO";

    /**
     * The text string for the SMTP EHLO command.
     */
    private final static String COMMAND_EHLO = "EHLO";

    /**
     * The text string for the SMTP AUTH command.
     */
    private final static String COMMAND_AUTH = "AUTH";

    /**
     * The text string for the SMTP MAIL command.
     */
    private final static String COMMAND_MAIL = "MAIL";

    /**
     * The text string for the SMTP RCPT command.
     */
    private final static String COMMAND_RCPT = "RCPT";

    /**
     * The text string for the SMTP NOOP command.
     */
    private final static String COMMAND_NOOP = "NOOP";

    /**
     * The text string for the SMTP RSET command.
     */
    private final static String COMMAND_RSET = "RSET";

    /**
     * The text string for the SMTP DATA command.
     */
    private final static String COMMAND_DATA = "DATA";

    /**
     * The text string for the SMTP QUIT command.
     */
    private final static String COMMAND_QUIT = "QUIT";

    /**
     * The text string for the SMTP HELP command.
     */
    private final static String COMMAND_HELP = "HELP";

    /**
     * The text string for the SMTP VRFY command.
     */
    private final static String COMMAND_VRFY = "VRFY";

    /**
     * The text string for the SMTP EXPN command.
     */
    private final static String COMMAND_EXPN = "EXPN";

    /**
     * The text string for the SMTP AUTH type PLAIN.
     */
    private final static String AUTH_TYPE_PLAIN = "PLAIN";

    /**
     * The text string for the SMTP AUTH type LOGIN.
     */
    private final static String AUTH_TYPE_LOGIN = "LOGIN";

    /**
     * The text string for the SMTP MAIL command SIZE option.
     */
    private final static String MAIL_OPTION_SIZE = "SIZE";


    /**
     * The TCP/IP socket over which the SMTP 
     * dialogue is occurring.
     */
    private Socket socket;

    /**
     * The incoming stream of bytes coming from the socket.
     */
    private InputStream in;

    /**
     * The writer to which outgoing messages are written.
     */
    private PrintWriter out;

    /**
     * A Reader wrapper for the incoming stream of bytes coming from the socket.
     */
    private BufferedReader inReader;

    /**
     * The remote host name obtained by lookup on the socket.
     */
    private String remoteHost;

    /**
     * The remote IP address of the socket.
     */
    private String remoteIP;

    /**
     * The user name of the authenticated user associated with this SMTP transaction.
     */
    private String authenticatedUser;

    /**
     * The id associated with this particular SMTP interaction.
     */
    private String smtpID;

    /**
     * Whether authentication is required to use
     * this SMTP server.
     */
    private boolean authRequired = false;


    /**
     * Whether the server verifies that the user
     * actually sending an email matches the
     * authentication credentials attached to the
     * SMTP interaction.
     */
    private boolean verifyIdentity = false;


    /**
     * The maximum message size allowed by this SMTP server.  The default
     * value, 0, means no limit.
     */
    private long maxmessagesize = 0;

    /**
     * The number of bytes to read before resetting the connection timeout
     * timer.  Defaults to 20,000 bytes.
     */
    private int lengthReset = 20000;
                                    
    /**
     * The scheduler used to handle timeouts for the SMTP interaction.
     */
    private TimeScheduler scheduler;

    /**
     * The user repository for this server - used to authenticate
     * users.
     */
    private UsersRepository users;

    /**
     * The internal mail server service.
     */
    private MailServer mailServer;

    /**
     * The hash map that holds variables for the SMTP message transfer in progress.
     *
     * This hash map should only be used to store variable set in a particular
     * set of sequential MAIL-RCPT-DATA commands, as described in RFC 2821.  Per
     * connection values should be stored as member variables in this class.
     */
    private HashMap state = new HashMap();

    /**
     * @see org.apache.avalon.framework.component.Composable#compose(ComponentManager)
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
     * @see org.apache.avalon.cornerstone.services.connection.ConnectionHandler#handleConnection(Socket)
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
            remoteIP = socket.getInetAddress().getHostAddress();
            remoteHost = socket.getInetAddress().getHostName();
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
            // Format is:  Sat, 24 Jan 1998 13:16:09 -0500

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
            writeLoggedFlushedResponse(responseString);

            while (parseCommand(readCommandLine())) {
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
                        .append(" (")
                        .append(remoteIP)
                        .append(") closed remotely.");
                getLogger().debug(errorBuffer.toString(), se );
            }
        } catch ( InterruptedIOException iioe ) {
            if (getLogger().isDebugEnabled()) {
                StringBuffer errorBuffer = 
                    new StringBuffer(64)
                        .append("Socket to ")
                        .append(remoteHost)
                        .append(" (")
                        .append(remoteIP)
                        .append(") timeout.");
                getLogger().debug( errorBuffer.toString(), iioe );
            }
        } catch ( IOException ioe ) {
            if (getLogger().isDebugEnabled()) {
                StringBuffer errorBuffer = 
                    new StringBuffer(256)
                            .append("Exception handling socket to ")
                            .append(remoteHost)
                            .append(" (")
                            .append(remoteIP)
                            .append(") : ")
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
     *
     * TODO: Remove this interface from the class and change the mechanism
     */
    public void targetTriggered(final String triggerName) {
        getLogger().error("Connection timeout on socket with " + remoteHost + " (" + remoteIP + ")");
        try {
            out.println("Connection timeout. Closing connection");
            socket.close();
        } catch (IOException e) {
            // Ignored
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
        out.flush();
        logResponseString(responseString);
    }

    /**
     * Reads a line of characters off the command line.
     *
     * @return the trimmed input line
     * @throws IOException if an exception is generated reading in the input characters
     */
    final String readCommandLine() throws IOException {
        return inReader.readLine().trim();
    }

    /**
     * Sets the user name associated with this SMTP interaction.
     *
     * @param userID the user name
     */
    private void setUser(String userID) {
        authenticatedUser = userID;
    }

    /**
     * Returns the user name associated with this SMTP interaction.
     *
     * @return the user name
     */
    private String getUser() {
        return authenticatedUser;
    }

    /**
     * Resets message-specific, but not authenticated user, state.
     *
     */
    private void resetState() {
        state.clear();
    }

    /**
     * This method parses SMTP commands read off the wire in handleConnection.
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
    private boolean parseCommand(String rawCommand) throws Exception {
        String argument = null;
        boolean returnValue = true;
        String command = rawCommand;

        if (rawCommand == null) {
            return false;
        }
        if ((state.get(MESG_FAILED) == null) && (getLogger().isDebugEnabled())) {
            getLogger().debug("Command received: " + command);
        }
        int spaceIndex = command.indexOf(" ");
        if (spaceIndex > 0) {
            argument = command.substring(spaceIndex + 1);
            command = command.substring(0, spaceIndex);
        }
        command = command.toUpperCase(Locale.US);
        if (command.equals(COMMAND_HELO)) {
            doHELO(argument);
        } else if (command.equals(COMMAND_EHLO)) {
            doEHLO(argument);
        } else if (command.equals(COMMAND_AUTH)) {
            doAUTH(argument);
        } else if (command.equals(COMMAND_MAIL)) {
            doMAIL(argument);
        } else if (command.equals(COMMAND_RCPT)) {
            doRCPT(argument);
        } else if (command.equals(COMMAND_NOOP)) {
            doNOOP(argument);
        } else if (command.equals(COMMAND_RSET)) {
            doRSET(argument);
        } else if (command.equals(COMMAND_DATA)) {
            doDATA(argument);
        } else if (command.equals(COMMAND_QUIT)) {
            doQUIT(argument);
            returnValue = false;
        } else if (command.equals(COMMAND_VRFY)) {
            doVRFY(argument);
        } else if (command.equals(COMMAND_EXPN)) {
            doEXPN(argument);
        } else if (command.equals(COMMAND_HELP)) {
            doHELP(argument);
        } else {
            if (state.get(MESG_FAILED) == null)  {
                doUnknownCmd(command, argument);
            }
        }
        return returnValue;
    }

    /**
     * Handler method called upon receipt of a HELO command.
     * Responds with a greeting and informs the client whether
     * client authentication is required.
     *
     * @param argument the argument passed in with the command by the SMTP client
     */
    private void doHELO(String argument) {
        String responseString = null;
        if (argument == null) {
            responseString = "501 Domain address required: " + COMMAND_HELO;
            writeLoggedFlushedResponse(responseString);
        } else {
            resetState();
            state.put(CURRENT_HELO_MODE, COMMAND_HELO);
            StringBuffer responseBuffer = new StringBuffer(256);
            if (authRequired) {
                //This is necessary because we're going to do a multiline response
                responseBuffer.append("250-");
            } else {
                responseBuffer.append("250 ");
            }
            responseBuffer.append(helloName)
                          .append(" Hello ")
                          .append(argument)
                          .append(" (")
                          .append(remoteHost)
                          .append(" [")
                          .append(remoteIP)
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
     * @param argument the argument passed in with the command by the SMTP client
     */
    private void doEHLO(String argument) {
        String responseString = null;
        if (argument == null) {
            responseString = "501 Domain address required: " + COMMAND_EHLO;
            writeLoggedFlushedResponse(responseString);
        } else {
            resetState();
            state.put(CURRENT_HELO_MODE, COMMAND_EHLO);
            // Extension defined in RFC 1870
            if (maxmessagesize > 0) {
                responseString = "250-SIZE " + maxmessagesize;
                writeLoggedResponse(responseString);
            }
            StringBuffer responseBuffer = new StringBuffer(256);
            if (authRequired) {
                //This is necessary because we're going to do a multiline response
                responseBuffer.append("250-");
            } else {
                responseBuffer.append("250 ");
            }
            responseBuffer.append(helloName)
                           .append(" Hello ")
                           .append(argument)
                           .append(" (")
                           .append(remoteHost)
                           .append(" [")
                           .append(remoteIP)
                           .append("])");
            responseString = responseBuffer.toString();
            if (authRequired) {
                writeLoggedResponse(responseString);
                responseString = "250 AUTH LOGIN PLAIN";
            }
            writeLoggedFlushedResponse(responseString);
        }
    }

    /**
     * Handler method called upon receipt of a AUTH command.
     * Handles client authentication to the SMTP server.
     *
     * @param argument the argument passed in with the command by the SMTP client
     */
    private void doAUTH(String argument)
            throws Exception {
        String responseString = null;
        if (getUser() != null) {
            responseString = "503 User has previously authenticated. "
                        + " Further authentication is not required!";
            writeLoggedFlushedResponse(responseString);
        } else if (argument == null) {
            responseString = "501 Usage: AUTH (authentication type) <challenge>";
            writeLoggedFlushedResponse(responseString);
        } else {
            String initialResponse = null;
            if ((argument != null) && (argument.indexOf(" ") > 0)) {
                initialResponse = argument.substring(argument.indexOf(" ") + 1);
                argument = argument.substring(0,argument.indexOf(" "));
            }
            String authType = argument.toUpperCase(Locale.US);
            if (authType.equals(AUTH_TYPE_PLAIN)) {
                doPlainAuth(initialResponse);
                return;
            } else if (authType.equals(AUTH_TYPE_LOGIN)) {
                doLoginAuth(initialResponse);
                return;
            } else {
                doUnknownAuth(authType, initialResponse);
                return;
            }
        }
    }

    /**
     * Carries out the Plain AUTH SASL exchange.
     *
     * @param initialResponse the initial response line passed in with the AUTH command
     */
    private void doPlainAuth(String initialResponse)
            throws IOException {
        String userpass = null, user = null, pass = null, responseString = null;
        if (initialResponse == null) {
            responseString = "334 OK. Continue authentication";
            writeLoggedFlushedResponse(responseString);
            userpass = readCommandLine();
        } else {
            userpass = initialResponse.trim();
        }
        try {
            if (userpass != null) {
                userpass = Base64.decodeAsString(userpass);
            }
            if (userpass != null) {
                StringTokenizer authTokenizer = new StringTokenizer(userpass, "\0");
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
            writeLoggedFlushedResponse(responseString);
        } else if (users.test(user, pass)) {
            setUser(user);
            responseString = "235 Authentication Successful";
            writeLoggedFlushedResponse(responseString);
            getLogger().info("AUTH method PLAIN succeeded");
        } else {
            responseString = "535 Authentication Failed";
            writeLoggedFlushedResponse(responseString);
            getLogger().error("AUTH method PLAIN failed");
        }
        return;
    }

    /**
     * Carries out the Login AUTH SASL exchange.
     *
     * @param initialResponse the initial response line passed in with the AUTH command
     */
    private void doLoginAuth(String initialResponse)
            throws IOException {
        String user = null, pass = null, responseString = null;
        if (initialResponse == null) {
            responseString = "334 VXNlcm5hbWU6"; // base64 encoded "Username:"
            writeLoggedFlushedResponse(responseString);
            user = readCommandLine();
        } else {
            user = initialResponse.trim();
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
        writeLoggedFlushedResponse(responseString);
        pass = readCommandLine();
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
        } else if (users.test(user, pass)) {
            setUser(user);
            responseString = "235 Authentication Successful";
            if (getLogger().isDebugEnabled()) {
                // TODO: Make this string a more useful debug message
                getLogger().debug("AUTH method LOGIN succeeded");
            }
        } else {
            responseString = "535 Authentication Failed";
            // TODO: Make this string a more useful error message
            getLogger().error("AUTH method LOGIN failed");
        }
        writeLoggedFlushedResponse(responseString);
        return;
    }

    /**
     * Handles the case of an unrecognized auth type.
     *
     * @param authType the unknown auth type
     * @param initialResponse the initial response line passed in with the AUTH command
     */
    private void doUnknownAuth(String authType, String initialResponse) {
        String responseString = "504 Unrecognized Authentication Type";
        writeLoggedFlushedResponse(responseString);
        if (getLogger().isErrorEnabled()) {
            StringBuffer errorBuffer =
                new StringBuffer(128)
                    .append("AUTH method ")
                        .append(authType)
                        .append(" is an unrecognized authentication type");
            getLogger().error(errorBuffer.toString());
        }
        return;
    }

    /**
     * Handler method called upon receipt of a MAIL command.
     * Sets up handler to deliver mail as the stated sender.
     *
     * @param argument the argument passed in with the command by the SMTP client
     */
    private void doMAIL(String argument) {
        String responseString = null;

        String sender = null;
        if ((argument != null) && (argument.indexOf(":") > 0)) {
            int colonIndex = argument.indexOf(":");
            sender = argument.substring(colonIndex + 1);
            argument = argument.substring(0, colonIndex);
        }
        if (state.containsKey(SENDER)) {
            responseString = "503 Sender already specified";
            writeLoggedFlushedResponse(responseString);
        } else if (argument == null || !argument.toUpperCase(Locale.US).equals("FROM")
                   || sender == null) {
            responseString = "501 Usage: MAIL FROM:<sender>";
            writeLoggedFlushedResponse(responseString);
        } else {
            sender = sender.trim();
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
                        mailOptionName = mailOption.substring(0, equalIndex).toUpperCase(Locale.US);
                        mailOptionValue = mailOption.substring(equalIndex + 1);
                    }

                    // Handle the SIZE extension keyword

                    if (mailOptionName.startsWith(MAIL_OPTION_SIZE)) {
                        if (!(doMailSize(mailOptionValue))) {
                            return;
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
                responseString = "501 Syntax error in MAIL command";
                writeLoggedFlushedResponse(responseString);
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
                    responseString = "501 Syntax error in sender address";
                    writeLoggedFlushedResponse(responseString);
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
            writeLoggedFlushedResponse(responseString);
        }
    }

    /**
     * Handles the SIZE MAIL option.
     *
     * @param mailOptionValue the option string passed in with the SIZE option
     * @returns true if further options should be processed, false otherwise
     */
    private boolean doMailSize(String mailOptionValue) {
        int size = 0;
        try {
            size = Integer.parseInt(mailOptionValue);
        } catch (NumberFormatException pe) {
            // This is a malformed option value.  We return an error
            String responseString = "501 Syntactically incorrect value for SIZE parameter";
            writeLoggedFlushedResponse(responseString);
            getLogger().error("Rejected syntactically incorrect value for SIZE parameter.");
            return false;
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
            String responseString = "552 Message size exceeds fixed maximum message size";
            writeLoggedFlushedResponse(responseString);
            StringBuffer errorBuffer =
                new StringBuffer(256)
                    .append("Rejected message from ")
                    .append(state.get(SENDER).toString())
                    .append(" from host ")
                    .append(remoteHost)
                    .append(" (")
                    .append(remoteIP)
                    .append(") of size ")
                    .append(size)
                    .append(" exceeding system maximum message size of ")
                    .append(maxmessagesize)
                    .append("based on SIZE option.");
            getLogger().error(errorBuffer.toString());
            return false;
        } else {
            // put the message size in the message state so it can be used
            // later to restrict messages for user quotas, etc.
            state.put(MESG_SIZE, new Integer(size));
        }
        return true;
    }


    /**
     * Handler method called upon receipt of a RCPT command.
     * Reads recipient.  Does some connection validation.
     *
     * @param argument the argument passed in with the command by the SMTP client
     */
    private void doRCPT(String argument) {
        String responseString = null;

        String recipient = null;
        if ((argument != null) && (argument.indexOf(":") > 0)) {
            int colonIndex = argument.indexOf(":");
            recipient = argument.substring(colonIndex + 1);
            argument = argument.substring(0, colonIndex);
        }
        if (!state.containsKey(SENDER)) {
            responseString = "503 Need MAIL before RCPT";
            writeLoggedFlushedResponse(responseString);
        } else if (argument == null || !argument.toUpperCase(Locale.US).equals("TO")
                   || recipient == null) {
            responseString = "501 Usage: RCPT TO:<recipient>";
            writeLoggedFlushedResponse(responseString);
        } else {
            Collection rcptColl = (Collection) state.get(RCPT_VECTOR);
            if (rcptColl == null) {
                rcptColl = new Vector();
            }
            recipient = recipient.trim();
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
                        rcptOptionName = rcptOption.substring(0, equalIndex).toUpperCase(Locale.US);
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
                writeLoggedFlushedResponse(responseString);
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
                responseString = "501 Syntax error in recipient address";
                writeLoggedFlushedResponse(responseString);

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
                if (getUser() == null) {
                    String toDomain = recipientAddress.getHost();
                    if (!mailServer.isLocalServer(toDomain)) {
                        responseString = "530 Authentication Required";
                        writeLoggedFlushedResponse(responseString);
                        getLogger().error("Rejected message - authentication is required for mail request");
                        return;
                    }
                } else {
                    // Identity verification checking
                    if (verifyIdentity) {
                        String authUser = (getUser()).toLowerCase(Locale.US);
                        MailAddress senderAddress = (MailAddress) state.get(SENDER);
                        boolean domainExists = false;

                        if ((!authUser.equals(senderAddress.getUser())) ||
                            (!mailServer.isLocalServer(senderAddress.getHost()))) {
                            responseString = "503 Incorrect Authentication for Specified Email Address";
                            writeLoggedFlushedResponse(responseString);
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
            writeLoggedFlushedResponse(responseString);
        }
    }

    /**
     * Handler method called upon receipt of a NOOP command.
     * Just sends back an OK and logs the command.
     *
     * @param argument the argument passed in with the command by the SMTP client
     */
    private void doNOOP(String argument) {
        String responseString = "250 OK";
        writeLoggedFlushedResponse(responseString);
    }

    /**
     * Handler method called upon receipt of a RSET command.
     * Resets message-specific, but not authenticated user, state.
     *
     * @param argument the argument passed in with the command by the SMTP client
     */
    private void doRSET(String argument) {
        String responseString = "";
        if ((argument == null) || (argument.length() == 0)) {
            resetState();
            responseString = "250 OK";
        } else {
            responseString = "500 Unexpected argument provided with RSET command";
        }
        writeLoggedFlushedResponse(responseString);
    }

    /**
     * Handler method called upon receipt of a DATA command.
     * Reads in message data, creates header, and delivers to
     * mail server service for delivery.
     *
     * @param argument the argument passed in with the command by the SMTP client
     */
    private void doDATA(String argument) {
        String responseString = null;
        if ((argument != null) && (argument.length() > 0)) {
            responseString = "500 Unexpected argument provided with DATA command";
            writeLoggedFlushedResponse(responseString);
        }
        if (!state.containsKey(SENDER)) {
            responseString = "503 No sender specified";
            writeLoggedFlushedResponse(responseString);
        } else if (!state.containsKey(RCPT_VECTOR)) {
            responseString = "503 No recipients specified";
            writeLoggedFlushedResponse(responseString);
        } else {
            responseString = "354 Ok Send data ending with <CRLF>.<CRLF>";
            writeLoggedFlushedResponse(responseString);
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
                // Removes the dot stuffing
                msgIn = new SMTPInputStream(msgIn);
                // Parse out the message headers
                MailHeaders headers = new MailHeaders(msgIn);
                // If headers do not contains minimum REQUIRED headers fields,
                // add them
                if (!headers.isSet(RFC2822Headers.DATE)) {
                    headers.setHeader(RFC2822Headers.DATE, rfc822DateFormat.format(new Date()));
                }
                if (!headers.isSet(RFC2822Headers.FROM) && state.get(SENDER) != null) {
                    headers.setHeader(RFC2822Headers.FROM, state.get(SENDER).toString());
                }
                // Determine the Return-Path
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
                // We will rebuild the header object to put Return-Path and our
                // Received header at the top
                Enumeration headerLines = headers.getAllHeaderLines();
                headers = new MailHeaders();
                // Put the Return-Path first
                headers.addHeaderLine(RFC2822Headers.RETURN_PATH + ": " + returnPath);
                // Put our Received header next
                StringBuffer headerLineBuffer = 
                    new StringBuffer(128)
                            .append(RFC2822Headers.RECEIVED + ": from ")
                            .append(remoteHost)
                            .append(" ([")
                            .append(remoteIP)
                            .append("])");

                headers.addHeaderLine(headerLineBuffer.toString());

                headerLineBuffer = 
                    new StringBuffer(256)
                            .append("          by ")
                            .append(this.helloName)
                            .append(" (")
                            .append(SOFTWARE_TYPE)
                            .append(") with SMTP ID ")
                            .append(smtpID);

                if (((Collection) state.get(RCPT_VECTOR)).size() == 1) {
                    // Only indicate a recipient if they're the only recipient
                    // (prevents email address harvesting and large headers in
                    //  bulk email)
                    headers.addHeaderLine(headerLineBuffer.toString());
                    headerLineBuffer = 
                        new StringBuffer(256)
                                .append("          for <")
                                .append(((Vector) state.get(RCPT_VECTOR)).get(0).toString())
                                .append(">;");
                    headers.addHeaderLine(headerLineBuffer.toString());
                } else {
                    // Put the ; on the end of the 'by' line
                    headerLineBuffer.append(";");
                    headers.addHeaderLine(headerLineBuffer.toString());
                }
                headers.addHeaderLine("          " + rfc822DateFormat.format(new Date()));

                // Add all the original message headers back in next
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
                // If the message size limit has been set, we'll
                // call mail.getSize() to force the message to be
                // loaded. Need to do this to enforce the size limit
                if (maxmessagesize > 0) {
                    mail.getMessageSize();
                }
                mail.setRemoteHost(remoteHost);
                mail.setRemoteAddr(remoteIP);
                mailServer.sendMail(mail);
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("Successfully sent mail to spool: " + mail.getName());
                }
            } catch (MessagingException me) {
                // Grab any exception attached to this one.
                Exception e = me.getNextException();
                // If there was an attached exception, and it's a
                // MessageSizeException
                if (e != null && e instanceof MessageSizeException) {
                    // Add an item to the state to suppress
                    // logging of extra lines of data
                    // that are sent after the size limit has
                    // been hit.
                    state.put(MESG_FAILED, Boolean.TRUE);
                    // then let the client know that the size
                    // limit has been hit.
                    responseString = "552 Error processing message: "
                                + e.getMessage();
                    StringBuffer errorBuffer =
                        new StringBuffer(256)
                            .append("Rejected message from ")
                            .append(state.get(SENDER).toString())
                            .append(" from host ")
                            .append(remoteHost)
                            .append(" (")
                            .append(remoteIP)
                            .append(") exceeding system maximum message size of ")
                            .append(maxmessagesize);
                    getLogger().error(errorBuffer.toString());
                } else {
                    responseString = "451 Error processing message: "
                                + me.getMessage();
                    getLogger().error("Unknown error occurred while processing DATA.", me);
                }
                writeLoggedFlushedResponse(responseString);
                return;
            }
            resetState();
            responseString = "250 Message received";
            writeLoggedFlushedResponse(responseString);
        }
    }

    /**
     * Handler method called upon receipt of a VRFY command.
     * This method informs the client that the command is
     * not implemented.
     *
     * @param argument the argument passed in with the command by the SMTP client
     */
    private void doVRFY(String argument) {

        String responseString = "502 VRFY is not supported";
        writeLoggedFlushedResponse(responseString);
    }

    /**
     * Handler method called upon receipt of a EXPN command.
     * This method informs the client that the command is
     * not implemented.
     *
     * @param argument the argument passed in with the command by the SMTP client
     */
    private void doEXPN(String argument) {

        String responseString = "502 EXPN is not supported";
        writeLoggedFlushedResponse(responseString);
    }

    /**
     * Handler method called upon receipt of a HELP command.
     * This method informs the client that the command is
     * not implemented.
     *
     * @param argument the argument passed in with the command by the SMTP client
     */
    private void doHELP(String argument) {

        String responseString = "502 HELP is not supported";
        writeLoggedFlushedResponse(responseString);
    }

    /**
     * Handler method called upon receipt of a QUIT command.
     * This method informs the client that the connection is
     * closing.
     *
     * @param argument the argument passed in with the command by the SMTP client
     */
    private void doQUIT(String argument) {

        String responseString = "";
        if ((argument == null) || (argument.length() == 0)) {
            StringBuffer responseBuffer =
                new StringBuffer(128)
                        .append("221 ")
                        .append(helloName)
                        .append(" Service closing transmission channel");
            responseString = responseBuffer.toString();
        } else {
            responseString = "500 Unexpected argument provided with QUIT command";
        }
        writeLoggedFlushedResponse(responseString);
    }

    /**
     * Handler method called upon receipt of an unrecognized command.
     * Returns an error response and logs the command.
     *
     * @param command the command parsed by the SMTP client
     * @param argument the argument passed in with the command by the SMTP client
     */
    private void doUnknownCmd(String command, String argument) {
        StringBuffer responseBuffer =
            new StringBuffer(128)
                    .append("500 ")
                    .append(helloName)
                    .append(" Syntax error, command unrecognized: ")
                    .append(command);
        String responseString = responseBuffer.toString();
        writeLoggedFlushedResponse(responseString);
    }
}
