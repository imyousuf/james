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

package org.apache.james.remotemanager;

import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.excalibur.pool.Poolable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.Constants;
import org.apache.james.services.*;
import org.apache.james.userrepository.DefaultUser;
import org.apache.james.util.watchdog.Watchdog;
import org.apache.james.util.watchdog.WatchdogTarget;
import org.apache.mailet.MailAddress;

import javax.mail.internet.ParseException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * Provides a really rude network interface to administer James.
 * Allow to add accounts.
 * TODO: -improve protocol
 *       -much more...
 *
 * @version $Revision: 1.21.4.6 $
 *
 */
public class RemoteManagerHandler
    extends AbstractLogEnabled
    implements ConnectionHandler, Poolable {

    /**
     * The text string for the ADDUSER command
     */
    private static final String COMMAND_ADDUSER = "ADDUSER";

    /**
     * The text string for the SETPASSWORD command
     */
    private static final String COMMAND_SETPASSWORD = "SETPASSWORD";

    /**
     * The text string for the DELUSER command
     */
    private static final String COMMAND_DELUSER = "DELUSER";

    /**
     * The text string for the LISTUSERS command
     */
    private static final String COMMAND_LISTUSERS = "LISTUSERS";

    /**
     * The text string for the COUNTUSERS command
     */
    private static final String COMMAND_COUNTUSERS = "COUNTUSERS";

    /**
     * The text string for the VERIFY command
     */
    private static final String COMMAND_VERIFY = "VERIFY";

    /**
     * The text string for the HELP command
     */
    private static final String COMMAND_HELP = "HELP";

    /**
     * The text string for the SETFORWARDING command
     */
    private static final String COMMAND_SETFORWARDING = "SETFORWARDING";

    /**
     * The text string for the SHOWFORWARDING command
     */
    private static final String COMMAND_SHOWFORWARDING = "SHOWFORWARDING";

    /**
     * The text string for the UNSETFORWARDING command
     */
    private static final String COMMAND_UNSETFORWARDING = "UNSETFORWARDING";

    /**
     * The text string for the SETALIAS command
     */
    private static final String COMMAND_SETALIAS = "SETALIAS";

    /**
     * The text string for the SHOWALIAS command
     */
    private static final String COMMAND_SHOWALIAS = "SHOWALIAS";

    /**
     * The text string for the UNSETALIAS command
     */
    private static final String COMMAND_UNSETALIAS = "UNSETALIAS";

    /**
     * The text string for the USER command
     */
    private static final String COMMAND_USER = "USER";

    /**
     * The text string for the QUIT command
     */
    private static final String COMMAND_QUIT = "QUIT";

    /**
     * The text string for the SHUTDOWN command
     */
    private static final String COMMAND_SHUTDOWN = "SHUTDOWN";

    /**
     * The per-service configuration data that applies to all handlers
     */
    private RemoteManagerHandlerConfigurationData theConfigData;

    /**
     * The current UsersRepository being managed/viewed/modified
     */
    private UsersRepository users;

    /**
     * Whether the local users repository should be used to store new
     * users.
     */
    private boolean inLocalUsers = true;

    /**
     * The reader associated with incoming commands.
     */
    private BufferedReader in;

    /**
     * The writer to which outgoing messages are written.
     */
    private PrintWriter out;

    /**
     * The thread executing this handler
     */
    private Thread handlerThread;

    /**
     * The TCP/IP socket over which the RemoteManager interaction
     * is occurring
     */
    private Socket socket;

    /**
     * The watchdog being used by this handler to deal with idle timeouts.
     */
    private Watchdog theWatchdog;

    /**
     * The watchdog target that idles out this handler.
     */
    private WatchdogTarget theWatchdogTarget = new RemoteManagerWatchdogTarget();

    /**
     * Set the configuration data for the handler.
     *
     * @param theData the configuration data
     */
    void setConfigurationData(RemoteManagerHandlerConfigurationData theData) {
        theConfigData = theData;

        // Reset the users repository to the default.
        users = theConfigData.getUsersRepository();
        inLocalUsers = true;
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
            getLogger().error("Remote Manager Connection has idled out.");
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
    public void handleConnection( final Socket connection )
        throws IOException {

        socket = connection;
        String remoteIP = socket.getInetAddress().getHostAddress();
        String remoteHost = socket.getInetAddress().getHostName();

        synchronized (this) {
            handlerThread = Thread.currentThread();
        }

        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "ASCII"), 512);
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()), 512), false);
            if (getLogger().isInfoEnabled()) {
                StringBuffer infoBuffer =
                    new StringBuffer(128)
                            .append("Access from ")
                            .append(remoteHost)
                            .append("(")
                            .append(remoteIP)
                            .append(")");
                getLogger().info( infoBuffer.toString() );
            }
            writeLoggedResponse("JAMES Remote Administration Tool " + Constants.SOFTWARE_VERSION );
            writeLoggedResponse("Please enter your login and password");
            String login = null;
            String password = null;
            do {
                if (login != null) {
                    final String message = "Login failed for " + login;
                    writeLoggedFlushedResponse(message);
                }
                writeLoggedFlushedResponse("Login id:");
                login = in.readLine().trim();
                writeLoggedFlushedResponse("Password:");
                password = in.readLine().trim();
            } while (!password.equals(theConfigData.getAdministrativeAccountData().get(login)) || password.length() == 0);

            StringBuffer messageBuffer =
                new StringBuffer(64)
                        .append("Welcome ")
                        .append(login)
                        .append(". HELP for a list of commands");
            out.println( messageBuffer.toString() );
            out.flush();
            if (getLogger().isInfoEnabled()) {
                StringBuffer infoBuffer =
                    new StringBuffer(128)
                            .append("Login for ")
                            .append(login)
                            .append(" successful");
                getLogger().info(infoBuffer.toString());
            }

            try {
                theWatchdog.start();
                while (parseCommand(in.readLine())) {
                    theWatchdog.reset();
                }
                theWatchdog.stop();
            } catch (IOException ioe) {
                //We can cleanly ignore this as it's probably a socket timeout
            } catch (Throwable thr) {
                System.out.println("Exception: " + thr.getMessage());
                getLogger().error("Encountered exception in handling the remote manager connection.", thr);
            }
            StringBuffer infoBuffer =
                new StringBuffer(64)
                        .append("Logout for ")
                        .append(login)
                        .append(".");
            getLogger().info(infoBuffer.toString());

        } catch ( final IOException e ) {
            out.println("Error. Closing connection");
            out.flush();
            if (getLogger().isErrorEnabled()) {
                StringBuffer exceptionBuffer =
                    new StringBuffer(128)
                            .append("Exception during connection from ")
                            .append(remoteHost)
                            .append(" (")
                            .append(remoteIP)
                            .append("): ")
                            .append(e.getMessage());
                getLogger().error(exceptionBuffer.toString());
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
            if (theWatchdog instanceof Disposable) {
                ((Disposable)theWatchdog).dispose();
            }
            theWatchdog = null;
        }

        in = null;
        out = null;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            if (getLogger().isErrorEnabled()) {
                getLogger().error("Exception closing socket: "
                                  + e.getMessage());
            }
        } finally {
            socket = null;
        }

        synchronized (this) {
            handlerThread = null;
        }

        // Reset user repository
        users = theConfigData.getUsersRepository();
        inLocalUsers = true;

        // Clear config data
        theConfigData = null;
    }

    /**
     * <p>This method parses and processes RemoteManager commands read off the
     * wire in handleConnection.  It returns true if expecting additional
     * commands, false otherwise.</p>
     *
     * @param command the raw command string passed in over the socket
     *
     * @return whether additional commands are expected.
     */
    private boolean parseCommand( String rawCommand ) {
        if (rawCommand == null) {
            return false;
        }
        String command = rawCommand.trim();
        String argument = null;
        int breakIndex = command.indexOf(" ");
        if (breakIndex > 0) {
            argument = command.substring(breakIndex + 1);
            command = command.substring(0, breakIndex);
        }
        command = command.toUpperCase(Locale.US);
        String argument1 = null;
        if (command.equals(COMMAND_ADDUSER)) {
            doADDUSER(argument);
        } else if (command.equals(COMMAND_SETPASSWORD)) {
            return doSETPASSWORD(argument);
        } else if (command.equals(COMMAND_DELUSER)) {
            return doDELUSER(argument);
        } else if (command.equals(COMMAND_LISTUSERS)) {
            return doLISTUSERS(argument);
        } else if (command.equals(COMMAND_COUNTUSERS)) {
            return doCOUNTUSERS(argument);
        } else if (command.equals(COMMAND_VERIFY)) {
            return doVERIFY(argument);
        } else if (command.equals(COMMAND_HELP)) {
            return doHELP(argument);
        } else if (command.equals(COMMAND_SETALIAS)) {
            return doSETALIAS(argument);
        } else if (command.equals(COMMAND_SETFORWARDING)) {
            return doSETFORWARDING(argument);
        } else if (command.equals(COMMAND_SHOWALIAS)) {
            return doSHOWALIAS(argument);
        } else if (command.equals(COMMAND_SHOWFORWARDING)) {
            return doSHOWFORWARDING(argument);
        } else if (command.equals(COMMAND_UNSETALIAS)) {
            return doUNSETALIAS(argument);
        } else if (command.equals(COMMAND_UNSETFORWARDING)) {
            return doUNSETFORWARDING(argument);
        } else if (command.equals(COMMAND_USER)) {
            return doUSER(argument);
        } else if (command.equals(COMMAND_QUIT)) {
            return doQUIT(argument);
        } else if (command.equals(COMMAND_SHUTDOWN)) {
            return doSHUTDOWN(argument);
        } else {
            return doUnknownCommand(rawCommand);
        }
        return true;
    }

    /**
     * Handler method called upon receipt of an ADDUSER command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doADDUSER(String argument) {
        int breakIndex = -1;
        if ((argument == null) ||
            (argument.equals("")) ||
            ((breakIndex = argument.indexOf(" ")) < 0)) {
            writeLoggedFlushedResponse("Usage: adduser [username] [password]");
            return true;
        }
        String username = argument.substring(0,breakIndex);
        String passwd = argument.substring(breakIndex + 1);
        if (username.equals("") || passwd.equals("")) {
            writeLoggedFlushedResponse("Usage: adduser [username] [password]");
            return true;
        }

        boolean success = false;
        if (users.contains(username)) {
            StringBuffer responseBuffer =
                new StringBuffer(64)
                        .append("User ")
                        .append(username)
                        .append(" already exists");
            String response = responseBuffer.toString();
            writeLoggedResponse(response);
        } else if ( inLocalUsers ) {
            // TODO: Why does the LocalUsers repository get treated differently?
            //       What exactly is the LocalUsers repository?
            success = theConfigData.getMailServer().addUser(username, passwd);
        } else {
            DefaultUser user = new DefaultUser(username, "SHA");
            user.setPassword(passwd);
            success = users.addUser(user);
        }
        if ( success ) {
            StringBuffer responseBuffer =
                new StringBuffer(64)
                        .append("User ")
                        .append(username)
                        .append(" added");
            String response = responseBuffer.toString();
            out.println(response);
            getLogger().info(response);
        } else {
            out.println("Error adding user " + username);
            getLogger().error("Error adding user " + username);
        }
        out.flush();
        return true;
    }

    /**
     * Handler method called upon receipt of an SETPASSWORD command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doSETPASSWORD(String argument) {

        int breakIndex = -1;
        if ((argument == null) ||
            (argument.equals("")) ||
            ((breakIndex = argument.indexOf(" ")) < 0)) {
            writeLoggedFlushedResponse("Usage: setpassword [username] [password]");
            return true;
        }
        String username = argument.substring(0,breakIndex);
        String passwd = argument.substring(breakIndex + 1);

        if (username.equals("") || passwd.equals("")) {
            writeLoggedFlushedResponse("Usage: adduser [username] [password]");
            return true;
        }
        User user = users.getUserByName(username);
        if (user == null) {
            writeLoggedFlushedResponse("No such user " + username);
            return true;
        }
        boolean success = user.setPassword(passwd);
        if (success) {
            users.updateUser(user);
            StringBuffer responseBuffer =
                new StringBuffer(64)
                        .append("Password for ")
                        .append(username)
                        .append(" reset");
            String response = responseBuffer.toString();
            out.println(response);
            getLogger().info(response);
        } else {
            out.println("Error resetting password");
            getLogger().error("Error resetting password");
        }
        out.flush();
        return true;
    }

    /**
     * Handler method called upon receipt of an DELUSER command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doDELUSER(String argument) {
        String user = argument;
        if ((user == null) || (user.equals(""))) {
            writeLoggedFlushedResponse("Usage: deluser [username]");
            return true;
        }
        if (users.contains(user)) {
            try {
                users.removeUser(user);
                StringBuffer responseBuffer =
                                             new StringBuffer(64)
                                             .append("User ")
                                             .append(user)
                                             .append(" deleted");
                String response = responseBuffer.toString();
                out.println(response);
                getLogger().info(response);
            } catch (Exception e) {
                StringBuffer exceptionBuffer =
                                              new StringBuffer(128)
                                              .append("Error deleting user ")
                                              .append(user)
                                              .append(" : ")
                                              .append(e.getMessage());
                String exception = exceptionBuffer.toString();
                out.println(exception);
                getLogger().error(exception);
            }
        } else {
            StringBuffer responseBuffer =
                                         new StringBuffer(64)
                                         .append("User ")
                                         .append(user)
                                         .append(" doesn't exist");
            String response = responseBuffer.toString();
            out.println(response);
        }
        out.flush();
        return true;
    }

    /**
     * Handler method called upon receipt of an LISTUSERS command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doLISTUSERS(String argument) {
        writeLoggedResponse("Existing accounts " + users.countUsers());
        for (Iterator it = users.list(); it.hasNext();) {
           writeLoggedResponse("user: " + (String) it.next());
        }
        out.flush();
        return true;
    }

    /**
     * Handler method called upon receipt of an COUNTUSERS command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doCOUNTUSERS(String argument) {
        writeLoggedFlushedResponse("Existing accounts " + users.countUsers());
        return true;
    }

    /**
     * Handler method called upon receipt of an VERIFY command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doVERIFY(String argument) {
        String user = argument;
        if (user == null || user.equals("")) {
            writeLoggedFlushedResponse("Usage: verify [username]");
            return true;
        }
        if (users.contains(user)) {
            StringBuffer responseBuffer =
                new StringBuffer(64)
                        .append("User ")
                        .append(user)
                        .append(" exists");
            String response = responseBuffer.toString();
            writeLoggedResponse(response);
        } else {
            StringBuffer responseBuffer =
                new StringBuffer(64)
                        .append("User ")
                        .append(user)
                        .append(" does not exist");
            String response = responseBuffer.toString();
            writeLoggedResponse(response);
        }
        out.flush();
        return true;
    }

    /**
     * Handler method called upon receipt of an HELP command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doHELP(String argument) {
        out.println("Currently implemented commands:");
        out.println("help                                    display this help");
        out.println("listusers                               display existing accounts");
        out.println("countusers                              display the number of existing accounts");
        out.println("adduser [username] [password]           add a new user");
        out.println("verify [username]                       verify if specified user exist");
        out.println("deluser [username]                      delete existing user");
        out.println("setpassword [username] [password]       sets a user's password");
        out.println("setalias [user] [alias]                 locally forwards all email for 'user' to 'alias'");
        out.println("showalias [username]                    shows a user's current email alias");
        out.println("unsetalias [user]                       unsets an alias for 'user'");
        out.println("setforwarding [username] [emailaddress] forwards a user's email to another email address");
        out.println("showforwarding [username]               shows a user's current email forwarding");
        out.println("unsetforwarding [username]              removes a forward");
        out.println("user [repositoryname]                   change to another user repository");
        out.println("shutdown                                kills the current JVM (convenient when James is run as a daemon)");
        out.println("quit                                    close connection");
        out.flush();
        return true;
    }

    /**
     * Handler method called upon receipt of an SETALIAS command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doSETALIAS(String argument) {
        int breakIndex = -1;
        if ((argument == null) ||
            (argument.equals("")) ||
            ((breakIndex = argument.indexOf(" ")) < 0)) {
            writeLoggedFlushedResponse("Usage: setalias [username] [emailaddress]");
            return true;
        }
        String username = argument.substring(0,breakIndex);
        String alias = argument.substring(breakIndex + 1);
        if (username.equals("") || alias.equals("")) {
            writeLoggedFlushedResponse("Usage: setalias [username] [alias]");
            return true;
        }
        JamesUser user = (JamesUser) users.getUserByName(username);
        if (user == null) {
            writeLoggedFlushedResponse("No such user " + username);
            return true;
        }
        JamesUser aliasUser = (JamesUser) users.getUserByName(alias);
        if (aliasUser == null) {
            writeLoggedFlushedResponse("Alias unknown to server - create that user first.");
            return true;
        }

        boolean success = user.setAlias(alias);
        if (success) {
            user.setAliasing(true);
            users.updateUser(user);
            StringBuffer responseBuffer =
                new StringBuffer(64)
                        .append("Alias for ")
                        .append(username)
                        .append(" set to:")
                        .append(alias);
            String response = responseBuffer.toString();
            out.println(response);
            getLogger().info(response);
        } else {
            out.println("Error setting alias");
            getLogger().error("Error setting alias");
        }
        out.flush();
        return true;
    }

    /**
     * Handler method called upon receipt of an SETFORWARDING command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doSETFORWARDING(String argument) {
        int breakIndex = -1;
        if ((argument == null) ||
            (argument.equals("")) ||
            ((breakIndex = argument.indexOf(" ")) < 0)) {
            writeLoggedFlushedResponse("Usage: setforwarding [username] [emailaddress]");
            return true;
        }
        String username = argument.substring(0,breakIndex);
        String forward = argument.substring(breakIndex + 1);
        if (username.equals("") || forward.equals("")) {
           writeLoggedFlushedResponse("Usage: setforwarding [username] [emailaddress]");
           return true;
        }
        // Verify user exists
        User baseuser = users.getUserByName(username);
        if (baseuser == null) {
            writeLoggedFlushedResponse("No such user " + username);
            return true;
        } else if (! (baseuser instanceof JamesUser ) ) {
            writeLoggedFlushedResponse("Can't set forwarding for this user type.");
            return true;
        }
        JamesUser user = (JamesUser)baseuser;
        // Verify acceptable email address
        MailAddress forwardAddr;
        try {
             forwardAddr = new MailAddress(forward);
        } catch(ParseException pe) {
            writeLoggedResponse("Parse exception with that email address: " + pe.getMessage());
            writeLoggedFlushedResponse("Forwarding address not added for " + username);
            return true;
        }

        boolean success = user.setForwardingDestination(forwardAddr);
        if (success) {
            user.setForwarding(true);
            users.updateUser(user);
            StringBuffer responseBuffer =
                new StringBuffer(64)
                        .append("Forwarding destination for ")
                        .append(username)
                        .append(" set to:")
                        .append(forwardAddr.toString());
            String response = responseBuffer.toString();
            out.println(response);
            getLogger().info(response);
        } else {
            out.println("Error setting forwarding");
            getLogger().error("Error setting forwarding");
        }
        out.flush();
        return true;
    }

    /**
     * Handler method called upon receipt of an SHOWALIAS command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the user name
     */
    private boolean doSHOWALIAS(String username) {
        if ( username == null || username.equals("") ) {
            writeLoggedFlushedResponse("Usage: showalias [username]");
            return true;
        }

        JamesUser user = (JamesUser)users.getUserByName(username);
        if ( user == null ) {
            writeLoggedFlushedResponse("No such user " + username);
            return true;
        }

        if ( !user.getAliasing() ) {
            writeLoggedFlushedResponse("User " + username + " does not currently have an alias");
            return true;
        }

        String alias = user.getAlias();

        if ( alias == null || alias.equals("") ) {    //  defensive programming -- neither should occur
            String errmsg = "For user " + username + ", the system indicates that aliasing is set but no alias was found";
            out.println(errmsg);
            getLogger().error(errmsg);
            return true;
        }

        writeLoggedFlushedResponse("Current alias for " + username + " is: " + alias);
        return true;
    }

    /**
     * Handler method called upon receipt of an SHOWFORWARDING command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the user name
     */
    private boolean doSHOWFORWARDING(String username) {
        if ( username == null || username.equals("") ) {
            writeLoggedFlushedResponse("Usage: showforwarding [username]");
            return true;
        }

        JamesUser user = (JamesUser)users.getUserByName(username);
        if ( user == null ) {
            writeLoggedFlushedResponse("No such user " + username);
            return true;
        }

        if ( !user.getForwarding() ) {
            writeLoggedFlushedResponse("User " + username + " is not currently being forwarded");
            return true;
        }

        MailAddress fwdAddr = user.getForwardingDestination();

        if ( fwdAddr == null ) {    //  defensive programming -- should not occur
            String errmsg = "For user " + username + ", the system indicates that forwarding is set but no forwarding destination was found";
            out.println(errmsg);
            getLogger().error(errmsg);
            return true;
        }

        writeLoggedFlushedResponse("Current forwarding destination for " + username + " is: " + fwdAddr);
        return true;
    }

    /**
     * Handler method called upon receipt of an UNSETALIAS command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doUNSETALIAS(String argument) {
        if ((argument == null) || (argument.equals(""))) {
            writeLoggedFlushedResponse("Usage: unsetalias [username]");
            return true;
        }
        String username = argument;
        JamesUser user = (JamesUser) users.getUserByName(username);
        if (user == null) {
            writeLoggedResponse("No such user " + username);
        } else if (user.getAliasing()){
            user.setAliasing(false);
            users.updateUser(user);
            StringBuffer responseBuffer =
                new StringBuffer(64)
                        .append("Alias for ")
                        .append(username)
                        .append(" unset");
            String response = responseBuffer.toString();
            out.println(response);
            getLogger().info(response);
        } else {
            writeLoggedResponse("Aliasing not active for" + username);
        }
        out.flush();
        return true;
    }

    /**
     * Handler method called upon receipt of an UNSETFORWARDING command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doUNSETFORWARDING(String argument) {
        if ((argument == null) || (argument.equals(""))) {
            writeLoggedFlushedResponse("Usage: unsetforwarding [username]");
            return true;
        }
        String username = argument;
        JamesUser user = (JamesUser) users.getUserByName(username);
        if (user == null) {
            writeLoggedFlushedResponse("No such user " + username);
        } else if (user.getForwarding()){
            user.setForwarding(false);
            users.updateUser(user);
            StringBuffer responseBuffer =
                new StringBuffer(64)
                        .append("Forward for ")
                        .append(username)
                        .append(" unset");
            String response = responseBuffer.toString();
            out.println(response);
            out.flush();
            getLogger().info(response);
        } else {
            writeLoggedFlushedResponse("Forwarding not active for" + username);
        }
        return true;
    }

    /**
     * Handler method called upon receipt of a USER command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doUSER(String argument) {
        if (argument == null || argument.equals("")) {
            writeLoggedFlushedResponse("Usage: user [repositoryName]");
            return true;
        }
        String repositoryName = argument.toLowerCase(Locale.US);
        UsersRepository repos = theConfigData.getUserStore().getRepository(repositoryName);
        if ( repos == null ) {
            writeLoggedFlushedResponse("No such repository: " + repositoryName);
        } else {
            users = repos;
            StringBuffer responseBuffer =
                new StringBuffer(64)
                        .append("Changed to repository '")
                        .append(repositoryName)
                        .append("'.");
            writeLoggedFlushedResponse(responseBuffer.toString());
            if ( repositoryName.equals("localusers") ) {
                inLocalUsers = true;
            } else {
                inLocalUsers = false;
            }
        }
        return true;
    }

    /**
     * Handler method called upon receipt of a QUIT command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doQUIT(String argument) {
        writeLoggedFlushedResponse("Bye");
        return false;
    }

    /**
     * Handler method called upon receipt of a SHUTDOWN command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doSHUTDOWN(String argument) {
        writeLoggedFlushedResponse("Shutting down, bye bye");
        System.exit(0);
        return false;
    }

    /**
     * Handler method called upon receipt of an unrecognized command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the unknown command
     */
    private boolean doUnknownCommand(String argument) {
        writeLoggedFlushedResponse("Unknown command " + argument);
        return true;
    }

    /**
     * This method logs at a "DEBUG" level the response string that
     * was sent to the RemoteManager client.  The method is provided largely
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
    private class RemoteManagerWatchdogTarget
        implements WatchdogTarget {

        /**
         * @see org.apache.james.util.watchdog.WatchdogTarget#execute()
         */
        public void execute() {
            RemoteManagerHandler.this.idleClose();
        }
    }
}
