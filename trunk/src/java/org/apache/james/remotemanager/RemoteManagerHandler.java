/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.remotemanager;

import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLoggable;
import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.cornerstone.services.scheduler.PeriodicTimeTrigger;
import org.apache.avalon.cornerstone.services.scheduler.Target;
import org.apache.avalon.cornerstone.services.scheduler.TimeScheduler;
import org.apache.james.Constants;
import org.apache.james.BaseConnectionHandler;
import org.apache.james.services.MailServer;
import org.apache.james.services.UsersRepository;
import org.apache.james.services.UsersStore;

/**
 * Provides a really rude network interface to administer James.
 * Allow to add accounts.
 * TODO: -improve protocol
 *       -add remove user
 *       -much more...
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 *
 */
public class RemoteManagerHandler
    extends BaseConnectionHandler
    implements ConnectionHandler, Composable, Configurable, Target {

    private UsersStore usersStore;
    private UsersRepository users;
    private TimeScheduler scheduler;
    private MailServer mailServer;

    private BufferedReader in;
    private PrintWriter out;
    private HashMap admaccount = new HashMap();
    private Socket socket;

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
     * @exception IOException if an error reading from socket occurs
     * @exception ProtocolException if an error handling connection occurs
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
            getLogger().info( "Access from " + remoteHost + "(" + remoteIP + ")" );
            out.println( "JAMES RemoteAdministration Tool " + Constants.SOFTWARE_VERSION );
            out.println("Please enter your login and password");
	    out.println("Login id:");
            String login = in.readLine();
	    out.println("Password:");
            String password = in.readLine();

            while (!password.equals(admaccount.get(login)) || password.length() == 0) {
                scheduler.resetTrigger(this.toString());
                final String message = "Login failed for " + login;
                out.println( message );
                getLogger().info( message );
                login = in.readLine();
                password = in.readLine();
            }

            scheduler.resetTrigger(this.toString());

            out.println( "Welcome " + login + ". HELP for a list of commands" );
            getLogger().info("Login for " + login + " succesful");

            while (parseCommand(in.readLine())) {
                scheduler.resetTrigger(this.toString());
            }
            getLogger().info("Logout for " + login + ".");
            socket.close();

        } catch ( final IOException e ) {
            out.println("Error. Closing connection");
            out.flush();
            getLogger().error( "Exception during connection from " + remoteHost +
                               " (" + remoteIP + ")");
        }

        scheduler.removeTrigger(this.toString());
    }

    public void targetTriggered( final String triggerName ) {
        getLogger().error("Connection timeout on socket");
        try {
            out.println("Connection timeout. Closing connection");
            socket.close();
        } catch ( final IOException ioe ) {
        }
    }

    private boolean parseCommand( String command ) {
        if (command == null) return false;
        StringTokenizer commandLine = new StringTokenizer(command.trim(), " ");
        int arguments = commandLine.countTokens();
        if (arguments == 0) {
            return true;
        } else if(arguments > 0) {
            command = commandLine.nextToken();
        }
        String argument = (String) null;
        if(arguments > 1) {
            argument = commandLine.nextToken();
        }
        String argument1 = (String) null;
        if(arguments > 2) {
            argument1 = commandLine.nextToken();
        }
        if (command.equalsIgnoreCase("ADDUSER")) {
            String user = argument;
            String passwd = argument1;
            try {
                if (user.equals("") || passwd.equals("")) {
                    out.println("usage: adduser [username] [password]");
                    return true;
                }
            } catch (NullPointerException e) {
                out.println("usage: adduser [username] [password]");
                return true;
            }
            if (users.contains(user)) {
                out.println("user " + user + " already exist");
            } else {
                if(mailServer.addUser(user, passwd)) {
                    out.println("User " + user + " added");
                    getLogger().info("User " + user + " added");
                } else {
                    out.println("Error adding user " + user);
                    getLogger().info("Error adding user " + user);
                }
            }
            out.flush();
        } else if (command.equalsIgnoreCase("DELUSER")) {
            String user = argument;
            if (user.equals("")) {
                out.println("usage: deluser [username]");
                return true;
            }
            try {
                users.removeUser(user);
            } catch (Exception e) {
                out.println("Error deleting user " + user + " : " + e.getMessage());
                return true;
            }
            out.println("User " + user + " deleted");
            getLogger().info("User " + user + " deleted");
        } else if (command.equalsIgnoreCase("LISTUSERS")) {
            out.println("Existing accounts " + users.countUsers());
            for (Iterator it = users.list(); it.hasNext();) {
                out.println("user: " + (String) it.next());
            }
        } else if (command.equalsIgnoreCase("COUNTUSERS")) {
            out.println("Existing accounts " + users.countUsers());
        } else if (command.equalsIgnoreCase("VERIFY")) {
            String user = argument;
            if (user.equals("")) {
                out.println("usage: verify [username]");
                return true;
            }
            if (users.contains(user)) {
                out.println("User " + user + " exist");
            } else {
                out.println("User " + user + " does not exist");
            }
        } else if (command.equalsIgnoreCase("HELP")) {
            out.println("Currently implemented commans:");
            out.println("help                            display this help");
            out.println("adduser [username] [password]   add a new user");
            out.println("deluser [username]              delete existing user");
            out.println("listusers                       display existing accounts");
            out.println("countusers                      display the number of existing accounts");
            out.println("verify [username]               verify if specified user exist");
            out.println("quit                            close connection");
            out.flush();
        } else if (command.equalsIgnoreCase("QUIT")) {
            out.println("bye");
            return false;
        } else {
            out.println("unknown command " + command);
        }
        return true;
    }
}

