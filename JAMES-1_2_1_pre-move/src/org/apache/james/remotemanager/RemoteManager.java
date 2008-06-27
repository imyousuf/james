/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.remotemanager;

import org.apache.avalon.*;
import org.apache.avalon.blocks.*;
import org.apache.james.*;
import org.apache.james.transport.*;
import org.apache.james.userrepository.*;
import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Provides a really rude network interface to administer James.
 * Allow to add accounts.
 * TODO: -improve protocol
 *       -add remove user
 *       -much more...
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class RemoteManager implements SocketServer.SocketHandler, TimeServer.Bell, Composer, Configurable {

    private ComponentManager comp;
    private Configuration conf;
    private Logger logger;
    private UsersRepository users;
    private TimeServer timeServer;
    private MailServer mailServer;

    private BufferedReader in;
    private InputStream socketIn;
    private PrintWriter out;
    private OutputStream r_out;
    private Hashtable admaccount;
    private Socket socket;

    public void setConfiguration(Configuration conf) {
        this.conf = conf;
    }

    public void setComponentManager(ComponentManager comp) {
        this.comp = comp;
    }

    public void init() throws Exception {

        logger = (Logger) comp.getComponent(Interfaces.LOGGER);
        logger.log("RemoteManager init...", "RemoteAdmin", logger.INFO);
        this.timeServer = (TimeServer) comp.getComponent(Interfaces.TIME_SERVER);
    this.mailServer = (MailServer) comp.getComponent(Interfaces.MAIL_SERVER);
        SocketServer socketServer = (SocketServer) comp.getComponent(Interfaces.SOCKET_SERVER);
        int port = conf.getConfiguration("port").getValueAsInt(4554);
        InetAddress bind = null;
        try {
            String bindTo = conf.getConfiguration("bind").getValue();
            if (bindTo.length() > 0) {
                bind = InetAddress.getByName(bindTo);
            }
        } catch (ConfigurationException e) {
        }

    String type = SocketServer.DEFAULT;
    try {
        if (conf.getConfiguration("useTLS").getValue().equals("TRUE")) type = SocketServer.TLS;
    } catch (ConfigurationException e) {
    }
    String typeMsg = "RemoteManager using " + type + " on port " + port;
        logger.log(typeMsg, "RemoteAdmin", logger.INFO);

        socketServer.openListener("JAMESRemoteControlListener",type, port, bind, this);

        admaccount = new Hashtable();
        for (Enumeration e = conf.getConfigurations("administrator_accounts.account"); e.hasMoreElements();) {
            Configuration c = (Configuration) e.nextElement();
            admaccount.put(c.getAttribute("login"), c.getAttribute("password"));
        }
        if (admaccount.isEmpty()) {
            logger.log("No Administrative account defined", "RemoteAdmin", logger.WARNING);
        }
        users = (UsersRepository) comp.getComponent(Constants.LOCAL_USERS);
        logger.log("RemoteManager ...init end", "RemoteAdmin", logger.INFO);
    }

    public void parseRequest(Socket s) {

        timeServer.setAlarm("RemoteManager", this, conf.getConfiguration("connectiontimeout").getValueAsLong(120000));
        socket = s;
        String remoteHost = s.getInetAddress().getHostName();
        String remoteIP = s.getInetAddress().getHostAddress();
        try {
            socketIn = s.getInputStream();
            in = new BufferedReader(new InputStreamReader(socketIn));
            r_out = s.getOutputStream();
            out = new PrintWriter(r_out, true);
            logger.log("Access from " + remoteHost + "(" + remoteIP + ")", "RemoteAdmin", logger.INFO);
            out.println("JAMES RemoteAdministration Tool " + Constants.SOFTWARE_VERSION);
            out.println("Please enter your login and password");
            String login = in.readLine();
            String password = in.readLine();
            while (!password.equals(admaccount.get(login)) || password.length() == 0) {
                out.println("Login failed for " + login);
                logger.log("Login for " + login + " failed", "RemoteAdmin", logger.INFO);
                login = in.readLine();
                password = in.readLine();
            }
            timeServer.resetAlarm("RemoteManager");
            out.println("Welcome " + login + ". HELP for a list of commands");
            logger.log("Login for " + login + " succesful", "RemoteAdmin", logger.INFO);
            while (parseCommand(in.readLine())) {
                timeServer.resetAlarm("RemoteManager");
            }
            logger.log("Logout for " + login + ".", "RemoteAdmin", logger.INFO);
            s.close();
        } catch (IOException e) {
            out.println("Error. Closing connection");
            out.flush();
            logger.log("Exception during connection from " + remoteHost + " (" + remoteIP + ")", "RemoteAdmin", logger.ERROR);
        }
        timeServer.removeAlarm("RemoteManager");
    }

    public void wake(String name, String memo) {
        logger.log("Connection timeout on socket", "RemoteAdmin", logger.ERROR);
        try {
            out.println("Connection timeout. Closing connection");
            socket.close();
        } catch (IOException e) {
        }
    }

    private boolean parseCommand(String command) {
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
            logger.log("User " + user + " added", "RemoteAdmin", logger.INFO);
        } else {
            out.println("Error adding user " + user);
            logger.log("Error adding user " + user, "RemoteAdmin", logger.INFO);
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
            logger.log("User " + user + " deleted", "RemoteAdmin", logger.INFO);
        } else if (command.equalsIgnoreCase("LISTUSERS")) {
            out.println("Existing accounts " + users.countUsers());
            for (Enumeration e = users.list(); e.hasMoreElements();) {
                out.println("user: " + (String) e.nextElement());
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

    public void destroy() {
    }
}

