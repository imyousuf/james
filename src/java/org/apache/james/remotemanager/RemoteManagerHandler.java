/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.remotemanager;

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
import org.apache.james.services.*;
import org.apache.james.userrepository.DefaultUser;
import org.apache.mailet.MailAddress;

import javax.mail.internet.ParseException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
 *
 * Last changed by: $Author: pgoldstein $ on $Date: 2002/08/18 07:21:09 $
 * $Revision: 1.13 $
 *
 */
public class RemoteManagerHandler
    extends BaseConnectionHandler
    implements ConnectionHandler, Composable, Configurable, Target {

    private UsersStore usersStore;
    private UsersRepository users;

    /**
     * The scheduler used to handle timeouts for the RemoteManager
     * interaction
    */
    private TimeScheduler scheduler;
    private MailServer mailServer;

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
     * A HashMap of (user id, passwords) for James administrators
     */
    private HashMap admaccount = new HashMap();

    /**
     * Pass the <code>Configuration</code> to the instance.
     *
     * @param configuration the class configurations.
     * @throws ConfigurationException if an error occurs
     */
    public void configure( final Configuration configuration )
        throws ConfigurationException {

        timeout = configuration.getChild( "connectiontimeout" ).getValueAsInteger( 120000 );

        final Configuration admin = configuration.getChild( "administrator_accounts" );
        final Configuration[] accounts = admin.getChildren( "account" );
        for ( int i = 0; i < accounts.length; i++ )
        {
            admaccount.put( accounts[ i ].getAttribute( "login" ),
                            accounts[ i ].getAttribute( "password" ) );
        }
    }

    /**
     * Pass the <code>ComponentManager</code> to the <code>composer</code>.
     * The instance uses the specified <code>ComponentManager</code> to 
     * acquire the components it needs for execution.
     *
     * @param componentManager The <code>ComponentManager</code> which this
     *                <code>Composable</code> uses.
     * @throws ComponentException if an error occurs
     */
    public void compose( final ComponentManager componentManager )
        throws ComponentException {

        scheduler = (TimeScheduler)componentManager.
            lookup( "org.apache.avalon.cornerstone.services.scheduler.TimeScheduler" );
        mailServer = (MailServer)componentManager.
            lookup( "org.apache.james.services.MailServer" );
        usersStore = (UsersStore)componentManager.
            lookup( "org.apache.james.services.UsersStore" );
        users = usersStore.getRepository("LocalUsers");;
    }

    /**
     * Handle a connection.
     * This handler is responsible for processing connections as they occur.
     *
     * @param connection the connection
     * @throws IOException if an error reading from socket occurs
     * @throws ProtocolException if an error handling connection occurs
     */
    public void handleConnection( final Socket connection )
        throws IOException {

        /*
          if( admaccount.isEmpty() ) {
          getLogger().warn("No Administrative account defined");
          getLogger().warn("RemoteManager failed to be handled");
          return;
          }
        */

        final PeriodicTimeTrigger trigger = new PeriodicTimeTrigger( timeout, -1 );
        scheduler.addTrigger( this.toString(), trigger, this );
        socket = connection;
        String remoteHost = socket.getInetAddress().getHostName();
        String remoteIP = socket.getInetAddress().getHostAddress();

        try {
            in = new BufferedReader(new InputStreamReader( socket.getInputStream() ));
            out = new PrintWriter( socket.getOutputStream(), true);
            if (getLogger().isInfoEnabled())
            {
                StringBuffer infoBuffer =
                    new StringBuffer(128)
                            .append("Access from ")
                            .append(remoteHost)
                            .append("(")
                            .append(remoteIP)
                            .append(")");
                getLogger().info( infoBuffer.toString() );
            }
            out.println( "JAMES RemoteAdministration Tool " + Constants.SOFTWARE_VERSION );
            out.println("Please enter your login and password");
            String login = null;
            String password = null;
            do {
                scheduler.resetTrigger(this.toString());
                if (login != null) {
                    final String message = "Login failed for " + login;
                    out.println( message );
                    getLogger().info( message );
                }
                out.println("Login id:");
                login = in.readLine().trim();
                out.println("Password:");
                password = in.readLine().trim();
            } while (!password.equals(admaccount.get(login)) || password.length() == 0);

            scheduler.resetTrigger(this.toString());

            StringBuffer messageBuffer =
                new StringBuffer(64)
                        .append("Welcome ")
                        .append(login)
                        .append(". HELP for a list of commands");
            out.println( messageBuffer.toString() );
            if (getLogger().isInfoEnabled())
            {
                StringBuffer infoBuffer =
                    new StringBuffer(128)
                            .append("Login for ")
                            .append(login)
                            .append(" successful");
                getLogger().info(infoBuffer.toString());
            }

            try {
                while (parseCommand(in.readLine())) {
                    scheduler.resetTrigger(this.toString());
                }
            } catch (IOException ioe) {
                //We can cleanly ignore this as it's probably a socket timeout
            } catch (Throwable thr) {
                System.out.println("Exception: " + thr.getMessage());
                thr.printStackTrace();
            }
            StringBuffer infoBuffer =
                new StringBuffer(64)
                        .append("Logout for ")
                        .append(login)
                        .append(".");
            getLogger().info(infoBuffer.toString());
            socket.close();

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
        }

        scheduler.removeTrigger(this.toString());
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
        } catch ( final IOException ioe ) {
        }
    }

    /**
     * <p>This method parses and processes RemoteManager commands read off the 
     * wire in handleConnection.  It returns true if expecting additional 
     * commands, false otherwise.</p>
     *
     * <p>TODO: Break this method into smaller methods.</p>
     *
     * @param command the raw command string passed in over the socket
     *
     * @return whether additional commands are expected.
     */
    private boolean parseCommand( String command ) {
        if (command == null) {
            return false;
        }
        StringTokenizer commandLine = new StringTokenizer(command.trim(), " ");
        int arguments = commandLine.countTokens();
        if (arguments == 0) {
            return true;
        } else if(arguments > 0) {
            command = commandLine.nextToken().toUpperCase(Locale.US);
        }
        String argument = (String) null;
        if(arguments > 1) {
            argument = commandLine.nextToken();
        }
        String argument1 = (String) null;
        if(arguments > 2) {
            argument1 = commandLine.nextToken();
        }
        if (command.equals("ADDUSER")) {
            String username = argument;
            String passwd = argument1;
            try {
                if (username.equals("") || passwd.equals("")) {
                    out.println("usage: adduser [username] [password]");
                    return true;
                }
            } catch (NullPointerException e) {
                out.println("usage: adduser [username] [password]");
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
            }
            else if ( inLocalUsers ) {
                success = mailServer.addUser(username, passwd);
            }
            else {
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
            }
            else {
                out.println("Error adding user " + username);
                getLogger().info("Error adding user " + username);
            }
            out.flush();
        } else if (command.equals("SETPASSWORD")) {
            if (argument == null || argument1 == null) {
                out.println("usage: setpassword [username] [password]");
                return true;
            }
            String username = argument;
            String passwd = argument1;
            if (username.equals("") || passwd.equals("")) {
                out.println("usage: adduser [username] [password]");
                return true;
            }
            User user = users.getUserByName(username);
            if (user == null) {
                out.println("No such user");
                return true;
            }
            boolean success;
            success = user.setPassword(passwd);
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
        } else if (command.equals("DELUSER")) {
            String user = argument;
            if (user.equals("")) {
                out.println("usage: deluser [username]");
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
            getLogger().info(response);
        } else if (command.equals("LISTUSERS")) {
            out.println("Existing accounts " + users.countUsers());
            for (Iterator it = users.list(); it.hasNext();) {
                out.println("user: " + (String) it.next());
            }
        } else if (command.equals("COUNTUSERS")) {
            out.println("Existing accounts " + users.countUsers());
        } else if (command.equals("VERIFY")) {
            String user = argument;
            if (user.equals("")) {
                out.println("usage: verify [username]");
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
        } else if (command.equals("HELP")) {
            out.println("Currently implemented commands:");
            out.println("help                                    display this help");
            out.println("listusers                               display existing accounts");
            out.println("countusers                              display the number of existing accounts");
            out.println("adduser [username] [password]           add a new user");
            out.println("verify [username]                       verify if specified user exist");
            out.println("deluser [username]                      delete existing user");
            out.println("setpassword [username] [password]       sets a user's password");
            out.println("setalias [username] [alias]             sets a user's alias");
            out.println("unsetalias [username]                   removes a user's alias");
            out.println("setforwarding [username] [emailaddress] forwards a user's email to another account");
            out.println("user [repositoryname]                   change to another user repository");
            out.println("shutdown                                kills the current JVM (convenient when James is run as a daemon)");
            out.println("quit                                    close connection");
            out.flush();
        } else if (command.equals("SETALIAS")) {
            if (argument == null || argument1 == null) {
                out.println("usage: setalias [username] [alias]");
                return true;
            }
            String username = argument;
            String alias = argument1;
            if (username.equals("") || alias.equals("")) {
                out.println("usage: adduser [username] [alias]");
                return true;
            }
            JamesUser user = (JamesUser) users.getUserByName(username);
            if (user == null) {
                out.println("No such user");
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
        } else if (command.equals("SETFORWARDING")) {
            if (argument == null || argument1 == null) {
                out.println("usage: setforwarding [username] [emailaddress]");
                return true;
            }
            String username = argument;
            String forward = argument1;
            if (username.equals("") || forward.equals("")) {
                out.println("usage: adduser [username] [emailaddress]");
                return true;
            }
            // Verify user exists
            User baseuser = users.getUserByName(username);
            if (baseuser == null) {
                out.println("No such user");
                return true;
            }
            else if (! (baseuser instanceof JamesUser ) ) {
                out.println("Can't set forwarding for this user type.");
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
        } else if (command.equals("UNSETALIAS")) {
            if (argument == null) {
                    out.println("usage: unsetalias [username]");
                    return true;
            }
            String username = argument;
            if (username.equals("")) {
                out.println("usage: adduser [username]");
                return true;
            }
            JamesUser user = (JamesUser) users.getUserByName(username);
            if (user == null) {
                out.println("No such user");
                return true;
            }

            if (user.getAliasing()){
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
        } else if (command.equals("USE")) {
            if (argument == null || argument.equals("")) {
                out.println("usage: use [repositoryName]");
                return true;
            }
            String repositoryName = argument.toLowerCase(Locale.US);
            UsersRepository repos = usersStore.getRepository(repositoryName);
            if ( repos == null ) {
                out.println("no such repository");
                return true;
            }
            else {
                users = repos;
                StringBuffer responseBuffer =
                    new StringBuffer(64)
                            .append("Changed to repository '")
                            .append(repositoryName)
                            .append("'.");
                out.println(responseBuffer.toString());
                if ( repositoryName.equals("localusers") ) {
                    inLocalUsers = true;
                }
                else {
                    inLocalUsers = false;
                }
                return true;
            }

        } else if (command.equals("QUIT")) {
            out.println("bye");
            return false;
        } else if (command.equals("SHUTDOWN")) {
            out.println("shuting down, bye bye");
            System.exit(0);
            return false;
        } else {
            out.println("unknown command " + command);
        }
        return true;
    }
}

