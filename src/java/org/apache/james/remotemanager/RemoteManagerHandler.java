/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
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
 *       -add remove user
 *       -much more...
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @author Peter M. Goldstein <farsight@alum.mit.edu>
 *
 * @version $Revision: 1.20 $
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
     * The text string for the UNSETFORWARDING command
     */
    private static final String COMMAND_UNSETFORWARDING = "UNSETFORWARDING";

    /**
     * The text string for the SETALIAS command
     */
    private static final String COMMAND_SETALIAS = "SETALIAS";

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
            out.println( "JAMES Remote Administration Tool " + Constants.SOFTWARE_VERSION );
            out.println("Please enter your login and password");
            String login = null;
            String password = null;
            do {
                if (login != null) {
                    final String message = "Login failed for " + login;
                    out.println( message );
                    getLogger().info( message );
                }
                out.println("Login id:");
                out.flush();
                login = in.readLine().trim();
                out.println("Password:");
                out.flush();
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
            out.println("Usage: adduser [username] [password]");
            out.flush();
            return true;
        }
        String username = argument.substring(0,breakIndex);
        String passwd = argument.substring(breakIndex + 1);
        if (username.equals("") || passwd.equals("")) {
            out.println("Usage: adduser [username] [password]");
            out.flush();
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
            out.println(response);
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
            getLogger().info("Error adding user " + username);
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
            out.println("Usage: setpassword [username] [password]");
            out.flush();
            return true;
        }
        String username = argument.substring(0,breakIndex);
        String passwd = argument.substring(breakIndex + 1);

        if (username.equals("") || passwd.equals("")) {
            out.println("Usage: adduser [username] [password]");
            return true;
        }
        User user = users.getUserByName(username);
        if (user == null) {
            out.println("No such user " + username);
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
            getLogger().info("Error resetting password");
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
            out.println("Usage: deluser [username]");
            return true;
        }
        try {
            users.removeUser(user);
        } catch (Exception e) {
            StringBuffer exceptionBuffer =
               new StringBuffer(128)
                       .append("Error deleting user ")
                       .append(user)
                       .append(" : ")
                       .append(e.getMessage());
            out.println(exceptionBuffer.toString());
            return true;
        }
        StringBuffer responseBuffer =
            new StringBuffer(64)
                    .append("User ")
                    .append(user)
                    .append(" deleted");
        String response = responseBuffer.toString();
        out.println(response);
        out.flush();
        getLogger().info(response);
        return true;
    }

    /**
     * Handler method called upon receipt of an LISTUSERS command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doLISTUSERS(String argument) {
        out.println("Existing accounts " + users.countUsers());
        for (Iterator it = users.list(); it.hasNext();) {
           out.println("user: " + (String) it.next());
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
        out.println("Existing accounts " + users.countUsers());
        out.flush();
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
        if (user.equals("")) {
            out.println("Usage: verify [username]");
            return true;
        }
        if (users.contains(user)) {
            StringBuffer responseBuffer =
                new StringBuffer(64)
                        .append("User ")
                        .append(user)
                        .append(" exists");
            String response = responseBuffer.toString();
            out.println(response);
        } else {
            StringBuffer responseBuffer =
                new StringBuffer(64)
                        .append("User ")
                        .append(user)
                        .append(" does not exist");
            String response = responseBuffer.toString();
            out.println(response);
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
        out.println("setalias [alias] [user]                 locally forwards all email for 'alias' to 'user'");
        out.println("unsetalias [alias]                      unsets an alias");
        out.println("setforwarding [username] [emailaddress] forwards a user's email to another email address");
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
            out.println("Usage: setalias [username] [emailaddress]");
            return true;
        }
        String username = argument.substring(0,breakIndex);
        String alias = argument.substring(breakIndex + 1);
        if (username.equals("") || alias.equals("")) {
            out.println("Usage: setalias [username] [alias]");
            return true;
        }
        JamesUser user = (JamesUser) users.getUserByName(username);
        if (user == null) {
            out.println("No such user " + username);
            return true;
        }
        JamesUser aliasUser = (JamesUser) users.getUserByName(alias);
        if (aliasUser == null) {
            out.println("Alias unknown to server"
                        + " - create that user first.");
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
            getLogger().info("Error setting alias");
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
            out.println("Usage: setforwarding [username] [emailaddress]");
            return true;
        }
        String username = argument.substring(0,breakIndex);
        String forward = argument.substring(breakIndex + 1);
        if (username.equals("") || forward.equals("")) {
           out.println("Usage: setforwarding [username] [emailaddress]");
           return true;
        }
        // Verify user exists
        User baseuser = users.getUserByName(username);
        if (baseuser == null) {
            out.println("No such user " + username);
            out.flush();
            return true;
        } else if (! (baseuser instanceof JamesUser ) ) {
            out.println("Can't set forwarding for this user type.");
            out.flush();
            return true;
        }
        JamesUser user = (JamesUser)baseuser;
        // Verify acceptable email address
        MailAddress forwardAddr;
        try {
             forwardAddr = new MailAddress(forward);
        } catch(ParseException pe) {
            out.println("Parse exception with that email address: "
                        + pe.getMessage());
            out.println("Forwarding address not added for " + username);
            out.flush();
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
            getLogger().info("Error setting forwarding");
        }
        out.flush();
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
            out.println("Usage: unsetalias [username]");
            out.flush();
            return true;
        }
        String username = argument;
        JamesUser user = (JamesUser) users.getUserByName(username);
        if (user == null) {
            out.println("No such user " + username);
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
            out.println("Aliasing not active for" + username);
            getLogger().info("Aliasing not active for" + username);
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
            out.println("Usage: unsetforwarding [username]");
            out.flush();
            return true;
        }
        String username = argument;
        JamesUser user = (JamesUser) users.getUserByName(username);
        if (user == null) {
            out.println("No such user " + username);
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
            getLogger().info(response);
        } else {
            out.println("Forwarding not active for" + username);
            getLogger().info("Forwarding not active for" + username);
        }
        out.flush();
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
            out.println("Usage: user [repositoryName]");
            out.flush();
            return true;
        }
        String repositoryName = argument.toLowerCase(Locale.US);
        UsersRepository repos = theConfigData.getUserStore().getRepository(repositoryName);
        if ( repos == null ) {
            out.println("No such repository: " + repositoryName);
        } else {
            users = repos;
            StringBuffer responseBuffer =
                new StringBuffer(64)
                        .append("Changed to repository '")
                        .append(repositoryName)
                        .append("'.");
            out.println(responseBuffer.toString());
            if ( repositoryName.equals("localusers") ) {
                inLocalUsers = true;
            } else {
                inLocalUsers = false;
            }
        }
        out.flush();
        return true;
    }

    /**
     * Handler method called upon receipt of a QUIT command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doQUIT(String argument) {
        out.println("Bye");
        out.flush();
        return false;
    }

    /**
     * Handler method called upon receipt of a SHUTDOWN command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doSHUTDOWN(String argument) {
        out.println("Shutting down, bye bye");
        out.flush();
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
        out.println("Unknown command " + argument);
        out.flush();
        return true;
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

